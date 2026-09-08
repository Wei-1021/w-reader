package com.wei.wreader.agent.v2.core;

/**
 * Agent 执行结果
 *
 * @author weizhanjie
 */
public record AgentResult(
        Status status,
        String content,
        String errorMessage
) {

    public enum Status {
        /** 成功完成 */
        SUCCESS,
        /** 达到最大迭代次数 */
        MAX_ITERATIONS,
        /** 已取消 */
        CANCELLED,
        /** 执行失败 */
        FAILED
    }

    public static AgentResult success(String content) {
        return new AgentResult(Status.SUCCESS, content, null);
    }

    public static AgentResult maxIterations() {
        return new AgentResult(Status.MAX_ITERATIONS, null, "已达到最大迭代次数");
    }

    public static AgentResult cancelled() {
        return new AgentResult(Status.CANCELLED, null, "已取消");
    }

    public static AgentResult failed(String errorMessage) {
        return new AgentResult(Status.FAILED, null, errorMessage);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }
}
