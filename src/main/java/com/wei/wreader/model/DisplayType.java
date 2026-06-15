package com.wei.wreader.model;

public enum DisplayType {
    SIDEBAR(1, "侧边栏"),
    STATUSBAR(2, "底部状态栏"),
    EDITOR_BANNER(3, "编辑器横幅"),
    TERMINAL(4, "控制台终端");

    private final int legacyValue;
    private final String displayName;

    DisplayType(int legacyValue, String displayName) {
        this.legacyValue = legacyValue;
        this.displayName = displayName;
    }

    public int toLegacyValue() { return legacyValue; }
    public String getDisplayName() { return displayName; }

    public static DisplayType fromLegacy(int v) {
        for (DisplayType t : values()) {
            if (t.legacyValue == v) return t;
        }
        return SIDEBAR;
    }
}
