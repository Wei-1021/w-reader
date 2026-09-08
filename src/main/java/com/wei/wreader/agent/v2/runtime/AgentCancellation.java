package com.wei.wreader.agent.v2.runtime;

import com.wei.wreader.agent.v2.exception.AgentCancelledException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 取消令牌
 * <p>
 * 所有 AgentLoop、Tool、HTTP、LLM 都应该检查 CancellationToken
 *
 * @author weizhanjie
 */
public final class AgentCancellation {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 请求取消
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * 如果已取消，抛出异常
     */
    public void throwIfCancelled() throws AgentCancelledException {
        if (cancelled.get()) {
            throw new AgentCancelledException();
        }
    }

    /**
     * 重置（用于复用）
     */
    public void reset() {
        cancelled.set(false);
    }
}
