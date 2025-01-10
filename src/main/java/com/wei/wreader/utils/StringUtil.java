package com.wei.wreader.utils;

import com.jayway.jsonpath.JsonPath;

import java.util.List;

public class StringUtil {
    /**
     * 将字符串按照指定的最大字符数分割成多个子字符串
     *
     * @param str      原始字符串
     * @param maxChars 单行最大字符数
     * @return 分割后的字符串数组
     */
    public static List<String> splitStringByMaxCharList(String str, int maxChars) {
        return List.of(splitStringByMaxChars(str, maxChars));
    }

    /**
     * 将字符串按照指定的最大字符数分割成多个子字符串
     *
     * @param str      原始字符串
     * @param maxChars 单行最大字符数
     * @return 分割后的字符串数组
     */
    public static String[] splitStringByMaxChars(String str, int maxChars) {
        if (str == null || str.isEmpty() || maxChars <= 0) {
            return new String[]{};
        }

        int length = str.length();
        int parts = length / maxChars; // 计算大致的分割数量
        if (length % maxChars != 0) {
            parts++; // 如果有余数，则分割数量加1
        }

        String[] result = new String[parts];

        for (int i = 0, start = 0; i < parts; i++) {
            int end = Math.min(start + maxChars, length);
            result[i] = str.substring(start, end);
            start = end;
        }

        return result;
    }

    public static void main(String[] args) {
        String jsonString = "{\"code\":0,\"message\":\"SUCCESS\"," +
                "\"data\":[" +
                    "{\"book_id\": \"123456\",  \"book_data\": [{\"book_name\": \"bk001\"}]}, " +
                    "{\"book_id\": \"123457\",  \"book_data\": [{\"book_name\": \"bk002\"}]}" +
                "]}";

        // 获取所有 data 项
        Object read = JsonPath.read(jsonString, "$.data[0].book_data[0]");
        System.out.println(read);

        String ss = "<java>1111</java>2222{123: 521}";
        // 提取<java>和</java>之间的内容
        String extractedContent1 = ss.substring(ss.indexOf("<java>") + "<java>".length(), ss.indexOf("</java>"));
        System.out.println(extractedContent1);
        // 提取</java>之后的内容
        String extractedContent2 = ss.substring(ss.indexOf("</java>") + "</java>".length());
        System.out.println(extractedContent2);

        // 提取{和}之间的内容
        String extractedContent3 = ss.substring(ss.indexOf("{") + 1, ss.indexOf("}"));
        System.out.println(extractedContent3);
    }
}
