package com.wei.wreader.utils.data;

import org.apache.commons.lang3.StringUtils;

/**
 * 数字工具类
 *
 * @author weizhanjie
 */
public class NumberUtil {
    private static final String[] NUMBER_STRING = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

    public static Integer parseInt(String str) {
        if (StringUtils.isBlank(str)) {
            return 0;
        }

        return Integer.valueOf(str);
    }

    /**
     * float
     */
    public static Float parseFloat(String str) {
        if (StringUtils.isBlank(str)) {
            return 0f;
        }
        return Float.valueOf(str);
    }
}
