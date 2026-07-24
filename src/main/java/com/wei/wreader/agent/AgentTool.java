package com.wei.wreader.agent;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Agent 工具接口
 *
 * @author weizhanjie
 */
public interface AgentTool {

    /**
     * 工具名称（英文，LLM 调用时使用）
     */
    String getName();

    /**
     * 工具描述（给 LLM 看的说明）
     */
    String getDescription();

    /**
     * 工具参数的 JSON Schema（OpenAI function calling 格式）
     */
    String getParametersSchema();

    /**
     * 执行工具
     *
     * @param arguments 工具参数（JSON 对象）
     * @return 执行结果（纯文本或 JSON 字符串）
     * @throws Exception 执行异常
     */
    String execute(JsonNode arguments) throws Exception;
}
