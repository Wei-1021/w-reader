package com.wei.wreader.agent.v2.llm;

import java.util.List;
import java.util.Map;

/**
 * LLM 消息
 *
 * @author weizhanjie
 */
public record LlmMessage(
        String role,
        String content,
        List<LlmToolCall> toolCalls,
        String toolCallId
) {

    /**
     * 创建 system 消息
     */
    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null);
    }

    /**
     * 创建 user 消息
     */
    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null);
    }

    /**
     * 创建 assistant 消息（纯文本）
     */
    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null, null);
    }

    /**
     * 创建 assistant 消息（带工具调用）
     */
    public static LlmMessage assistantWithToolCalls(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage("assistant", content, toolCalls, null);
    }

    /**
     * 创建 tool 结果消息
     */
    public static LlmMessage toolResult(String toolCallId, String content) {
        return new LlmMessage("tool", content, null, toolCallId);
    }

    /**
     * 转换为 Map 格式（用于 API 请求）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("role", role);

        if (content != null) {
            map.put("content", content);
        }

        if (toolCalls != null && !toolCalls.isEmpty()) {
            map.put("tool_calls", toolCalls.stream()
                    .map(LlmToolCall::toMap)
                    .toList());
        }

        if (toolCallId != null) {
            map.put("tool_call_id", toolCallId);
        }

        return map;
    }

    public boolean isUser() {
        return "user".equals(role);
    }

    public boolean isAssistant() {
        return "assistant".equals(role);
    }

    public boolean isSystem() {
        return "system".equals(role);
    }

    public boolean isTool() {
        return "tool".equals(role);
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
