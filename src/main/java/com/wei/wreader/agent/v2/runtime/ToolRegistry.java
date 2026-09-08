package com.wei.wreader.agent.v2.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.diagnostic.Logger;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.agent.v2.exception.AgentCancelledException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 工具注册表 - 负责 Tool 注册、查询、执行
 *
 * @author weizhanjie
 */
public final class ToolRegistry {

    private static final Logger LOG = Logger.getInstance(ToolRegistry.class);
    private static final ExecutorService TOOL_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "WReader-AgentTool");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /**
     * 注册工具
     */
    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        LOG.info("Registered tool: " + tool.getName());
    }

    /**
     * 批量注册工具
     */
    public void registerAll(List<Tool> toolList) {
        for (Tool tool : toolList) {
            register(tool);
        }
    }

    /**
     * 获取工具
     */
    public Tool get(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有工具
     */
    public List<Tool> getAll() {
        return List.copyOf(tools.values());
    }

    /**
     * 获取工具名称列表
     */
    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * 执行工具
     *
     * @param name      工具名称
     * @param context   工具上下文
     * @param arguments 参数
     * @return 工具执行结果
     */
    public ToolResult execute(String name, ToolContext context, JsonNode arguments) {
        Tool tool = tools.get(name);

        if (tool == null) {
            LOG.warn("Unknown tool: " + name);
            return ToolResult.failure("Unknown tool: " + name);
        }
        if (tool.permission() != ToolPermission.READ_ONLY && tool.permission() != ToolPermission.NETWORK) {
            return ToolResult.failure("当前 Agent 不允许执行此工具权限: " + tool.permission());
        }

        try {
            context.throwIfCancelled();
            LOG.info("Executing tool: " + name);
            long startTime = System.currentTimeMillis();

            Future<ToolResult> future = TOOL_EXECUTOR.submit(() -> tool.execute(context, arguments));
            ToolResult result;
            try {
                result = future.get(tool.timeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return ToolResult.failure("工具执行超时: " + tool.timeout().toSeconds() + " 秒");
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                return ToolResult.failure("工具执行已取消");
            }

            long duration = System.currentTimeMillis() - startTime;
            LOG.info("Tool " + name + " executed in " + duration + "ms");

            return result;
        } catch (AgentCancelledException e) {
            LOG.info("Tool " + name + " cancelled");
            return ToolResult.failure("已取消");
        } catch (Exception e) {
            LOG.warn("Tool " + name + " execution failed", e);
            return ToolResult.failure(e.getMessage());
        }
    }

    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
    }
}
