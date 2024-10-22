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
     * 最大单行字数
     */
    private int singleLineChars;
    /**
     * 是否显示行号
     */
    private boolean isShowLineNum;
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
    private int autoReadTime;

    public int getSingleLineChars() {
        return singleLineChars;
    }

    public void setSingleLineChars(int singleLineChars) {
        this.singleLineChars = singleLineChars;
    }

    public boolean isShowLineNum() {
        return isShowLineNum;
    }

    public void setShowLineNum(boolean showLineNum) {
        isShowLineNum = showLineNum;
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

    public int getAutoReadTime() {
        return autoReadTime;
    }

    public void setAutoReadTime(int autoReadTime) {
        this.autoReadTime = autoReadTime;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "singleLineChars=" + singleLineChars +
                ", isShowLineNum=" + isShowLineNum +
                ", displayType=" + displayType +
                ", dataLoadType=" + dataLoadType +
                ", charset='" + charset + '\'' +
                ", autoReadTime=" + autoReadTime +
                '}';
    }
}
