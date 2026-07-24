package com.wei.wreader.agent;

/**
 * Agent 事件回调接口
 *
 * @author weizhanjie
 */
public interface AgentCallback {

    /**
     * Agent 输出文本（思考/分析过程）
     *
     * @param text 文本内容
     */
    void onMessage(String text);

    /**
     * 工具调用开始
     *
     * @param toolName  工具名称
     * @param arguments 工具参数（JSON 字符串）
     */
    void onToolCall(String toolName, String arguments);

    /**
     * 工具执行完成
     *
     * @param toolName 工具名称
     * @param result   执行结果
     */
    void onToolResult(String toolName, String result);

    /**
     * 最终规则生成完成
     *
     * @param siteRuleJson 生成的 SiteBean JSON 规则
     */
    void onComplete(String siteRuleJson);

    /**
     * 错误
     *
     * @param error 错误信息
     */
    void onError(String error);
}
