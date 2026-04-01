package com.wei.wreader.utils.tts.mimo.v2.player;

import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * MP3 文件播放器（基于 JLayer）
 * 用于播放已保存的 MP3 文件
 */
public class MP3FilePlayer {

    private AdvancedPlayer player;
    private volatile boolean playing = false;
    private volatile boolean paused = false;
    private final Object lock = new Object();

    /**
     * 播放 MP3 文件（阻塞）
     */
    public void play(String filePath) throws Exception {
        try (InputStream is = new java.io.FileInputStream(filePath)) {
            player = new AdvancedPlayer(is);
            playing = true;
            player.play();
        } finally {
            playing = false;
        }
    }

    /**
     * 播放 MP3 字节数组（阻塞）
     */
    public void play(byte[] mp3Data) throws Exception {
        try (InputStream is = new ByteArrayInputStream(mp3Data)) {
            player = new AdvancedPlayer(is);
            playing = true;
            player.play();
        } finally {
            playing = false;
        }
    }

    /**
     * 异步播放 MP3 文件
     */
    public void playAsync(String filePath, PlaybackCompleteListener listener) {
        new Thread(() -> {
            try {
                play(filePath);
                if (listener != null) {
                    listener.onComplete();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }).start();
    }

    /**
     * 异步播放 MP3 字节数组
     */
    public void playAsync(byte[] mp3Data, PlaybackCompleteListener listener) {
        new Thread(() -> {
            try {
                play(mp3Data);
                if (listener != null) {
                    listener.onComplete();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }).start();
    }

    /**
     * 播放 MP3 流式数据（阻塞）
     * 将流式接收到的 PCM16 数据转为 MP3 后播放需要额外编码，
     * 此方法用于播放已经转成 MP3 格式的流
     */
    public void playStream(InputStream mp3Stream) throws Exception {
        player = new AdvancedPlayer(mp3Stream);
        playing = true;
        try {
            player.play();
        } finally {
            playing = false;
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        playing = false;
        if (player != null) {
            player.close();
        }
    }

    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * 播放完成回调
     */
    public interface PlaybackCompleteListener {
        void onComplete();
        void onError(Exception e);
    }
}
