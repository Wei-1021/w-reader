package com.wei.wreader.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtil {

    public static Integer getInt(JsonObject json, String key) {
        JsonElement jsonElement = json.get(key);
        return jsonElement == null ? 0 : jsonElement.getAsInt();
    }

    public static Float getFloat(JsonObject json, String key) {
        JsonElement jsonElement = json.get(key);
        return jsonElement == null ? 0 : jsonElement.getAsFloat();
    }

    public static Long getLong(JsonObject json, String key) {
        JsonElement jsonElement = json.get(key);
        return jsonElement == null ? 0 : jsonElement.getAsLong();
    }

    public static Double getDouble(JsonObject json, String key) {
        JsonElement jsonElement = json.get(key);
        return jsonElement == null ? 0 : jsonElement.getAsDouble();
    }

    public static String getString(JsonObject json, String key, String defaultValue) {
        JsonElement jsonElement = json.get(key);
        return jsonElement == null ? defaultValue : jsonElement.getAsString();
    }

    public static Boolean getBoolean(JsonObject json, String key) {
        JsonElement jsonElement = json.get(key);
        return jsonElement != null && jsonElement.getAsBoolean();
    }

    public static String getString(JsonObject json, String key) {
        JsonElement jsonElement = json.get(key);
        return jsonElement == null ? "" : jsonElement.getAsString();
    }

    public static JsonElement get(JsonObject json, String key) {
        try {
            return json.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断是否为有效得的JSON字符串
     * @param jsonString
     * @return
     */
    public static boolean isValid(String jsonString) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            return jsonObject != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
