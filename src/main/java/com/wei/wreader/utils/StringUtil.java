package com.wei.wreader.utils;

import com.jayway.jsonpath.JsonPath;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
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
     * @param maxChars 单行最大字符数base64Decode
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

    /**
     * BASE64解码
     * @param content 待解码的Base64字符串
     * @param isConvert 是否进行转换
     */
    public static String base64Decode(String content, Boolean isConvert) {
        try {
            if (isConvert) {
                content = convertString(content);
            }
            return new String(Base64.getDecoder().decode(content), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String base64Encode(String content, Boolean isConvert) {
        if (isConvert) {
            content = convertString(content);
        }
        return Base64.getEncoder().encodeToString(content.getBytes());
    }

    /**
     * 将输入字符串中的字母进行特定的转换
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    public static String convertString(String input) {
        StringBuilder result = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                int charCode = (int) (double) ((int) c / 97);
                int i = (Character.toLowerCase(c) - 83) % 26;
                int kValue = i != 0 ? i : 26;

                char convertedChar = (char) (kValue + (charCode == 0 ? 64 : 96));
                result.append(convertedChar);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static Object jsonPathRead(String jsonString, String path) {
        return JsonPath.read(jsonString, path);
    }

    public static void main(String[] args) {
        String jsonString = """
                    {"code":0,"message":"SUCCESS",
                    "data":[
                        {"book_id": "123456",
                         "book_data": [
                            {"book_name": "bk001"},
                            {"book_name": "bk002"}
                         ]},
                        {"book_id": "123457",
                         "book_data": [
                            {"book_name": "bk003"},
                            {"book_name": "bk004"}
                         ]}
                    ]}
                """;

        // 获取所有 data 项
        Object read = JsonPath.read(jsonString, "$.data[*].book_data[*]");
        Object read2 = JsonPath.read(read, "$.[1].book_name");
        System.out.println(read);
        System.out.println(read2);

        System.out.println(convertString("Az"));
    }
}
