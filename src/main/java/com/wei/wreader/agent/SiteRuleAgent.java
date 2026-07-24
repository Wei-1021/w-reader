package com.wei.wreader.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.llm.LLMClient;
import com.wei.wreader.llm.LLMClient.AgentConversation;
import com.wei.wreader.llm.LLMClient.AgentResponse;
import com.wei.wreader.llm.LLMClient.LLMException;
import com.wei.wreader.llm.LLMClient.ToolCall;
import com.wei.wreader.llm.LLMClient.ToolDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 书源规则生成 Agent 编排器
 * <p>
 * 使用 LLM function calling 实现完全自主的书源规则生成，
 * Agent 可以自主决定获取哪些页面、如何分析、何时生成规则。
 *
 * @author weizhanjie
 */
public class SiteRuleAgent {
    private static final Logger LOG = Logger.getInstance(SiteRuleAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ITERATIONS = 20;

    private final Project project;
    private final String llmBaseUrl;
    private final String llmApiKey;
    private final String llmModel;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public SiteRuleAgent(Project project, String llmBaseUrl, String llmApiKey, String llmModel) {
        this.project = project;
        this.llmBaseUrl = llmBaseUrl;
        this.llmApiKey = llmApiKey;
        this.llmModel = llmModel;
    }

    /**
     * 取消当前生成
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * 启动 Agent 生成流程
     *
     * @param websiteUrl 目标网站 URL
     * @param callback   事件回调
     */
    public void generate(String websiteUrl, AgentCallback callback) {
        cancelled.set(false);

        try {
            // 初始化工具
            SiteRuleTools tools = new SiteRuleTools(project);
            String baseUrl = tools.getExplorer().normalizeBaseUrl(websiteUrl);
            tools.setBaseUrl(baseUrl);

            // 创建 LLM 客户端
            LLMClient llmClient = new LLMClient(llmBaseUrl, llmApiKey, llmModel);
            llmClient.setReadTimeout(180000); // Agent 可能需要更长的超时

            // 获取工具定义
            List<ToolDefinition> toolDefs = tools.getToolDefinitions();

            // 创建 Agent 对话
            String systemPrompt = buildSystemPrompt();
            AgentConversation conversation = llmClient.createAgentConversation(systemPrompt, toolDefs);
            conversation.setMaxIterations(MAX_ITERATIONS);

            // 发送初始用户消息
            String userMessage = "请为这个小说网站生成书源规则: " + baseUrl;
            AgentResponse response = conversation.send(userMessage);

            // Agent 循环
            while (!cancelled.get()) {
                // 处理文本响应
                if (response.getContent() != null && !response.getContent().isEmpty()) {
                    callback.onMessage(response.getContent());
                }

                // 没有工具调用 → Agent 完成
                if (!response.hasToolCalls()) {
                    break;
                }

                // 达到最大迭代次数
                if (conversation.isMaxIterationsReached()) {
                    callback.onError("已达到最大迭代次数 (" + MAX_ITERATIONS + ")，请重试或手动调整");
                    return;
                }

                // 处理工具调用
                for (ToolCall toolCall : response.getToolCalls()) {
                    if (cancelled.get()) break;

                    String toolName = toolCall.getFunctionName();
                    String toolArgs = toolCall.getArguments();

                    // 通知 UI 工具调用开始
                    callback.onToolCall(toolName, toolArgs);

                    // 执行工具
                    String toolResult;
                    try {
                        JsonNode argsNode = objectMapper.readTree(toolArgs);
                        toolResult = tools.executeTool(toolName, argsNode);
                    } catch (Exception e) {
                        LOG.warn("Tool execution failed: " + toolName, e);
                        toolResult = "工具执行失败: " + e.getMessage();
                    }

                    // 限制工具结果长度，避免超出 LLM 上下文
                    if (toolResult.length() > 30000) {
                        toolResult = toolResult.substring(0, 30000) + "\n... (结果已截断)";
                    }

                    // 通知 UI 工具执行结果
                    callback.onToolResult(toolName, toolResult);

                    // 如果是 complete_rule 工具，直接完成
                    if ("complete_rule".equals(toolName)) {
                        String finalRule = tools.getFinalRule();
                        if (finalRule != null && !finalRule.isEmpty()) {
                            callback.onComplete(finalRule);
                            return;
                        }
                    }

                    // 提交工具结果给 LLM
                    try {
                        response = conversation.submitToolResult(toolCall.getId(), toolResult);
                    } catch (LLMException e) {
                        callback.onError("LLM 调用失败: " + e.getUserFriendlyMessage());
                        return;
                    }
                }
            }

            if (cancelled.get()) {
                callback.onError("已取消生成");
            }

        } catch (LLMException e) {
            LOG.error("Agent generation failed", e);
            callback.onError("LLM 调用失败: " + e.getUserFriendlyMessage());
        } catch (Exception e) {
            LOG.error("Agent generation failed", e);
            callback.onError("生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建 System Prompt
     */
    private String buildSystemPrompt() {
        String ruleDoc = loadResource("md/custom-rule-info.md");
        String exampleRule = loadResource("json/default-site-rule.json");

        return """
                你是一个专业的书源规则生成专家。你的任务是分析小说网站的页面结构，生成 W-Reader 插件的 SiteBean JSON 规则。

                ## 你的能力
                你可以通过以下工具来分析网站：
                - fetch_page: 获取网页内容
                - search_website: 在网站搜索书籍
                - extract_html_elements: 用 CSS 选择器提取 HTML 元素
                - parse_json_path: 用 JsonPath 提取 JSON 数据
                - validate_site_rule: 校验生成的规则
                - complete_rule: 提交最终规则（生成流程的最后一步）

                ## 工作流程
                1. 先获取网站首页，了解网站结构
                2. 执行搜索，找到搜索结果页面
                3. 从搜索结果中找到一本书的详情页链接
                4. 获取书籍详情页，分析章节目录结构
                5. 获取一个章节内容页，分析正文提取方式
                6. 根据分析结果组装 SiteBean 规则
                7. 使用 validate_site_rule 校验规则
                8. 校验通过后使用 complete_rule 提交规则

                ## 重要提示
                - CSS 选择器必须是有效的 Jsoup 语法
                - hasHtml: HTML 页面填 true，JSON API 填 false
                - id 填域名，name 填网站名，baseUrl 含协议
                - 空字段填 ""，不要 null
                - bookListUrlElement 和 bookListTitleElement 相对于每个列表项
                - 如果页面内容是通过 JS 动态加载的，注意分析 API 端点
                - 每次调用工具后，根据结果决定下一步操作
                - 如果某个步骤失败，尝试其他方法或跳过
                - 若填写规则时需要编写脚本代码规则，尽量使用js规则编写代码

                ## SiteBean 规范文档

                """ + ruleDoc + """

                ## 示例规则

                """ + exampleRule + """

                现在请开始分析目标网站并生成书源规则。
                """;
    }

    private String loadResource(String path) {
        try (InputStream is = SiteRuleAgent.class.getClassLoader().getResourceAsStream(path)) {
            return is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to load resource: " + path, e);
            return "";
        }
    }
}
