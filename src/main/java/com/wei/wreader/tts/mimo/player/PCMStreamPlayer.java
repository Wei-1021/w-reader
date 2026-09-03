package com.wei.wreader.tts.mimo.player;

import javax.sound.sampled.*;

/**
 * MiMo TTS v2.5 PCM16 流式音频播放器
 * 使用 Java Sound API 播放 PCM16 裸数据
 * 参数：24kHz 采样率，16bit，单声道
 */
public class PCMStreamPlayer implements AudioPlayer {

    private static final int SAMPLE_RATE = 24000;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private final SourceDataLine line;
    private volatile boolean playing = false;
    private volatile boolean paused = false;
    private volatile long bytesWritten = 0;

    private final Object pauseLock = new Object();

    public PCMStreamPlayer() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
                SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN
        );
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("PCM16 line not supported");
        }
        this.line = (SourceDataLine) AudioSystem.getLine(info);
        this.line.open(format);
        this.line.start();
        this.playing = true;
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        if (!playing || !line.isOpen()) return;

        // 暂停时阻塞等待恢复，不丢弃数据
        synchronized (pauseLock) {
            while (paused && playing) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // 写入数据，但分小块写入以便快速响应停止
        if (playing && line.isOpen()) {
            // 将数据分成小块写入，每块最多 4096 字节
            int chunkSize = 4096;
            int remaining = length;
            int pos = offset;

            while (remaining > 0 && playing && line.isOpen()) {
                int writeSize = Math.min(chunkSize, remaining);
                int written = line.write(data, pos, writeSize);
                bytesWritten += written;
                pos += written;
                remaining -= written;
            }
        }
    }

    @Override
    public void pause() {
        if (playing && !paused && line.isOpen()) {
            paused = true;
            line.stop();
        }
    }

    @Override
    public void resume() {
        if (playing && paused && line.isOpen()) {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
            line.start();
        }
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void stop() {
        playing = false;
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        if (line.isOpen()) {
            // 不使用 drain()，直接停止并清空缓冲
            line.flush();  // 清空缓冲区中的数据
            line.stop();
            line.close();
        }
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public long getBytesWritten() {
        return bytesWritten;
    }
}
