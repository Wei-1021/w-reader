package com.wei.wreader.agent.v2.runtime;

import com.intellij.openapi.diagnostic.Logger;
import com.wei.wreader.agent.v2.core.AgentResult;

/**
 * Agent 追踪器 - 记录 Agent 执行过程
 *
 * @author weizhanjie
 */
public class AgentTracer {

    private static final Logger LOG = Logger.getInstance(AgentTracer.class);

    private final String sessionId;
    private long startTime;
    private int toolCallCount;

    public AgentTracer(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 开始追踪
     */
    public void start() {
        startTime = System.currentTimeMillis();
        toolCallCount = 0;
        LOG.info("[Agent:" + sessionId + "] Started");
    }

    /**
     * 记录迭代
     */
    public void traceIteration(int iteration) {
        LOG.info("[Agent:" + sessionId + "] Iteration: " + iteration);
    }

    /**
     * 记录 LLM 调用
     */
    public void traceLlmCall(int messageCount, int toolCount) {
        LOG.info("[Agent:" + sessionId + "] LLM call - messages: " + messageCount + ", tools: " + toolCount);
    }

    /**
     * 记录工具调用
     */
    public void traceToolCall(String toolName) {
        toolCallCount++;
        LOG.info("[Agent:" + sessionId + "] Tool call: " + toolName + " (total: " + toolCallCount + ")");
    }

    /**
     * 记录工具结果
     */
    public void traceToolResult(String toolName, boolean success, long durationMs) {
        LOG.info("[Agent:" + sessionId + "] Tool result: " + toolName +
                " - " + (success ? "success" : "failure") +
                " (" + durationMs + "ms)");
    }

    /**
     * 记录完成
     */
    public void traceComplete(AgentResult result) {
        long duration = System.currentTimeMillis() - startTime;
        LOG.info("[Agent:" + sessionId + "] Completed - status: " + result.status() +
                ", duration: " + duration + "ms, tool calls: " + toolCallCount);
    }

    /**
     * 记录错误
     */
    public void traceError(Throwable error) {
        long duration = System.currentTimeMillis() - startTime;
        LOG.error("[Agent:" + sessionId + "] Error after " + duration + "ms", error);
    }

    /**
     * 记录取消
     */
    public void traceCancel() {
        long duration = System.currentTimeMillis() - startTime;
        LOG.info("[Agent:" + sessionId + "] Cancelled after " + duration + "ms, tool calls: " + toolCallCount);
    }
}
