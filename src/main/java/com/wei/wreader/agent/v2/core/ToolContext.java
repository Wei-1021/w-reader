package com.wei.wreader.agent.v2.core;

import com.intellij.openapi.project.Project;

/**
 * 工具上下文 - 给 Tool 提供运行环境
 *
 * @author weizhanjie
 */
public final class ToolContext {

    private final Project project;
    private final AgentContext agentContext;

    public ToolContext(Project project, AgentContext agentContext) {
        this.project = project;
        this.agentContext = agentContext;
    }

    /**
     * 获取 IDEA Project（可能为 null，CLI 模式下）
     */
    public Project getProject() {
        return project;
    }

    /**
     * 获取 Agent 上下文
     */
    public AgentContext getAgentContext() {
        return agentContext;
    }

    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return agentContext.isCancelled();
    }

    /**
     * 抛出取消异常（如果已取消）
     */
    public void throwIfCancelled() {
        agentContext.throwIfCancelled();
    }
}
