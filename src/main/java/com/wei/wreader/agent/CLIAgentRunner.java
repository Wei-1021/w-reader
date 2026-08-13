package com.wei.wreader.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI Agent 运行器
 * <p>
 * 通过 ProcessBuilder 调用 Claude Code / mimo 等 CLI 工具，
 * 让它们自主分析网站并生成书源规则，插件只负责收发结果。
 *
 * @author weizhanjie
 */
public class CLIAgentRunner {
    private static final Logger LOG = Logger.getInstance(CLIAgentRunner.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** CLI 超时时间（毫秒）：30 分钟 */
    private static final long TIMEOUT_MS = 30 * 60 * 1000;

    /** JSON 块提取正则：匹配 ```json ... ``` 或裸 JSON */
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```json\\s*\\n?([\\s\\S]*?)\\s*```|```\\s*\\n?([\\s\\S]*?)\\s*```",
            Pattern.MULTILINE
    );

    public enum CLIType {
        CLAUDE("claude", "Claude Code"),
        OPENCODE("opencode", "OpenCode"),
        MIMOCODE("mimo", "MiMo Code");

        public final String command;
        public final String displayName;

        CLIType(String command, String displayName) {
            this.command = command;
            this.displayName = displayName;
        }
    }

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<Process> currentProcess = new AtomicReference<>();
    private final List<Thread> workerThreads = new ArrayList<>();

    /**
     * 取消当前执行 - 会杀死 CLI 进程树并中断所有工作线程
     */
    public void cancel() {
        cancelled.set(true);

        // 杀死当前进程及其子进程树
        Process process = currentProcess.get();
        if (process != null && process.isAlive()) {
            LOG.info("Destroying CLI process tree...");

            // 先尝试关闭 stdin 通知 CLI 工具输入结束
            try {
                process.getOutputStream().close();
                LOG.info("Closed process stdin");
            } catch (Exception e) {
                LOG.debug("Failed to close stdin", e);
            }

            // 等待一小段时间让 CLI 工具自行退出
            try {
                if (process.waitFor(1, TimeUnit.SECONDS)) {
                    LOG.info("Process exited gracefully after stdin close");
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 如果还没退出，强制杀死进程树
            destroyProcessTree(process);
        }

        // 中断所有工作线程
        synchronized (workerThreads) {
            for (Thread thread : workerThreads) {
                if (thread.isAlive()) {
                    LOG.info("Interrupting thread: " + thread.getName());
                    thread.interrupt();
                }
            }
            workerThreads.clear();
        }
    }

    /**
     * 彻底杀死进程及其所有子进程（跨平台）
     */
    private void destroyProcessTree(Process process) {
        try {
            long pid = process.pid();
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

            if (isWindows) {
                // Windows: 使用 taskkill /F /T /PID 杀死整个进程树
                // /F = 强制杀死, /T = 杀死子进程树, /PID = 指定进程ID
                ProcessBuilder pb = new ProcessBuilder(
                        "cmd", "/c", "taskkill /F /T /PID " + pid
                );
                pb.redirectErrorStream(true);
                Process killProcess = pb.start();
                killProcess.waitFor(5, TimeUnit.SECONDS);

                // 读取输出用于调试
                String output = new String(killProcess.getInputStream().readAllBytes());
                LOG.info("taskkill output for PID " + pid + ": " + output);
            } else {
                // Linux/Mac: 使用 kill -9 杀死进程组
                // 首先尝试杀死进程组（负PID），如果失败则杀死单个进程
                ProcessBuilder pb = new ProcessBuilder(
                        "/bin/sh", "-c",
                        "kill -9 -" + pid + " 2>/dev/null; kill -9 " + pid + " 2>/dev/null; true"
                );
                pb.redirectErrorStream(true);
                Process killProcess = pb.start();
                killProcess.waitFor(5, TimeUnit.SECONDS);
                LOG.info("kill executed for PID: " + pid);
            }

            // 等待原进程结束
            process.waitFor(3, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOG.warn("Failed to destroy process tree, falling back to destroyForcibly", e);
            // 回退到 destroyForcibly
            process.destroyForcibly();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 检测 CLI 是否可用
     *
     * @param cliType CLI 类型
     * @return true 如果命令存在且可执行
     */
    public static boolean detectCLI(CLIType cliType) {
        try {
            ProcessBuilder pb = new ProcessBuilder(buildCommandArgs(cliType, "--version"));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readProcessOutput(process, 10_000);
            int exitCode = process.waitFor();
            return exitCode == 0 && !output.trim().isEmpty();
        } catch (Exception e) {
            LOG.debug("CLI detection failed for " + cliType.command + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用 CLI 生成书源规则（实时输出版本）
     *
     * @param cliType     CLI 类型
     * @param websiteUrl  目标网站 URL
     * @param ruleDoc     规范文档
     * @param exampleRule 示例规则 JSON
     * @param callback    回调
     */
    public void generate(CLIType cliType, String websiteUrl, String ruleDoc,
                         String exampleRule, AgentCallback callback) {
        cancelled.set(false);
        File tempPromptFile = null;

        try {
            String prompt = buildPrompt(websiteUrl, ruleDoc, exampleRule);
            callback.onMessage("正在启动 " + cliType.displayName + " 分析网站: " + websiteUrl);

            // 将 prompt 写入临时文件，避免 Windows 命令行长度限制
            tempPromptFile = File.createTempFile("wreader-agent-prompt-", ".txt");
            Files.writeString(tempPromptFile.toPath(), prompt);

            // 构建命令：通过 stdin 管道传入 prompt
            // Windows: cmd /c type tempfile | claude --output-format text
            // Linux:   cat tempfile | claude --output-format text
            ProcessBuilder pb = buildCommand(cliType, tempPromptFile.getAbsolutePath());
            pb.redirectErrorStream(false);

            callback.onToolCall(cliType.displayName, "启动 CLI 进程...");

            Process process = pb.start();
            currentProcess.set(process);

            // 用于收集完整输出（用于最终 JSON 提取）
            StringBuilder fullOutput = new StringBuilder();

            // 异步读取 stderr - Claude Code 可能将进度信息输出到 stderr
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (cancelled.get()) break;
                        LOG.info("[" + cliType.command + " stderr] " + line);

                        // Claude Code 的 stderr 也可能包含有用信息
                        if (CLIType.CLAUDE.equals(cliType) && !line.trim().isEmpty()) {
                            // 跳过一些无用的调试信息
                            if (!line.contains("DEBUG") && !line.contains("trace")) {
                                callback.onToolCall(cliType.displayName, "[进度] " + line);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!cancelled.get()) {
                        LOG.debug("Error reading CLI stderr", e);
                    }
                }
            }, cliType.command + "-stderr");
            stderrThread.setDaemon(true);
            synchronized (workerThreads) {
                workerThreads.add(stderrThread);
            }
            stderrThread.start();

            // 实时读取 stdout - 每行都回调显示
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (cancelled.get()) break;

                        // 尝试解析 stream-json 格式
                        String displayText = parseStreamJsonLine(line, cliType);
                        if (displayText != null && !displayText.isEmpty()) {
                            fullOutput.append(displayText).append("\n");
                            LOG.info("[" + cliType.command + "] " + displayText);
                            callback.onToolCall(cliType.displayName, displayText);
                        } else if (!line.trim().isEmpty()) {
                            // 非 JSON 格式，直接显示
                            fullOutput.append(line).append("\n");
                            LOG.info("[" + cliType.command + " raw] " + line);
                            callback.onToolCall(cliType.displayName, line);
                        }
                    }
                } catch (Exception e) {
                    if (!cancelled.get()) {
                        LOG.warn("Error reading CLI output", e);
                    }
                }
            }, cliType.command + "-stdout");
            stdoutThread.setDaemon(true);
            synchronized (workerThreads) {
                workerThreads.add(stdoutThread);
            }
            stdoutThread.start();

            // 等待进程结束（带超时）
            boolean finished = false;
            long startTime = System.currentTimeMillis();
            while (!finished) {
                // 检查取消（优先级最高）
                if (cancelled.get()) {
                    LOG.info("Cancel detected, destroying process tree...");
                    destroyProcessTree(process);
                    callback.onError("已取消");
                    return;
                }

                try {
                    finished = process.waitFor(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // 检查超时
                if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                    LOG.info("Timeout detected, destroying process tree...");
                    destroyProcessTree(process);
                    callback.onError(cliType.displayName + " 执行超时（30分钟），已终止");
                    return;
                }
            }

            // 进程已结束，等待 stdout 线程完成
            try {
                stdoutThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 检查是否被取消
            if (cancelled.get()) {
                callback.onError("已取消");
                return;
            }

            String output = fullOutput.toString();

            LOG.info("CLI output length: " + output.length());
            LOG.info("CLI output (first 1000 chars): " + output.substring(0, Math.min(1000, output.length())));

            if (output.trim().isEmpty()) {
                callback.onError(cliType.displayName + " 未返回结果，请检查 CLI 是否正确安装和配置");
                return;
            }

            // 提取 JSON 规则
            String jsonRule = extractJson(output);
            if (jsonRule != null && !jsonRule.isEmpty()) {
                if (!jsonRule.trim().startsWith("[")) {
                    jsonRule = "[" + jsonRule;
                }
                if (!jsonRule.trim().endsWith("]")) {
                    jsonRule = jsonRule + "]";
                }

                LOG.info("Successfully extracted JSON rule (length: " + jsonRule.length() + ")");
                callback.onToolResult(cliType.displayName, "✓ 分析完成，已提取规则 JSON");
                callback.onComplete(jsonRule);
            } else {
                // 没有找到 JSON，将原始输出作为消息返回，方便用户查看
                LOG.warn("Failed to extract JSON from CLI output");
                callback.onMessage("📋 CLI 输出内容:\n" + output);
                callback.onError("未能从 " + cliType.displayName + " 输出中提取到有效的 JSON 规则。请检查上方输出内容或手动复制 JSON。");
            }

        } catch (Exception e) {
            LOG.error("CLI agent execution failed", e);
            callback.onError("执行失败: " + e.getMessage());
        } finally {
            // 清理资源
            currentProcess.set(null);
            synchronized (workerThreads) {
                workerThreads.clear();
            }
            if (tempPromptFile != null && tempPromptFile.exists()) {
                tempPromptFile.delete();
            }
        }
    }

    /**
     * 构建 CLI 命令（通过 stdin 管道传入 prompt 文件内容）
     * <p>
     * 使用 "type file | command" (Windows) 或 "cat file | command" (Linux/Mac)
     * 避免命令行参数过长导致 CreateProcess error=206
     *
     * @param cliType         CLI 类型
     * @param promptFilePath  prompt 临时文件路径
     */
    private ProcessBuilder buildCommand(CLIType cliType, String promptFilePath) {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        List<String> args = new ArrayList<>();

        // 各 CLI 的额外参数
        // Claude Code: 使用 stream-json + verbose 获取流式输出
        // OpenCode/MiMo Code: 使用 --output json 获取 JSON 格式输出
        String extraArgs = switch (cliType) {
            case CLAUDE -> " --output-format stream-json --verbose --allowedTools \"Bash(*)\" \"WebFetch(domain:*)\"";
            case OPENCODE -> " run --format json";
            case MIMOCODE -> " run --format json";
            default -> "";
        };

        if (isWindows) {
            // cmd /c "type "C:\path with spaces\file.txt" | claude --output-format text"
            // 整条管道命令作为单个字符串传给 cmd /c，确保正确解析
            String cmd = "type \"" + promptFilePath + "\" | " + cliType.command + extraArgs;
            args.add("cmd");
            args.add("/c");
            args.add(cmd);
        } else {
            String cmd = "cat \"" + promptFilePath + "\" | " + cliType.command + extraArgs;
            args.add("/bin/sh");
            args.add("-c");
            args.add(cmd);
        }

        return new ProcessBuilder(args);
    }

    /**
     * 构建命令参数列表
     * Windows 上需要通过 cmd /c 包装 .cmd 脚本（如 npm 全局安装的 claude、mimo）
     */
    private static List<String> buildCommandArgs(CLIType cliType, String... extraArgs) {
        List<String> args = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            args.add("cmd");
            args.add("/c");
        }
        args.add(cliType.command);
        Collections.addAll(args, extraArgs);
        return args;
    }

    /**
     * 构建发送给 CLI 的 Prompt
     */
    private String buildPrompt(String websiteUrl, String ruleDoc, String exampleRule) {
        return "你是一个专业的书源规则生成专家。请分析以下小说网站并生成 W-Reader 插件的 SiteBean JSON 规则。\n\n"
                + "## 目标网站\n" + websiteUrl + "\n\n"
                + "## 重要限制\n"
                + "**⚠️ 工具使用规则：**\n"
//                + "- **必须使用 curl 获取网页，不要使用 WebFetch！**\n"
                + "- curl 命令已被预先授权，可以直接执行，无需等待用户批准\n"
                + "- 使用 curl 时请加 -s 参数（静默模式）避免进度输出\n"
//                + "- 使用 head -c 50000 限制输出大小，避免内容过长\n\n"
                + "### curl 使用示例\n"
                + "```bash\n"
                + "curl -s -L \"http://example.com\"\n"
                + "```\n\n"
                + "## 工作流程\n"
                + "1. 使用 curl 获取网站首页，了解网站结构\n"
                + "2. 尝试在网站搜索一本书（如搜索关键词\"完美\"），找到搜索结果页面\n"
                + "3. 从搜索结果中找到一本书的详情页链接，获取详情页\n"
                + "4. 分析书籍详情页的章节目录结构\n"
                + "5. 获取一个章节内容页，分析正文提取方式\n"
                + "6. 根据分析结果组装 SiteBean 规则 JSON\n\n"
                + "## 重要提示\n"
//                + "- **必须使用 curl 而不是 WebFetch 获取网页**\n"
                + "- curl 命令已被授权，可以直接执行\n"
                + "- CSS 选择器必须是有效的 Jsoup 语法\n"
                + "- hasHtml: HTML 页面填 true，JSON API 填 false\n"
                + "- id 填域名，name 填网站名，baseUrl 含协议\n"
                + "- 空字段填 \"\"，不要 null\n"
                + "- bookListUrlElement 和 bookListTitleElement 相对于每个列表项\n"
                + "- 如果页面内容是通过 JS 动态加载的，注意分析 API 端点\n"
                + "- 若填写规则时需要编写脚本代码规则，尽量使用 js 规则编写代码\n\n"
                + "## SiteBean 规范文档\n\n" + ruleDoc + "\n\n"
                + "## 示例规则\n\n" + exampleRule + "\n\n"
                + "## 输出要求\n"
                + "分析完成后，请输出完整的 SiteBean JSON 规则。\n"
                + "**必须使用以下格式输出，确保可以被程序正确解析：**\n\n"
                + "```json\n"
                + "[\n"
                + "  {\n"
                + "    \"enabled\": true,\n"
                + "    \"id\": \"域名\",\n"
                + "    \"name\": \"网站名\",\n"
                + "    \"baseUrl\": \"http://...\",\n"
                + "    ...\n"
                + "  }\n"
                + "]\n"
                + "```\n\n"
                + "注意：\n"
                + "1. JSON 必须是有效的格式\n"
                + "2. 必须用 ```json 和 ``` 包裹\n"
                + "3. 确保是完整的 JSON 数组格式\n"
                + "4. 不用将JSON写入本地文件，直接输出即可\n";
    }

    /**
     * 读取进程输出（带超时）
     */
    private static String readProcessOutput(Process process, long timeoutMs) {
        StringBuilder output = new StringBuilder();
        Thread readThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                // ignore
            }
        }, "cli-output-reader");
        readThread.setDaemon(true);
        readThread.start();

        try {
            readThread.join(timeoutMs);
            if (readThread.isAlive()) {
                readThread.interrupt();
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return output.toString();
    }

    /**
     * 从 CLI 输出中提取 JSON 规则
     * 优先匹配 ```json 代码块，其次匹配裸 JSON
     */
    private static String extractJson(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        LOG.info("Extracting JSON from output (length: " + output.length() + ")");

        // 1. 尝试匹配 ```json ... ``` 代码块
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(output);
        while (matcher.find()) {
            String json = matcher.group(1) != null ? matcher.group(1).trim() : matcher.group(2).trim();
            if (!json.isEmpty()) {
                // 尝试提取其中的 JSON
                String extracted = extractJsonContent(json);
                if (extracted != null) {
                    LOG.info("Found JSON in code block");
                    return extracted;
                }
            }
        }

        // 2. 尝试匹配裸 JSON
        String extracted = extractJsonContent(output);
        if (extracted != null) {
            LOG.info("Found bare JSON in output");
            return extracted;
        }

        LOG.warn("No valid JSON found in output");
        return null;
    }

    /**
     * 从字符串中提取 JSON 内容（支持对象和数组）
     */
    private static String extractJsonContent(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 尝试提取 JSON 对象
        String json = extractByBraces(text, '{', '}');
        if (json != null && isValidJson(json)) {
            return json;
        }

        // 尝试提取 JSON 数组
        json = extractByBraces(text, '[', ']');
        if (json != null && isValidJson(json)) {
            return json;
        }

        return null;
    }

    /**
     * 通过括号匹配提取内容
     */
    private static String extractByBraces(String text, char openBrace, char closeBrace) {
        // 找到第一个开括号
        int startIndex = text.indexOf(openBrace);
        if (startIndex < 0) {
            return null;
        }

        // 从开括号开始，找到匹配的闭括号
        int depth = 0;
        int endIndex = -1;
        boolean inString = false;
        boolean escapeNext = false;

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escapeNext) {
                escapeNext = false;
                continue;
            }

            if (c == '\\' && inString) {
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (c == openBrace) {
                depth++;
            } else if (c == closeBrace) {
                depth--;
                if (depth == 0) {
                    endIndex = i + 1;
                    break;
                }
            }
        }

        if (endIndex > startIndex) {
            return text.substring(startIndex, endIndex);
        }

        return null;
    }

    /**
     * 验证是否为有效 JSON（支持对象和数组）
     */
    private static boolean isValidJson(String str) {
        try {
            JsonNode node = objectMapper.readTree(str);
            // 支持 JSON 对象和非空数组
            return (node.isObject() || (node.isArray() && !node.isEmpty()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 CLI 工具的 JSON 格式输出<br>
     * 支持的格式：<br>
     * 1. Claude Code stream-json 格式：<br>
     * 2. OpenCode/MiMo Code JSON 格式：<br>
     *
     * @param line     JSON 行
     * @param cliType  CLI 类型
     * @return 要显示的文本，如果不需要显示则返回 null
     */
    private static String parseStreamJsonLine(String line, CLIType cliType) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        try {
            JsonNode node = objectMapper.readTree(line);
            String type = node.path("type").asText();

            // 根据 CLI 类型选择不同的解析策略
            if (CLIType.CLAUDE.equals(cliType)) {
                return parseClaudeCodeJson(node, type);
            } else {
                return parseOpenCodeJson(node, type);
            }
        } catch (Exception e) {
            // JSON 解析失败，返回原始行
            LOG.debug("Failed to parse stream-json line: " + line, e);
        }

        return null;
    }

    /**
     * 解析 Claude Code 的 stream-json 格式
     */
    private static String parseClaudeCodeJson(JsonNode node, String type) {
        if ("assistant".equals(type)) {
            JsonNode message = node.path("message");
            JsonNode content = message.path("content");

            if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : content) {
                    String itemType = item.path("type").asText();

                    if ("text".equals(itemType)) {
                        // 文本内容
                        String text = item.path("text").asText();
                        if (!text.isEmpty()) {
                            sb.append(text);
                        }
                    } else if ("thinking".equals(itemType)) {
                        // 思考内容
                        String thinking = item.path("thinking").asText();
                        if (!thinking.isEmpty()) {
                            sb.append(" [ thinking ] ").append(thinking);
                        }
                    } else if ("tool_use".equals(itemType)) {
                        // 工具调用
                        String toolName = item.path("name").asText();
                        JsonNode input = item.path("input");

                        if ("Bash".equals(toolName)) {
                            String command = input.path("command").asText();
                            String description = input.path("description").asText();
                            if (!description.isEmpty()) {
                                sb.append("🔧 ").append(description).append("\n");
                            }
                            sb.append("$ ").append(command);
                        } else {
                            sb.append("🔧 工具调用: ").append(toolName);
                        }
                    }
                }
                String result = sb.toString();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } else if ("user".equals(type)) {
            // 用户消息或工具结果
            JsonNode message = node.path("message");
            JsonNode content = message.path("content");

            if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : content) {
                    String itemType = item.path("type").asText();

                    if ("tool_result".equals(itemType)) {
                        // 工具结果
                        String contentText = item.path("content").asText();
                        if (!contentText.isEmpty()) {
                            // 截断过长的输出
                            if (contentText.length() > 500) {
                                contentText = contentText.substring(0, 500) + "...";
                            }
                            sb.append("📋 结果: ").append(contentText);
                        }
                    }
                }
                String result = sb.toString();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }  else if ("system".equals(type)) {
            // 系统消息
            return "system: " + node.asText();
        }else if ("result".equals(type)) {
            // 最终结果
            String result = node.path("result").asText();
            if (!result.isEmpty()) {
                return "✅ 完成: " + result;
            }
        } else if ("error".equals(type)) {
            // 错误信息
            String errorMessage = node.path("message").asText();
            if (!errorMessage.isEmpty()) {
                return "❌ 错误: " + errorMessage;
            }
        }

        return null;
    }

    /**
     * 解析 OpenCode/MiMo Code 的 JSON 格式
     */
    private static String parseOpenCodeJson(JsonNode node, String type) {
        if ("message".equals(type) || "text".equals(type)) {
            // 消息内容
            JsonNode partNode = node.path("part");
            if (partNode != null) {
                return partNode.path("text").asText();
            }
        } else if ("tool_use".equals(type) || "tool_call".equals(type)) {
            // 工具调用
            JsonNode partNode = node.path("part");
            if (partNode == null) {
                return null;
            }
            String partTool = partNode.path("tool").asText();
            JsonNode stateNode = partNode.path("state");
            JsonNode inputNode = null;
            if (stateNode != null) {
                inputNode = stateNode.path("input");
            }

            if (inputNode == null) {
                return null;
            }

            String command = inputNode.path("command").asText();
            String description = inputNode.path("description").asText();
            String output = stateNode.path("output").asText();

            return "🔧 " + description + ": " + partTool + "(" + command + ")" + "\n$ " + output;
        } else if (type.startsWith("step_")) {
            String tokens = "";
            JsonNode partNode = node.path("part");
            if (partNode != null) {
                tokens =  ", tokens: " + partNode.path("tokens").asText();
            }

            return "step: " + type + tokens;
        }

        return null;
    }
}
