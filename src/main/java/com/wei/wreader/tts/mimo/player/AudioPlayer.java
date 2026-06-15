package com.wei.wreader.tts.mimo.player;

/**
 * MiMo TTS v2.5 音频播放器接口
 */
public interface AudioPlayer {

    /**
     * 写入音频数据块
     */
    void write(byte[] data, int offset, int length);

    /**
     * 停止播放
     */
    void stop();

    /**
     * 是否正在播放
     */
    boolean isPlaying();

    /**
     * 获取已播放的字节数
     */
    long getBytesWritten();
}
