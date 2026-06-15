package com.wei.wreader.model;

public enum DataLoadType {
    NETWORK(1, "网络加载"),
    LOCAL_FILE(2, "本地加载");

    private final int legacyValue;
    private final String displayName;

    DataLoadType(int legacyValue, String displayName) {
        this.legacyValue = legacyValue;
        this.displayName = displayName;
    }

    public int toLegacyValue() { return legacyValue; }
    public String getDisplayName() { return displayName; }

    public static DataLoadType fromLegacy(int v) {
        for (DataLoadType t : values()) {
            if (t.legacyValue == v) return t;
        }
        return NETWORK;
    }
}
