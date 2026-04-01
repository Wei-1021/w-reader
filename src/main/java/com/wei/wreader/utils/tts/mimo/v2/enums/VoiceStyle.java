package com.wei.wreader.utils.tts.mimo.v2.enums;

/**
 * MiMo TTS 语音风格枚举
 */
public enum VoiceStyle {

    // ==================== 语速控制 ====================
    SPEED_UP("变快"),
    SPEED_DOWN("变慢"),

    // ==================== 情绪 ====================
    HAPPY("开心"),
    SAD("悲伤"),
    ANGRY("生气"),

    // ==================== 角色扮演 ====================
    SUN_WUKONG("孙悟空"),
    LIN_DAIYU("林黛玉"),

    // ==================== 风格变化 ====================
    WHISPER("悄悄话"),
    SQUEAKY_VOICE("夹子音"),
    TAIWAN_ACCENT("台湾腔"),

    // ==================== 方言 ====================
    NORTHEAST_DIALECT("东北话"),
    SICHUAN_DIALECT("四川话"),
    HENAN_DIALECT("河南话"),
    CANTONESE("粤语"),

    // ==================== 特殊 ====================
    SING("唱歌");

    private final String value;

    VoiceStyle(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 包装文本，添加风格标签
     */
    public String wrapText(String text) {
        return String.format("<style>%s</style>%s", value, text);
    }

    /**
     * 包装自定义风格文本
     */
    public static String wrapCustomText(String style, String text) {
        return String.format("<style>%s</style>%s", style, text);
    }

    public static VoiceStyle fromValue(String value) {
        for (VoiceStyle style : values()) {
            if (style.value.equals(value)) {
                return style;
            }
        }
        throw new IllegalArgumentException("Unknown voice style: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
