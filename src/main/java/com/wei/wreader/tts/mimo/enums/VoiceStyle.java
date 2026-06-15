package com.wei.wreader.tts.mimo.enums;

/**
 * MiMo TTS v2.5 语音风格枚举
 * 风格通过在文本开头添加 (风格) 标签控制
 */
public enum VoiceStyle {

    // 基础情绪
    HAPPY("开心"),
    SAD("悲伤"),
    ANGRY("愤怒"),
    FEAR("恐惧"),
    SURPRISE("惊讶"),
    EXCITED("兴奋"),
    GRIEVANCE("委屈"),
    CALM("平静"),
    COLD("冷漠"),

    // 复合情绪
    MELANCHOLY("怅然"),
    GRATIFIED("欣慰"),
    HELPLESS("无奈"),
    GUILTY("愧疚"),
    RELIEVED("释然"),
    JEALOUS("嫉妒"),
    WEARY("厌倦"),
    UNEASY("忐忑"),
    EMOTIONAL("动情"),

    // 整体语调
    GENTLE("温柔"),
    COOL("高冷"),
    LIVELY("活泼"),
    SERIOUS("严肃"),
    LAZY("慵懒"),
    PLAYFUL("俏皮"),
    DEEP("深沉"),
    CAPABLE("干练"),
    SHARP("凌厉"),

    // 音色定位
    MAGNETIC("磁性"),
    MELLOW("醇厚"),
    CLEAR("清亮"),
    ETHEREAL("空灵"),
    YOUNG("稚嫩"),
    OLD("苍老"),
    SWEET("甜美"),
    HOARSE("沙哑"),
    ELEGANT("醇雅"),

    // 人设腔调
    SQUEAKY_VOICE("夹子音"),
    QUEEN_VOICE("御姐音"),
    SHOTA_VOICE("正太音"),
    UNCLE_VOICE("大叔音"),
    TAIWAN_ACCENT("台湾腔"),

    // 方言
    NORTHEAST_DIALECT("东北话"),
    SICHUAN_DIALECT("四川话"),
    HENAN_DIALECT("河南话"),
    CANTONESE("粤语"),

    // 角色扮演
    SUN_WUKONG("孙悟空"),
    LIN_DAIYU("林黛玉"),

    // 特殊
    SING("唱歌");

    private final String value;

    VoiceStyle(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 包装文本，添加风格标签 (风格)文本
     */
    public String wrapText(String text) {
        return String.format("(%s)%s", value, text);
    }

    /**
     * 包装自定义风格文本
     */
    public static String wrapCustomText(String style, String text) {
        return String.format("(%s)%s", style, text);
    }

    /**
     * 检查文本是否已包含风格标签
     */
    public static boolean hasStyleTag(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        return first == '(' || first == '（' || first == '[' || first == '【';
    }

    public static VoiceStyle fromValue(String value) {
        for (VoiceStyle style : values()) {
            if (style.value.equals(value)) {
                return style;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
