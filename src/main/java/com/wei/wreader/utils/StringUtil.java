package com.wei.wreader.utils;

import java.util.List;

public class StringUtil {
    /**
     * 将字符串按照指定的最大字符数分割成多个子字符串
     *
     * @param str     原始字符串
     * @param maxChars 单行最大字符数
     * @return 分割后的字符串数组
     */
    public static List<String> splitStringByMaxCharList(String str, int maxChars) {
        return List.of(splitStringByMaxChars(str, maxChars));
    }

    /**
     * 将字符串按照指定的最大字符数分割成多个子字符串
     *
     * @param str     原始字符串
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
        String longString = "这里是一段非常长的中文字符串，用于测试分割功能，确保它能够按照指定的最大字符数正确地被分割成多个子字符串。";
        int maxCharsPerLine = 10; // 设定每行最大字符数为10

        String[] parts = splitStringByMaxChars(longString, maxCharsPerLine);

        for (String part : parts) {
            System.out.println(part);
        }
    }
}
