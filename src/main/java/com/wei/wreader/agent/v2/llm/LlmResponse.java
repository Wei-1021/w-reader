package com.wei.wreader.agent.v2.llm;

import java.util.List;

/**
 * LLM 响应
 *
 * @author weizhanjie
 */
public record LlmResponse(
        String content,
        List<LlmToolCall> toolCalls,
        String finishReason
) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    public boolean isStop() {
        return "stop".equals(finishReason);
    }

    public boolean isToolCalls() {
        return "tool_calls".equals(finishReason);
    }
}
