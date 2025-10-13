package com.wei.wreader.utils.comm;

import com.intellij.openapi.editor.colors.EditorColorsManager;

public class ThemeUtils {

    /**
     * 判断当前主题是否为深色主题
     * @return true: 深色主题, false: 浅色主题
     */
    public static boolean isDarkTheme() {
        // 获取EditorColorsManager实例（单例）
        EditorColorsManager colorsManager = EditorColorsManager.getInstance();
        // 判断是否为深色主题
        return colorsManager.isDarkEditor();
    }
}