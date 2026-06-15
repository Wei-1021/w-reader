package com.wei.wreader.content;

import com.wei.wreader.model.BookInfo;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.StringUtil;

/**
 * HTML内容渲染器 - 负责生成带样式的HTML内容
 */
public class HtmlContentRenderer {

    /**
     * 构建原始样式内容（使用网页原有CSS）
     */
    public static String buildOriginalStyleContent(String chapterContent, String contentOriginalStyle,
                                                    String fontColorHex, int fontSize,
                                                    BookInfo bookInfo) {
        chapterContent = String.format("""
                <div class="%s" style="color:%s;font-size:%dpx;">%s</div>
                """, ConstUtil.NEW_FONT_CLASS_NAME, fontColorHex, fontSize, chapterContent);

        return StringUtil.buildFullHtml(bookInfo.getBookName(), contentOriginalStyle, chapterContent);
    }

    /**
     * 构建自定义样式内容（使用自定义字体样式）
     */
    public static String buildCustomStyleContent(String chapterContent, String fontColorHex,
                                                  String fontFamily, int fontSize) {
        return String.format("""
                <div style="color:%s;font-family:'%s';font-size:%dpx;">%s</div>
                """, fontColorHex, fontFamily, fontSize, chapterContent);
    }

    /**
     * 获取带样式的内容（通用方法）
     */
    public static String getStyledContent(String text, String fontFamily, int fontSize, String fontColorHex) {
        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";

        text = text.replaceAll("(?s)<style[^>]*>.*?</style>", "");
        return "<div style=\"" + style + "\">" + text + "</div>";
    }

    /**
     * 构建章节标题HTML
     */
    public static String buildChapterTitleHtml(String title, String fontColorHex) {
        return "<h3 style=\"text-align: center;margin-bottom: 20px;color:" +
                fontColorHex + ";\">" + title + "</h3>";
    }
}
