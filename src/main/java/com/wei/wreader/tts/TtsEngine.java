package com.wei.wreader.tts;

import java.util.function.Consumer;

/**
 * TTS引擎统一接口
 */
public interface TtsEngine {
    /**
     * 开始语音合成
     * @param text 要合成的文本
     */
    void synthesize(String text);

    /**
     * 开始播放
     */
    void start();

    /**
     * 停止播放
     */
    void stop();

    /**
     * 释放资源
     */
    void dispose();

    /**
     * 是否正在播放
     */
    boolean isPlaying();

    /**
     * 设置播放完成回调
     */
    void setOnComplete(Runnable callback);

    /**
     * 设置错误回调
     */
    void setOnError(Consumer<Exception> callback);
}
