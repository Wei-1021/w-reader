package com.wei.wreader.utils.data;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Map工具类
 *
 * @author weizhanjie
 */
public class MapUtil {

    /**
     * 获取Key对应的String值，当结果为null时，返回空字符串
     *
     * @param map
     * @param key
     * @return
     */
    public static String getString(Map<String, Object> map, String key) {
        return getNotNullString(map, key);
    }

    /**
     * 获取Key对应的String值，当结果为null时，返回空字符串
     *
     * @param map
     * @param key
     * @return
     */
    public static String getNotNullString(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        try {
            Object obj = map.get(key);
            if (obj == null) {
                return "";
            }

            return String.valueOf(obj);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取Key对应的Integer值，当结果为null时，返回0
     *
     * @param map
     * @param key
     * @return
     */
    public static Integer getInt(Map<String, Object> map, String key) {
        return getNotNullInt(map, key);
    }

    /**
     * 获取Key对应的Integer值，当结果为null时，返回0
     *
     * @param map
     * @param key
     * @return
     */
    public static Integer getNotNullInt(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return 0;
        }

        Object obj = map.get(key);
        if (obj == null) {
            return 0;
        }

        try {
            return obj instanceof Number ? ((Number) obj).intValue() : Integer.parseInt((String) obj);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取Key对应的Long值，当结果为null时，返回0
     *
     * @param map
     * @param key
     * @return
     */
    public static Long getNotNullLong(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return 0L;
        }

        Object obj = map.get(key);
        if (obj == null) {
            return 0L;
        }

        try {
            return obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong((String) obj);
        } catch (Exception e) {
            return 0L;
        }
    }
    /**
     * 获取Key对应的Float值，当结果为null时，返回0
     *
     * @param map
     * @param key
     * @return
     */
    public static Float getFloat(Map<String, Object> map, String key) {
        return getNotNullFloat(map, key);
    }


    /**
     * 获取Key对应的Float值，当结果为null时，返回0
     *
     * @param map
     * @param key
     * @return
     */
    public static Float getNotNullFloat(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return 0F;
        }

        Object obj = map.get(key);
        if (obj == null) {
            return 0F;
        }

        try {
            return obj instanceof Number ? ((Number) obj).floatValue() : Float.parseFloat((String) obj);
        } catch (Exception e) {
            return 0F;
        }
    }

    /**
     * 获取Key对应的Double值，当结果为null时，返回0
     *
     * @param map
     * @param key
     * @return
     */
    public static Double getDouble(Map<String, Object> map, String key) {
        return getNotNullDouble(map, key);
    }

    /**
     * 获取Key对应的Double值，当结果为null时，返回0
     *
     * @param map
     * @param key
     * @return
     */
    public static Double getNotNullDouble(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return 0d;
        }

        Object obj = map.get(key);
        if (obj == null) {
            return 0d;
        }

        try {
            return obj instanceof Number ? ((Number) obj).doubleValue() : Double.parseDouble((String) obj);
        } catch (Exception e) {
            return 0d;
        }
    }

    /**
     * 获取Key对应的Boolean值，当结果为null时，返回false
     * @param map
     * @param key
     * @return
     */
    public static Boolean getBoolean(Map<String, Object> map, String key) {
        return getNotNullBoolean(map, key);
    }

    /**
     * 获取Key对应的Boolean值，当结果为null时，返回false
     * @param map
     * @param key
     * @return
     */
    public static Boolean getNotNullBoolean(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return false;
        }

        Object obj = map.get(key);
        if (obj == null) {
            return false;
        }

        try {
            return obj instanceof Boolean ? (Boolean) obj : Boolean.parseBoolean((String) obj);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取Key对应的List数据，当结果为null时，返回空集合
     *
     * @param map
     * @param key
     * @return
     */
    public static <T> List<T> getNotNullList(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return new ArrayList<>();
        }

        Object obj = map.get(key);
        if (obj == null) {
            return new ArrayList<>();
        }

        return (List<T>) obj;
    }

    /**
     * 获取Key对应的Set数据，当结果为null时，返回空集合
     *
     * @param map
     * @param key
     * @return
     */
    public static <T> Set<T> getNotNullSet(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return new HashSet<>();
        }

        Object obj = map.get(key);
        if (obj == null) {
            return new HashSet<>();
        }

        return (Set<T>) obj;
    }

    /**
     * 获取Key对应的Map数据，当结果为null时，返回空集合
     *
     * @param map
     * @param key
     * @return
     */
    public static <K, V> Map<K, V> getNotNullMap(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return new HashMap<>();
        }

        Object obj = map.get(key);
        if (obj == null) {
            return new HashMap<>();
        }

        return (Map<K, V>) obj;
    }

    /**
     * 获取Key对应的值
     *
     * @param key
     * @return
     */
    public static <V> V get(Map<String, V> map, String key) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return map.get(key);
    }

    public static String toJson(Map<String, Object> map ){
        String json="";
		try {
	        ObjectMapper objectMapper = new ObjectMapper();
			json = objectMapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return json;
    }
    
    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        map.put("1", new ArrayList<>());
        System.out.println(getNotNullList(map, "1"));
    }

}
