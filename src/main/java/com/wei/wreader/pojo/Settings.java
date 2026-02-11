package com.wei.wreader.pojo;

/**
 * 配置信息
 * @author weizhanjie
 */
public class Settings {
    /**
     * 显示类型。1-侧边栏（默认）
     */
    public static final int DISPLAY_TYPE_SIDEBAR = 1;
    /**
     * 显示类型。2-底部状态栏
     */
    public static final int DISPLAY_TYPE_STATUSBAR = 2;
    /**
     * 显示类型。3-编辑器顶部提示
     */
    public static final int DISPLAY_TYPE_EDITOR_BANNER = 3;
    /**
     * 显示类型。4-终端控制台
     */
    public static final int DISPLAY_TYPE_TERMINAL = 4;
    /**
     * 显示类型。侧边栏（默认）
     */
    public static final String DISPLAY_TYPE_SIDEBAR_STR = "侧边栏";
    /**
     * 显示类型。底部状态栏
     */
    public static final String DISPLAY_TYPE_STATUSBAR_STR = "底部状态栏";
    /**
     * 显示类型。编辑器顶部提示
     */
    public static final String DISPLAY_TYPE_EDITOR_BANNER_STR = "编辑器横幅";
    /**
     * 显示类型。控制台终端
     */
    public static final String DISPLAY_TYPE_TERMINAL_STR = "控制台终端";
    /**
     * 数据加载模式--网络加载（默认）
     */
    public static final int DATA_LOAD_TYPE_NETWORK = 1;
    /**
     * 数据加载模式--本地加载
     */
    public static final int DATA_LOAD_TYPE_LOCAL = 2;
    /**
     * 自定义书源规则本文区域类型--2-IDEA代码编辑器（额外增加代码行数、代码高亮、代码折叠、格式化等）
     */
    public static final int CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR = 1;
    /**
     * 自定义书源规则本文区域类型--1-普通文本框(只有读写，无其它额外功能);
     */
    public static final int CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA = 2;
    /**
     * 自定义书源规则本文区域类型--1-普通文本框(只有读写，无其它额外功能);
     */
    public static final String CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA_TEXT = "普通文本框";
    /**
     * 自定义书源规则本文区域类型--2-IDEA代码编辑器（额外增加代码行数、代码高亮、代码折叠、格式化等）
     */
    public static final String CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR_TEXT = "代码编辑器";
    /**
     * 自定义书源规则本文区域类型--1-普通文本框(只有读写，无其它额外功能);
     */
    public static final String CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA_HINT = "只有读写，无其它额外功能";
    /**
     * 自定义书源规则本文区域类型--2-IDEA代码编辑器（额外增加代码行数、代码高亮、代码折叠、格式化等）
     */
    public static final String CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR_HINT = "拥有行数、高亮、报错、折叠等功能，但更消耗性能";
    /**
     * 最大单行字数
     */
    private int singleLineChars;
    /**
     * 是否显示行号
     */
    private boolean showLineNum;
    /**
     * 显示类型。1-侧边栏（默认），2-底部状态栏，3-左下角控制台
     */
    private int displayType;
    /**
     * 数据加载模式。1-网络加载，2-本地加载
     */
    private int dataLoadType;
    /**
     * 字符集
     */
    private String charset;
    /**
     * 自动阅读时间(秒)
     */
    private float autoReadTime;
    /**
     * 本地加载时是否显示图片
     */
    private boolean showLocalImg;
    /**
     * 主图标风格：1-默认，2-浅色
     */
    private int mainIconStyle;
    /**
     * 编辑器提示消息框宽度
     */
    private int editorHintWidth;
    /**
     * 编辑器提示消息框高度
     */
    private int editorHintHeight;
    /**
     * 自定义书源规则文本区域类型;<br>
     * 1-普通文本框(只有读写，无其它额外功能);<br>
     * 2-IDEA代码编辑器（额外增加代码行数、代码高亮、代码折叠、格式化等）
     */
    private int customSiteRuleTextAreaType;

    //--------------------------------------------
    //----------------  语音TTS  ------------------
    //--------------------------------------------
    /**
     * 音色
     */
    private String voiceRole;
    /**
     * 听书超时(秒)
     */
    private int audioTimeout;
    /**
     * 语速
     */
    private Float rate;
    /**
     * 音量
     */
    private Integer volume;
    /**
     * 语音风格
     */
    private String audioStyle;


    public int getSingleLineChars() {
        return singleLineChars;
    }

    public void setSingleLineChars(int singleLineChars) {
        this.singleLineChars = singleLineChars;
    }

    public boolean isShowLineNum() {
        return showLineNum;
    }

    public void setShowLineNum(boolean showLineNum) {
        this.showLineNum = showLineNum;
    }

    public int getDisplayType() {
        return displayType;
    }

    public void setDisplayType(int displayType) {
        this.displayType = displayType;
    }

    public int getDataLoadType() {
        return dataLoadType;
    }

    public void setDataLoadType(int dataLoadType) {
        this.dataLoadType = dataLoadType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public float getAutoReadTime() {
        return autoReadTime;
    }

    public void setAutoReadTime(float autoReadTime) {
        this.autoReadTime = autoReadTime;
    }

    public boolean isShowLocalImg() {
        return showLocalImg;
    }

    public void setShowLocalImg(boolean showLocalImg) {
        this.showLocalImg = showLocalImg;
    }

    public int getMainIconStyle() {
        return mainIconStyle;
    }

    public void setMainIconStyle(int mainIconStyle) {
        this.mainIconStyle = mainIconStyle;
    }

    public int getEditorHintWidth() {
        return editorHintWidth;
    }

    public void setEditorHintWidth(int editorHintWidth) {
        this.editorHintWidth = editorHintWidth;
    }

    public int getEditorHintHeight() {
        return editorHintHeight;
    }

    public void setEditorHintHeight(int editorHintHeight) {
        this.editorHintHeight = editorHintHeight;
    }

    public String getVoiceRole() {
        return voiceRole;
    }

    public void setVoiceRole(String voiceRole) {
        this.voiceRole = voiceRole;
    }

    public int getAudioTimeout() {
        return audioTimeout;
    }

    public void setAudioTimeout(int audioTimeout) {
        this.audioTimeout = audioTimeout;
    }

    public Float getRate() {
        return rate;
    }

    public void setRate(Float rate) {
        this.rate = rate;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public String getAudioStyle() {
        return audioStyle;
    }

    public void setAudioStyle(String audioStyle) {
        this.audioStyle = audioStyle;
    }

    public int getCustomSiteRuleTextAreaType() {
        return customSiteRuleTextAreaType;
    }

    public void setCustomSiteRuleTextAreaType(int customSiteRuleTextAreaType) {
        this.customSiteRuleTextAreaType = customSiteRuleTextAreaType;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "singleLineChars=" + singleLineChars +
                ", showLineNum=" + showLineNum +
                ", displayType=" + displayType +
                ", dataLoadType=" + dataLoadType +
                ", charset='" + charset + '\'' +
                ", autoReadTime=" + autoReadTime +
                ", showLocalImg=" + showLocalImg +
                ", mainIconStyle=" + mainIconStyle +
                ", editorHintWidth=" + editorHintWidth +
                ", editorHintHeight=" + editorHintHeight +
                ", customSiteRuleTextAreaType=" + customSiteRuleTextAreaType +
                ", voiceRole='" + voiceRole + '\'' +
                ", audioTimeout=" + audioTimeout +
                ", rate=" + rate +
                ", volume=" + volume +
                ", audioStyle='" + audioStyle + '\'' +
                '}';
    }
}
