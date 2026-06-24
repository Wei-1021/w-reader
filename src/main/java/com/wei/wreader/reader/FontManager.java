package com.wei.wreader.reader;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.AppConfigService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.data.ConstUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 字体管理器 - 管理字体大小、颜色、样式
 */
public class FontManager {
    private final CacheService cacheService;
    private final AppConfigService appConfig;

    private String fontFamily;
    private int fontSize;
    private String fontColorHex;

    public FontManager(CacheService cacheService, AppConfigService appConfig) {
        this.cacheService = cacheService;
        this.appConfig = appConfig;
    }

    /**
     * 初始化字体设置
     */
    public void initializeFontSettings() {
        fontFamily = cacheService.getFontFamily();
        if (fontFamily == null || fontFamily.isEmpty() || "JetBrains Mono".equals(fontFamily)) {
            fontFamily = ConstUtil.DEFAULT_FONT_FAMILY;
            cacheService.setFontFamily(fontFamily);
        }

        fontSize = cacheService.getFontSize();
        if (fontSize == 0) {
            fontSize = ConstUtil.DEFAULT_FONT_SIZE;
            cacheService.setFontSize(fontSize);
        }

        fontColorHex = cacheService.getFontColorHex();
        if (fontColorHex == null || fontColorHex.isEmpty()) {
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
            Color defaultForeground = scheme.getDefaultForeground();
            fontColorHex = String.format("#%02x%02x%02x",
                    defaultForeground.getRed(),
                    defaultForeground.getGreen(),
                    defaultForeground.getBlue());
            cacheService.setFontColorHex(fontColorHex);
        }
    }

    /**
     * 字体缩小
     */
    public void fontSizeSub() {
        fontFamily = cacheService.getFontFamily();
        if (fontSize == 0) fontSize = cacheService.getFontSize();
        if (fontSize <= 1) return;
        fontSize--;
        cacheService.setFontSize(fontSize);
    }

    /**
     * 字体放大
     */
    public void fontSizeAdd() {
        fontFamily = cacheService.getFontFamily();
        if (fontSize == 0) fontSize = cacheService.getFontSize();
        fontSize++;
        cacheService.setFontSize(fontSize);
    }

    /**
     * 改变字体颜色
     */
    public void changeFontColor() {
        Color color = changeFontColor(null, null);
        if (color != null) {
            fontColorHex = String.format("#%02x%02x%02x",
                    color.getRed(), color.getGreen(), color.getBlue());
            cacheService.setFontColorHex(fontColorHex);
        }
    }

    /**
     * 改变字体颜色
     */
    public Color changeFontColor(Component component, Color preselectedColor) {
        fontColorHex = cacheService.getFontColorHex();
        Color currentFontColor = Color.decode(fontColorHex);
        return JColorChooser.showDialog(component, "选择颜色", preselectedColor == null ? currentFontColor : preselectedColor);
    }

    public String getFontFamily() { return fontFamily; }
    public int getFontSize() { return fontSize; }
    public String getFontColorHex() { return fontColorHex; }
}
