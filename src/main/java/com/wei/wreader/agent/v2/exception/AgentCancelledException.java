package com.wei.wreader.agent.v2.exception;

/**
 * Agent 取消异常
 *
 * @author weizhanjie
 */
public class AgentCancelledException extends RuntimeException {

    public AgentCancelledException() {
        super("Agent 已取消");
    }

    public AgentCancelledException(String message) {
        super(message);
    }
}
