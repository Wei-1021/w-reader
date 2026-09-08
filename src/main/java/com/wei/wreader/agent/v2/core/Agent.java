package com.wei.wreader.agent.v2.core;

import java.util.List;
import java.util.UUID;

/**
 * Agent 接口 - 负责声明，不负责循环
 * <p>
 * Agent = System Prompt + Tools + 业务逻辑
 * 不应该自己实现 while 循环，循环由 AgentLoop 控制
 *
 * @author weizhanjie
 */
public interface Agent {

    /**
     * 获取 Agent 唯一标识
     */
    String getId();

    /**
     * 获取 Agent 名称（用于 UI 显示）
     */
    String getName();

    /**
     * 获取 System Prompt
     */
    String getSystemPrompt();

    /**
     * 获取 Agent 需要的工具列表
     */
    List<Tool> getTools();

    /**
     * 创建 Agent 上下文（可覆盖以提供自定义上下文）
     */
    default AgentContext createContext() {
        return new AgentContext(getId() + "-" + UUID.randomUUID());
    }
}
