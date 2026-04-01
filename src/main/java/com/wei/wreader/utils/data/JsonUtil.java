package com.wei.wreader.utils.data;

import com.google.gson.*;

import java.util.HashMap;
import java.util.Map;

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
     *
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

    /**
     * 将 JsonObject 转换为 Map
     *
     * @param jsonObject JsonObject 对象
     * @return Map<String, Object>
     */
    public static Map<String, Object> convertJsonObjectToMap(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            map.put(key, jsonObject.get(key));
        }
        return map;
    }

    /**
     * 将 String 转换为 Map
     *
     * @param jsonStr
     * @return Map<String, Object>
     */
    public static Map<String, Object> convertStringToMap(String jsonStr) {
        JsonObject jsonObject = getJsonObject(jsonStr);
        return jsonObject != null ? convertJsonObjectToMap(jsonObject) : null;
    }

    /**
     * 将 Json 字符串转换为 JsonObject
     *
     * @param jsonString
     * @return
     */
    public static JsonObject getJsonObject(String jsonString) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(jsonString, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将 Json 字符串转换为 JsonArray
     *
     * @param jsonString
     * @return
     */
    public static JsonArray getJsonArray(String jsonString) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(jsonString, JsonArray.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
