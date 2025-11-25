package com.wei.wreader.utils.tts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wei.wreader.utils.file.FileUtil;

/**
 * 音色风格
 */
public class VoiceRoleStyle {
    private static final String EDGETTS_ROLE_STYLE = "json/EdgeTTS-Role-Style.json";

    /**
     * 根据音色获取此音色下包含的风格
     * @param voiceRoleValue 音色名称
     * @return
     */
    public static VoiceStyle[] getByRoleValue(String voiceRoleValue) {
        if (voiceRoleValue == null) {
            return new VoiceStyle[]{};
        }

        VoiceRole voiceRole = VoiceRole.valueOf(voiceRoleValue);
        return getVoiceRoleStyle(voiceRole);
    }

    /**
     * 根据音色获取此音色下包含的风格
     * @param voiceRoleNickName 音色昵称
     * @return
     */
    public static VoiceStyle[] getByRoleNickName(String voiceRoleNickName) {
        if (voiceRoleNickName == null) {
            return new VoiceStyle[]{};
        }

        return getVoiceRoleStyle(VoiceRole.getByNickName(voiceRoleNickName));
    }

    /**
     * 根据音色获取此音色下包含的风格
     * @param voiceRole
     * @return
     */
    public static VoiceStyle[] getVoiceRoleStyle(VoiceRole voiceRole) {
        if (voiceRole == null) {
            return new VoiceStyle[]{};
        }

        String roleStylesStr = FileUtil.readResourcesJsonStr(EDGETTS_ROLE_STYLE);
        if (roleStylesStr == null || roleStylesStr.isEmpty()) {
            return new VoiceStyle[]{};
        }
        String name = voiceRole.name();

        // 转成Json对象
        JsonObject jsonObject = JsonParser.parseString(roleStylesStr).getAsJsonObject();
        JsonObject roleStyleJsonObject = jsonObject.getAsJsonObject(name);
        if (roleStyleJsonObject == null) {
            return new VoiceStyle[]{};
        }

        JsonArray styles = roleStyleJsonObject.getAsJsonArray("styles");
        if (styles == null) {
            return new VoiceStyle[]{};
        }

        VoiceStyle[] voiceStyles = new VoiceStyle[styles.size()];
        for (int i = 0, len = styles.size(); i < len; i++) {
            voiceStyles[i] = VoiceStyle.getByValue(styles.get(i).getAsString());
        }

        return voiceStyles;
    }
}
