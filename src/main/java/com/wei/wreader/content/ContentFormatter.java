package com.wei.wreader.content;

import com.wei.wreader.util.data.ConstUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.regex.Pattern;

/**
 * 内容格式化器 - 负责文本内容的清洗和格式化
 */
public class ContentFormatter {

    /**
     * 处理章节内容文本（去除HTML标签等）
     */
    public static String processChapterContentText(String contentHtml) {
        Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
        String text = pattern.matcher(contentHtml).replaceAll("　");
        text = StringUtils.normalizeSpace(text);
        return StringEscapeUtils.unescapeHtml4(text);
    }

    /**
     * 从HTML内容中提取纯文本
     */
    public static String extractTextFromHtml(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
        String text = pattern.matcher(html).replaceAll("　");
        text = StringUtils.normalizeSpace(text);
        return StringEscapeUtils.unescapeHtml4(text);
    }

    /**
     * 移除style标签
     */
    public static String removeStyleTags(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        return html.replaceAll("(?s)<style[^>]*>.*?</style>", "");
    }
}
