package com.wei.wreader.tts.mimo.player;

import javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * MiMo TTS v2.5 MP3 文件播放器（基于 JLayer）
 */
public class MP3FilePlayer {

    private AdvancedPlayer player;
    private volatile boolean playing = false;

    /**
     * 播放 MP3 文件（阻塞）
     */
    public void play(String filePath) throws Exception {
        try (InputStream is = new FileInputStream(filePath)) {
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
                if (listener != null) listener.onComplete();
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        }).start();
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

    public boolean isPlaying() {
        return playing;
    }

    public interface PlaybackCompleteListener {
        void onComplete();
        void onError(Exception e);
    }
}
