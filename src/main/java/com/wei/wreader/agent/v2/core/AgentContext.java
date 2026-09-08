package com.wei.wreader.agent.v2.core;

import com.wei.wreader.agent.v2.llm.LlmMessage;
import com.wei.wreader.agent.v2.exception.AgentCancelledException;

import java.util.*;

/**
 * Agent 上下文 - 整个 Agent Runtime 的状态容器
 * <p>
 * 保存消息历史、迭代次数、取消状态、业务属性等
 *
 * @author weizhanjie
 */
public class AgentContext {

    private final String sessionId;
    private final List<LlmMessage> messages = new ArrayList<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private volatile boolean cancelled = false;
    private int iteration = 0;
    private int maxIterations = 30;
    private int toolCallCount = 0;
    private int maxToolCalls = 100;
    private int maxContextChars = 120_000;
    private long maxDurationMillis = 10 * 60 * 1000L;
    private long startedAtMillis = System.currentTimeMillis();

    public AgentContext(String sessionId) {
        this.sessionId = sessionId;
    }

    // ==================== Session ====================

    public String getSessionId() {
        return sessionId;
    }

    // ==================== 消息历史 ====================

    public List<LlmMessage> getMessages() {
        return messages;
    }

    public void addMessage(LlmMessage message) {
        messages.add(message);
    }

    public void addUserMessage(String content) {
        messages.add(LlmMessage.user(content));
    }

    public void addAssistantMessage(String content) {
        messages.add(LlmMessage.assistant(content));
    }

    public void addToolResult(String toolCallId, String content) {
        messages.add(LlmMessage.toolResult(toolCallId, content));
    }

    // ==================== 迭代控制 ====================

    public int getIteration() {
        return iteration;
    }

    public void nextIteration() {
        iteration++;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public boolean isMaxIterationsReached() {
        return iteration >= maxIterations;
    }

    public int getToolCallCount() {
        return toolCallCount;
    }

    public void recordToolCall() {
        toolCallCount++;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public void setMaxDurationMillis(long maxDurationMillis) {
        this.maxDurationMillis = maxDurationMillis;
    }

    public boolean isMaxToolCallsReached() {
        return toolCallCount >= maxToolCalls;
    }

    public boolean isDurationExceeded() {
        return System.currentTimeMillis() - startedAtMillis >= maxDurationMillis;
    }

    public boolean isContextLimitExceeded() {
        int size = 0;
        for (LlmMessage message : messages) {
            if (message.content() != null) {
                size += message.content().length();
            }
            if (message.toolCalls() != null) {
                for (var toolCall : message.toolCalls()) {
                    size += toolCall.arguments().toString().length();
                }
            }
            if (size > maxContextChars) {
                return true;
            }
        }
        return false;
    }

    // ==================== 取消控制 ====================

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    public void throwIfCancelled() throws AgentCancelledException {
        if (cancelled) {
            throw new AgentCancelledException("Agent 已取消");
        }
    }

    // ==================== 业务属性 ====================

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void put(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) attributes.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        Object value = attributes.get(key);
        return value != null ? (T) value : defaultValue;
    }

    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    public void remove(String key) {
        attributes.remove(key);
    }

    // ==================== 重置 ====================

    public void reset() {
        messages.clear();
        attributes.clear();
        cancelled = false;
        iteration = 0;
        toolCallCount = 0;
        startedAtMillis = System.currentTimeMillis();
    }
}
