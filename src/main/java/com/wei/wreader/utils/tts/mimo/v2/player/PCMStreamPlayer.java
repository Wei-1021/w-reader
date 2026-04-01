package com.wei.wreader.utils.tts.mimo.v2.player;

import javax.sound.sampled.*;

/**
 * PCM16 流式音频播放器
 * 使用 Java Sound API 播放 PCM16 裸数据
 *
 * 参数：16kHz 采样率，16bit，单声道
 */
public class PCMStreamPlayer implements AudioPlayer {

    private static final int SAMPLE_RATE = 24000;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private final SourceDataLine line;
    private volatile boolean playing = false;
    private volatile long bytesWritten = 0;

    /**
     * 创建 PCM16 流式播放器
     */
    public PCMStreamPlayer() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_BITS,
                CHANNELS,
                SIGNED,
                BIG_ENDIAN
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

    /**
     * 创建指定参数的播放器
     */
    public PCMStreamPlayer(int sampleRate, int sampleSizeBits, int channels)
            throws LineUnavailableException {

        AudioFormat format = new AudioFormat(
                sampleRate,
                sampleSizeBits,
                channels,
                SIGNED,
                BIG_ENDIAN
        );

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("PCM line not supported with given format");
        }

        this.line = (SourceDataLine) AudioSystem.getLine(info);
        this.line.open(format);
        this.line.start();
        this.playing = true;
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        if (playing && line.isOpen()) {
            int written = line.write(data, offset, length);
            bytesWritten += written;
        }
    }

    @Override
    public void stop() {
        playing = false;
        if (line.isOpen()) {
            line.drain();
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

    /**
     * 获取底层 SourceDataLine
     */
    public SourceDataLine getLine() {
        return line;
    }
}
