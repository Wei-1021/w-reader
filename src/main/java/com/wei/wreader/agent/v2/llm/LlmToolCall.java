package com.wei.wreader.agent.v2.llm;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * LLM 工具调用
 *
 * @author weizhanjie
 */
public record LlmToolCall(
        String id,
        String name,
        JsonNode arguments
) {

    /**
     * 转换为 Map 格式（用于 API 请求）
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "arguments", arguments.toString()
                )
        );
    }
}
