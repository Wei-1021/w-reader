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
     * 显示类型。3-控制台终端
     */
    public static final int DISPLAY_TYPE_TERMINAL = 3;
    /**
     * 显示类型。侧边栏（默认）
     */
    public static final String DISPLAY_TYPE_SIDEBAR_STR = "侧边栏";
    /**
     * 显示类型。底部状态栏
     */
    public static final String DISPLAY_TYPE_STATUSBAR_STR = "底部状态栏";
    /**
     * 显示类型。控制台终端
     */
    public static final String DISPLAY_TYPE_TERMINAL_STR = "控制台终端";


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
}
