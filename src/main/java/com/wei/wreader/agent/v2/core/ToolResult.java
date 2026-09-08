package com.wei.wreader.agent.v2.core;

/**
 * 工具执行结果
 * <p>
 * LLM 只看到 content，不需要知道 Java Exception
 *
 * @author weizhanjie
 */
public record ToolResult(
        boolean success,
        String content
) {

    public static ToolResult success(String content) {
        return new ToolResult(true, content);
    }

    public static ToolResult failure(String message) {
        return new ToolResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }
}
