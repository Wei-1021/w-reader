package com.wei.wreader.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 OpenAI 兼容 API 客户端
 */
public class LLMClient {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    private int connectTimeout = 30000;
    private int readTimeout = 120000;

    public LLMClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 chat completions 接口
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 模型返回的文本内容
     * @throws LLMException 调用失败
     */
    public String chatCompletion(String systemPrompt, String userPrompt) throws LLMException {
        String endpoint = "/chat/completions";
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "stream", false,
                "temperature", 0.1
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String response = sendPostRequest(endpoint, jsonBody);
            return parseContent(response);
        } catch (IOException e) {
            throw new LLMException("请求AI接口失败: " + e.getMessage(), e);
        }
    }

    private String sendPostRequest(String endpoint, String jsonBody) throws IOException, LLMException {
        URL url = URI.create(baseUrl + endpoint).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readStream(connection.getInputStream());
            } else {
                String errorBody = readStream(connection.getErrorStream());
                String errorMsg = parseErrorMessage(errorBody);
                throw new LLMException("HTTP " + responseCode + ": " + errorMsg, responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    private String parseContent(String responseBody) throws LLMException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String text = choices.get(0).path("message").path("content").asText();
                return StringUtils.isNotBlank(text) ? text : "";
            }
            throw new LLMException("AI返回数据中未找到内容");
        } catch (IOException e) {
            throw new LLMException("解析AI返回数据失败: " + e.getMessage(), e);
        }
    }

    private String parseErrorMessage(String errorBody) {
        try {
            JsonNode root = objectMapper.readTree(errorBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String message = error.path("message").asText();
                return StringUtils.isNotBlank(message) ? message : errorBody;
            }
            return errorBody;
        } catch (Exception e) {
            return errorBody;
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    // ==================== 多轮对话 ====================

    /**
     * 创建一个多轮对话实例，共享 system prompt，提升缓存命中率
     */
    public Conversation createConversation(String systemPrompt) {
        return new Conversation(systemPrompt);
    }

    /**
     * 多轮对话 - 所有消息共享同一个上下文，LLM 可缓存前缀
     */
    public class Conversation {
        private final List<Map<String, String>> messages = new ArrayList<>();

        private Conversation(String systemPrompt) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        /**
         * 发送一条用户消息，返回助手回复。历史消息自动保留。
         */
        public String send(String userMessage) throws LLMException {
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.1);

            try {
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                String response = sendPostRequest("/chat/completions", jsonBody);
                String content = parseContent(response);
                messages.add(Map.of("role", "assistant", "content", content));
                return content;
            } catch (IOException e) {
                throw new LLMException("请求AI接口失败: " + e.getMessage(), e);
            }
        }

        /**
         * 获取当前对话轮数（不含 system）
         */
        public int getTurnCount() {
            return (messages.size() - 1) / 2;
        }

        /**
         * 获取完整消息列表（用于调试）
         */
        public List<Map<String, String>> getMessages() {
            return new ArrayList<>(messages);
        }
    }

    // ==================== Agent 对话（支持 tool_calls） ====================

    /**
     * 创建一个 Agent 对话实例，支持 function calling / tool_calls
     *
     * @param systemPrompt 系统提示词
     * @param tools        工具定义列表
     */
    public AgentConversation createAgentConversation(String systemPrompt, List<ToolDefinition> tools) {
        return new AgentConversation(systemPrompt, tools);
    }

    /**
     * 工具定义（OpenAI function calling 格式）
     */
    public static class ToolDefinition {
        private final String name;
        private final String description;
        private final String parametersSchema; // JSON Schema 字符串

        public ToolDefinition(String name, String description, String parametersSchema) {
            this.name = name;
            this.description = description;
            this.parametersSchema = parametersSchema;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getParametersSchema() { return parametersSchema; }
    }

    /**
     * 工具调用信息
     */
    public static class ToolCall {
        private final String id;
        private final String functionName;
        private final String arguments;

        public ToolCall(String id, String functionName, String arguments) {
            this.id = id;
            this.functionName = functionName;
            this.arguments = arguments;
        }

        public String getId() { return id; }
        public String getFunctionName() { return functionName; }
        public String getArguments() { return arguments; }
    }

    /**
     * Agent 响应（可能包含文本和/或工具调用）
     */
    public static class AgentResponse {
        private final String content;
        private final List<ToolCall> toolCalls;
        private final String finishReason;

        public AgentResponse(String content, List<ToolCall> toolCalls, String finishReason) {
            this.content = content;
            this.toolCalls = toolCalls != null ? toolCalls : List.of();
            this.finishReason = finishReason;
        }

        public String getContent() { return content; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public String getFinishReason() { return finishReason; }
        public boolean hasToolCalls() { return !toolCalls.isEmpty(); }
    }

    /**
     * Agent 对话 - 支持 tool_calls 的多轮对话
     */
    public class AgentConversation {
        private final List<Map<String, Object>> messages = new ArrayList<>();
        private final List<Map<String, Object>> tools;
        private int maxIterations = 20;
        private int currentIteration = 0;

        private AgentConversation(String systemPrompt, List<ToolDefinition> toolDefs) {
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // 构建 tools 数组（OpenAI 格式）
            this.tools = new ArrayList<>();
            if (toolDefs != null) {
                for (ToolDefinition tool : toolDefs) {
                    Map<String, Object> toolMap = new HashMap<>();
                    toolMap.put("type", "function");
                    Map<String, Object> function = new HashMap<>();
                    function.put("name", tool.getName());
                    function.put("description", tool.getDescription());
                    try {
                        function.put("parameters", objectMapper.readTree(tool.getParametersSchema()));
                    } catch (Exception e) {
                        function.put("parameters", objectMapper.createObjectNode());
                    }
                    toolMap.put("function", function);
                    this.tools.add(toolMap);
                }
            }
        }

        /**
         * 发送用户消息，返回 Agent 响应（可能包含 tool_calls）
         */
        public AgentResponse send(String userMessage) throws LLMException {
            messages.add(Map.of("role", "user", "content", userMessage));
            return callLLM();
        }

        /**
         * 提交工具执行结果，返回下一个 Agent 响应
         *
         * @param toolCallId 工具调用 ID
         * @param result     工具执行结果
         */
        public AgentResponse submitToolResult(String toolCallId, String result) throws LLMException {
            Map<String, Object> toolMessage = new HashMap<>();
            toolMessage.put("role", "tool");
            toolMessage.put("tool_call_id", toolCallId);
            toolMessage.put("content", result);
            messages.add(toolMessage);
            currentIteration++;
            return callLLM();
        }

        private AgentResponse callLLM() throws LLMException {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.1);
            if (!tools.isEmpty()) {
                requestBody.put("tools", tools);
            }

            try {
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                String response = sendPostRequest("/chat/completions", jsonBody);
                return parseAgentResponse(response);
            } catch (IOException e) {
                throw new LLMException("请求AI接口失败: " + e.getMessage(), e);
            }
        }

        private AgentResponse parseAgentResponse(String responseBody) throws LLMException {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.size() == 0) {
                    throw new LLMException("AI返回数据中未找到内容");
                }

                JsonNode choice = choices.get(0);
                JsonNode message = choice.path("message");
                String finishReason = choice.path("finish_reason").asText("");

                String content = message.path("content").isTextual()
                        ? message.path("content").asText() : null;

                List<ToolCall> toolCalls = new ArrayList<>();
                JsonNode toolCallsNode = message.path("tool_calls");
                if (toolCallsNode.isArray()) {
                    for (JsonNode tc : toolCallsNode) {
                        String id = tc.path("id").asText();
                        String functionName = tc.path("function").path("name").asText();
                        String arguments = tc.path("function").path("arguments").asText();
                        toolCalls.add(new ToolCall(id, functionName, arguments));
                    }
                }

                // 将 assistant 消息加入历史（保留 tool_calls 信息）
                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                if (content != null && !content.isEmpty()) {
                    assistantMsg.put("content", content);
                }
                if (!toolCalls.isEmpty()) {
                    List<Map<String, Object>> tcList = new ArrayList<>();
                    for (ToolCall tc : toolCalls) {
                        Map<String, Object> tcMap = new HashMap<>();
                        tcMap.put("id", tc.getId());
                        tcMap.put("type", "function");
                        tcMap.put("function", Map.of("name", tc.getFunctionName(), "arguments", tc.getArguments()));
                        tcList.add(tcMap);
                    }
                    assistantMsg.put("tool_calls", tcList);
                }
                messages.add(assistantMsg);

                return new AgentResponse(content, toolCalls, finishReason);
            } catch (IOException e) {
                throw new LLMException("解析AI返回数据失败: " + e.getMessage(), e);
            }
        }

        public int getCurrentIteration() { return currentIteration; }
        public int getMaxIterations() { return maxIterations; }
        public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }
        public boolean isMaxIterationsReached() { return currentIteration >= maxIterations; }

        public List<Map<String, Object>> getMessages() { return new ArrayList<>(messages); }
    }

    /**
     * LLM 调用异常
     */
    public static class LLMException extends Exception {
        private final int statusCode;

        public LLMException(String message) {
            super(message);
            this.statusCode = -1;
        }

        public LLMException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public LLMException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
        }

        public int getStatusCode() {
            return statusCode;
        }

        /**
         * 获取用户友好的错误提示
         */
        public String getUserFriendlyMessage() {
            if (statusCode == 401 || statusCode == 403) {
                return "API Key无效，请检查API配置";
            } else if (statusCode == 429) {
                return "API请求频率限制，请稍后重试";
            } else if (statusCode >= 500) {
                return "AI服务暂时不可用，请稍后重试";
            } else if (getMessage() != null && getMessage().contains("timed out")) {
                return "连接AI服务超时，请检查网络或API Base URL";
            }
            return getMessage();
        }
    }
}
