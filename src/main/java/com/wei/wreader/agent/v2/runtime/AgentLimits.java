package com.wei.wreader.agent.v2.runtime;

import java.time.Duration;

/**
 * Agent 限制 - 防止无限循环和资源耗尽
 *
 * @author weizhanjie
 */
public class AgentLimits {

    /** 最大迭代次数 */
    private int maxIterations = 30;

    /** 最大工具调用次数 */
    private int maxToolCalls = 100;

    /** 最大上下文字符数 */
    private int maxContextChars = 120_000;

    /** 最大执行时长 */
    private Duration maxDuration = Duration.ofMinutes(10);

    public AgentLimits() {
    }

    public AgentLimits(int maxIterations, int maxToolCalls, int maxContextChars, Duration maxDuration) {
        this.maxIterations = maxIterations;
        this.maxToolCalls = maxToolCalls;
        this.maxContextChars = maxContextChars;
        this.maxDuration = maxDuration;
    }

    // ==================== Getters & Setters ====================

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public Duration getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(Duration maxDuration) {
        this.maxDuration = maxDuration;
    }

    // ==================== 预设配置 ====================

    /**
     * 默认配置
     */
    public static AgentLimits defaults() {
        return new AgentLimits();
    }

    /**
     * 书源生成配置（更宽松）
     */
    public static AgentLimits siteRule() {
        return new AgentLimits(
                35,           // 更多迭代
                150,          // 更多工具调用
                200_000,      // 更大上下文
                Duration.ofMinutes(15)  // 更长时长
        );
    }

    /**
     * 快速配置（更严格）
     */
    public static AgentLimits fast() {
        return new AgentLimits(
                10,
                30,
                50_000,
                Duration.ofMinutes(3)
        );
    }
}
