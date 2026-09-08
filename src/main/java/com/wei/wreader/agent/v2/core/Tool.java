package com.wei.wreader.agent.v2.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;

/**
 * Tool 接口 - Agent 与真实世界之间的桥梁
 * <p>
 * 所有外部能力均通过 Tool 暴露，便于：
 * - 权限控制
 * - 超时控制
 * - 日志记录
 * - UI 展示
 * - 测试
 *
 * @author weizhanjie
 */
public interface Tool {

    /**
     * 获取工具名称（唯一标识）
     */
    String getName();

    /**
     * 获取工具描述（用于 LLM 理解工具功能）
     */
    String getDescription();

    /**
     * 获取参数 Schema（JSON Schema 格式）
     */
    JsonNode getParametersSchema();

    /**
     * 执行工具
     *
     * @param context   工具上下文
     * @param arguments 参数（JSON 格式）
     * @return 工具执行结果
     * @throws Exception 执行异常
     */
    ToolResult execute(ToolContext context, JsonNode arguments) throws Exception;

    /**
     * 获取工具超时时间（默认 30 秒）
     */
    default Duration timeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * 获取工具权限（默认只读）
     */
    default ToolPermission permission() {
        return ToolPermission.READ_ONLY;
    }
}
