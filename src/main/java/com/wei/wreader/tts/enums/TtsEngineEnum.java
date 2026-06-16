package com.wei.wreader.tts.enums;

/**
 * TTS 引擎枚举
 */
public enum TtsEngineEnum {

    EDGE("edge", "Edge TTS", null, null, null),
    MIMO("mimo", "MiMo TTS", 
         "https://platform.xiaomimimo.com/console/api-keys",
         "<html>MiMo API Key 不支持 Token Plan 的 API Key<br>MiMo TTS 限时免费<br>请前往「API Keys」页面申请 API Key</html>",
         "MiMo 音色");

    private final String engineId;
    private final String displayName;
    private final String apiKeyUrl;
    private final String apiKeyHint;
    private final String voiceGroupName;

    TtsEngineEnum(String engineId, String displayName, String apiKeyUrl, String apiKeyHint, String voiceGroupName) {
        this.engineId = engineId;
        this.displayName = displayName;
        this.apiKeyUrl = apiKeyUrl;
        this.apiKeyHint = apiKeyHint;
        this.voiceGroupName = voiceGroupName;
    }

    public String getEngineId() {
        return engineId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiKeyUrl() {
        return apiKeyUrl;
    }

    public String getApiKeyHint() {
        return apiKeyHint;
    }

    public String getVoiceGroupName() {
        return voiceGroupName;
    }

    /**
     * 是否支持 API Key
     */
    public boolean hasApiKey() {
        return apiKeyUrl != null;
    }

    /**
     * 根据 engineId 查找枚举
     */
    public static TtsEngineEnum fromEngineId(String engineId) {
        if (engineId == null) {
            return EDGE;
        }
        for (TtsEngineEnum engine : values()) {
            if (engine.engineId.equals(engineId)) {
                return engine;
            }
        }
        return EDGE;
    }

    /**
     * 获取所有引擎 ID
     */
    public static String[] getEngineIds() {
        String[] ids = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            ids[i] = values()[i].engineId;
        }
        return ids;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
