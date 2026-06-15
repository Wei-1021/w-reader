package com.wei.wreader.tts.mimo.enums;

/**
 * MiMo TTS v2.5 音色枚举
 */
public enum Voice {

    MIMO_DEFAULT("mimo_default", "MiMo-默认"),
    BINGTANG("冰糖", "冰糖"),
    MOLI("茉莉", "茉莉"),
    SUDA("苏打", "苏打"),
    BAIHUA("白桦", "白桦"),
    MIA("Mia", "Mia"),
    CHLOE("Chloe", "Chloe"),
    MILO("Milo", "Milo"),
    DEan("Dean", "Dean");

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
        return MIMO_DEFAULT;
    }

    @Override
    public String toString() {
        return description;
    }
}
