package com.wei.wreader.agent.v2.runtime;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.agent.v2.event.AgentEvent;
import com.wei.wreader.agent.v2.llm.LlmClient;

import java.util.function.Consumer;

/**
 * Agent 运行器 - 简化 Agent 调用的入口
 * <p>
 * 提供便捷方法来创建和运行 Agent
 *
 * @author weizhanjie
 */
public class AgentRunner {

    private static final Logger LOG = Logger.getInstance(AgentRunner.class);

    private final LlmClient llmClient;
    private final Project project;

    public AgentRunner(LlmClient llmClient, Project project) {
        this.llmClient = llmClient;
        this.project = project;
    }

    /**
     * 同步运行 Agent
     *
     * @param agent       Agent 定义
     * @param userMessage 用户消息
     * @param eventHandler 事件处理器
     * @return Agent 结果
     */
    public AgentResult run(Agent agent, String userMessage, Consumer<AgentEvent> eventHandler) {
        // 创建上下文
        AgentContext context = agent.createContext();

        // 注册工具
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerAll(agent.getTools());

        // 创建 AgentLoop
        AgentLoop agentLoop = new AgentLoop(llmClient, toolRegistry, project);

        // 运行
        return agentLoop.run(agent, context, userMessage, eventHandler);
    }

    /**
     * 异步运行 Agent
     *
     * @param agent       Agent 定义
     * @param userMessage 用户消息
     * @param eventHandler 事件处理器
     * @param callback    完成回调
     */
    public void runAsync(Agent agent, String userMessage,
                         Consumer<AgentEvent> eventHandler, Consumer<AgentResult> callback) {
        // 创建上下文
        AgentContext context = agent.createContext();

        // 注册工具
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerAll(agent.getTools());

        // 创建 AgentLoop
        AgentLoop agentLoop = new AgentLoop(llmClient, toolRegistry, project);

        // 异步运行
        agentLoop.runAsync(agent, context, userMessage, eventHandler, callback);
    }

    /**
     * 创建带取消支持的 Agent 会话
     */
    public AgentSession createSession(Agent agent) {
        AgentContext context = agent.createContext();

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerAll(agent.getTools());

        AgentLoop agentLoop = new AgentLoop(llmClient, toolRegistry, project);

        return new AgentSession(agent, context, agentLoop);
    }

    /**
     * Agent 会话 - 支持取消和多次交互
     */
    public static class AgentSession {

        private final Agent agent;
        private final AgentContext context;
        private final AgentLoop agentLoop;
        private volatile Thread runningThread;

        public AgentSession(Agent agent, AgentContext context, AgentLoop agentLoop) {
            this.agent = agent;
            this.context = context;
            this.agentLoop = agentLoop;
        }

        /**
         * 异步执行
         */
        public void runAsync(String userMessage, Consumer<AgentEvent> eventHandler,
                             Consumer<AgentResult> callback) {
            runningThread = new Thread(() -> {
                try {
                    AgentResult result = agentLoop.run(agent, context, userMessage, eventHandler);
                    if (callback != null) {
                        callback.accept(result);
                    }
                } catch (Exception e) {
                    LOG.error("Agent session failed", e);
                    if (callback != null) {
                        callback.accept(AgentResult.failed(e.getMessage()));
                    }
                } finally {
                    runningThread = null;
                }
            }, "AgentSession-" + agent.getId());
            runningThread.setDaemon(true);
            runningThread.start();
        }

        /**
         * 取消执行
         */
        public void cancel() {
            context.cancel();
            Thread thread = runningThread;
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }

        /**
         * 是否正在运行
         */
        public boolean isRunning() {
            Thread thread = runningThread;
            return thread != null && thread.isAlive();
        }

        public Agent getAgent() {
            return agent;
        }

        public AgentContext getContext() {
            return context;
        }
    }
}
