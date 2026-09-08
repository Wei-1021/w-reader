package com.wei.wreader.agent.v2.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.wei.wreader.agent.v2.core.AgentResult;
import com.wei.wreader.agent.v2.core.ToolResult;

/**
 * Agent 事件 - UI 不应该直接依赖 Agent 内部实现
 * <p>
 * 通过事件流通知 UI 更新
 *
 * @author weizhanjie
 */
public sealed interface AgentEvent
        permits AgentEvent.Started,
                AgentEvent.AssistantMessage,
                AgentEvent.ToolCallStarted,
                AgentEvent.ToolCallFinished,
                AgentEvent.Completed,
                AgentEvent.Failed,
                AgentEvent.Cancelled {

    /**
     * Agent 开始执行
     */
    record Started(String sessionId) implements AgentEvent {}

    /**
     * Assistant 消息
     */
    record AssistantMessage(String content) implements AgentEvent {}

    /**
     * 工具调用开始
     */
    record ToolCallStarted(String name, JsonNode arguments) implements AgentEvent {}

    /**
     * 工具调用完成
     */
    record ToolCallFinished(String name, ToolResult result) implements AgentEvent {}

    /**
     * Agent 执行完成
     */
    record Completed(AgentResult result) implements AgentEvent {}

    /**
     * Agent 执行失败
     */
    record Failed(Throwable error) implements AgentEvent {}

    /**
     * Agent 已取消
     */
    record Cancelled() implements AgentEvent {}
}
