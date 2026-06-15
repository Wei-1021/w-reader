package com.wei.wreader.tts.mimo.enums;

/**
 * MiMo TTS v2.5 音频输出格式
 */
public enum AudioFormat {

    WAV("wav", "WAV格式，包含完整头部信息"),
    PCM16("pcm16", "PCM16裸数据，适合流式处理");

    private final String value;
    private final String description;

    AudioFormat(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static AudioFormat fromValue(String value) {
        for (AudioFormat format : values()) {
            if (format.value.equals(value)) {
                return format;
            }
        }
        return PCM16;
    }

    @Override
    public String toString() {
        return description;
    }
}
