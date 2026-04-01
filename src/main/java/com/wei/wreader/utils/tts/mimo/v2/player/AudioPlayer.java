package com.wei.wreader.utils.tts.mimo.v2.player;

/**
 * 音频播放器接口
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
