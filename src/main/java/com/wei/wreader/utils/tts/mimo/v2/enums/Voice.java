package com.wei.wreader.utils.tts.mimo.v2.enums;

/**
 * MiMo TTS 支持的音色
 */
public enum Voice {

    MIMO_DEFAULT("mimo_default", "MiMo-默认"),
    DEFAULT_ZH("default_zh", "MiMo-中文女声"),
    DEFAULT_EN("default_en", "MiMo-英文女声");

    private final String value;
    private final String description;

    Voice(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static Voice fromValue(String value) {
        for (Voice voice : values()) {
            if (voice.value.equals(value)) {
                return voice;
            }
        }
        throw new IllegalArgumentException("Unknown voice: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
