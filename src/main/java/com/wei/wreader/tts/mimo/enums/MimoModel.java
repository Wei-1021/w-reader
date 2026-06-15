package com.wei.wreader.tts.mimo.enums;

import com.wei.wreader.util.SettingConstants;

/**
 * MiMo TTS v2.5 模型枚举
 */
public enum MimoModel {

    PRESET("mimo-v2.5-tts", "预置音色", "风格指令",
            "提示：直接用一句话描述想要的语音风格。",
            SettingConstants.VOICE_STYLE_PRESETS),
    VOICE_DESIGN("mimo-v2.5-tts-voicedesign", "文本描述音色", "音色描述",
            "提示：请简述音色特征，如角色身份、适配场景、年龄性别、口音语言、语气情绪、语速音量等。",
            SettingConstants.VOICE_DESC_PRESETS),
    VOICE_CLONE("mimo-v2.5-tts-voiceclone", "音频样本复刻（无效）", "音色描述",
            "",
            new String[][]{});

    private final String modelId;
    private final String description;
    private final String voiceDescriptionLabel;
    private final String voiceDescriptionTip;
    private final String[][] voiceStylePresets;

    MimoModel(String modelId, String description, String voiceDescriptionLabel,
              String voiceDescriptionTip, String[][] voiceStylePresets) {
        this.modelId = modelId;
        this.description = description;
        this.voiceDescriptionLabel = voiceDescriptionLabel;
        this.voiceDescriptionTip = voiceDescriptionTip;
        this.voiceStylePresets = voiceStylePresets;
    }

    public String getModelId() {
        return modelId;
    }

    public String getDescription() {
        return description;
    }

    public String getVoiceDescriptionLabel() {
        return voiceDescriptionLabel;
    }

    public String getVoiceDescriptionTip() {
        return voiceDescriptionTip;
    }

    public String[][] getVoiceStylePresets() {
        return voiceStylePresets;
    }

    /**
     * 获取显示名称（用于 UI 下拉框）
     */
    public String getDisplayName() {
        return modelId + " (" + description + ")";
    }

    /**
     * 根据 modelId 查找枚举
     */
    public static MimoModel fromModelId(String modelId) {
        if (modelId == null) {
            return PRESET;
        }
        for (MimoModel model : values()) {
            if (model.modelId.equals(modelId)) {
                return model;
            }
        }
        return PRESET;
    }

    /**
     * 根据显示名称查找枚举
     */
    public static MimoModel fromDisplayName(String displayName) {
        if (displayName == null) {
            return PRESET;
        }
        for (MimoModel model : values()) {
            if (model.getDisplayName().equals(displayName)) {
                return model;
            }
        }
        return PRESET;
    }

    /**
     * 根据索引查找枚举（0=PRESET, 1=VOICE_DESIGN, 2=VOICE_CLONE）
     */
    public static MimoModel fromIndex(int index) {
        MimoModel[] values = values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return PRESET;
    }

    /**
     * 获取枚举索引
     */
    public int toIndex() {
        return ordinal();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
