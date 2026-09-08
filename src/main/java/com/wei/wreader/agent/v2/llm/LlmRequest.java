package com.wei.wreader.agent.v2.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM 请求
 *
 * @author weizhanjie
 */
public record LlmRequest(
        String model,
        List<LlmMessage> messages,
        List<LlmToolDefinition> tools,
        double temperature,
        int maxTokens
) {

    /**
     * 转换为 Map 格式（用于 API 请求）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("model", model);
        map.put("messages", messages.stream()
                .map(LlmMessage::toMap)
                .toList());
        map.put("temperature", temperature);

        if (maxTokens > 0) {
            map.put("max_tokens", maxTokens);
        }

        if (tools != null && !tools.isEmpty()) {
            map.put("tools", tools.stream()
                    .map(LlmToolDefinition::toMap)
                    .toList());
            map.put("tool_choice", "auto");
        }

        return map;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private List<LlmMessage> messages;
        private List<LlmToolDefinition> tools;
        private double temperature = 0.1;
        private int maxTokens = 0;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<LlmMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder tools(List<LlmToolDefinition> tools) {
            this.tools = tools;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public LlmRequest build() {
            return new LlmRequest(model, messages, tools, temperature, maxTokens);
        }
    }
}
