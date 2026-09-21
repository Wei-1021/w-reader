package com.wei.wreader.agent.v2.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.wei.wreader.agent.v2.llm.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 LLM 客户端
 * <p>
 * 支持 OpenAI、DeepSeek、SiliconFlow、Ollama 等 OpenAI 兼容 API
 *
 * @author weizhanjie
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger LOG = Logger.getInstance(OpenAiCompatibleLlmClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final String model;

    private int connectTimeout = 30000;
    private int readTimeout = 180000;

    public OpenAiCompatibleLlmClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public LlmResponse chat(LlmRequest request) throws LlmException {
        try {
            // 使用请求中的 model，如果没有则使用默认的
            LlmRequest actualRequest = request;
            if (request.model() == null || request.model().isEmpty()) {
                actualRequest = new LlmRequest(
                        model,
                        request.messages(),
                        request.tools(),
                        request.temperature(),
                        request.maxTokens()
                );
            }

            String jsonBody = objectMapper.writeValueAsString(actualRequest.toMap());
            String responseBody = sendPostRequest("/chat/completions", jsonBody);
            return parseResponse(responseBody);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("请求 LLM 失败: " + e.getMessage(), e);
        }
    }

    private String sendPostRequest(String endpoint, String jsonBody) throws IOException, LlmException {
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
                throw new LlmException("HTTP " + responseCode + ": " + errorMsg, responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    private LlmResponse parseResponse(String responseBody) throws LlmException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new LlmException("LLM 返回数据中未找到 choices");
            }

            JsonNode choice = choices.get(0);
            JsonNode message = choice.path("message");
            String finishReason = choice.path("finish_reason").asText();
            if (finishReason == null || finishReason.isEmpty()) {
                finishReason = "stop";
            }

            // 提取内容
            String content = message.path("content").asText();
            if (content.isEmpty()) {
                content = null;
            }

            // 提取工具调用
            List<LlmToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = message.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode tc : toolCallsNode) {
                    String id = tc.path("id").asText();
                    String name = tc.path("function").path("name").asText();
                    String argumentsStr = tc.path("function").path("arguments").asText();

                    JsonNode arguments;
                    try {
                        arguments = objectMapper.readTree(argumentsStr);
                    } catch (Exception e) {
                        arguments = objectMapper.createObjectNode();
                    }

                    toolCalls.add(new LlmToolCall(id, name, arguments));
                }
            }

            return new LlmResponse(content, toolCalls, finishReason);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("解析 LLM 响应失败: " + e.getMessage(), e);
        }
    }

    private String parseErrorMessage(String errorBody) {
        try {
            JsonNode root = objectMapper.readTree(errorBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String message = error.path("message").asText();
                return message != null && !message.isEmpty() ? message : errorBody;
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

    // ==================== Getters & Setters ====================

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
