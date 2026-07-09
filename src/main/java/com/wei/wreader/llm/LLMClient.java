package com.wei.wreader.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                return choices.get(0).path("message").path("content").asText("");
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
                return error.path("message").asText(errorBody);
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
