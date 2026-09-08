package com.wei.wreader.agent.v2.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.wei.wreader.agent.v2.core.Tool;

import java.util.Map;

/**
 * LLM 工具定义
 *
 * @author weizhanjie
 */
public record LlmToolDefinition(
        String name,
        String description,
        JsonNode parametersSchema
) {

    /**
     * 从 Tool 创建
     */
    public static LlmToolDefinition fromTool(Tool tool) {
        return new LlmToolDefinition(
                tool.getName(),
                tool.getDescription(),
                tool.getParametersSchema()
        );
    }

    /**
     * 转换为 Map 格式（用于 API 请求）
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parametersSchema
                )
        );
    }
}
