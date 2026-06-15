package com.wei.wreader.content;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.jayway.jsonpath.JsonPath;
import com.wei.wreader.model.ChapterRules;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.util.comm.ScriptCodeUtil;
import com.wei.wreader.util.comm.StringTemplateEngine;
import com.wei.wreader.util.comm.MethodExecutor;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.ListUtil;
import com.wei.wreader.util.http.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 内容解析器 - 负责从HTML/API响应中解析章节内容
 */
public class ContentParser {
    private static final Logger LOG = Logger.getInstance(ContentParser.class);

    /**
     * 判断是否应该使用API方式加载内容
     */
    public static boolean shouldUseApiMethod(ChapterRules rules) {
        String contentUrl = rules.getUrl();
        String dataRule = rules.getUrlDataRule();
        return StringUtils.isNotBlank(contentUrl) && StringUtils.isNotBlank(dataRule);
    }

    /**
     * 通过API方式加载内容
     */
    public static String loadContentViaApi(String url, ChapterRules rules) throws Exception {
        HttpRequestBase requestBase = HttpUtil.commonRequest(url);
        requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = httpClient.execute(requestBase)) {
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);
                JsonObject menuListJson = new Gson().fromJson(result, JsonObject.class);
                Object readJson = JsonPath.read(menuListJson.toString(), rules.getUrlDataRule());
                return readJson.toString();
            }
        }
        return "";
    }

    /**
     * 通过HTML方式加载内容
     */
    public static HtmlParseResult loadContentViaHtml(String url, ChapterRules rules) throws IOException {
        Document document = Jsoup.connect(url)
                .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                .get();

        Element headElement = document.head();
        Element bodyElement = document.body();
        Elements chapterContentElements = bodyElement.select(rules.getContentElementName());

        if (chapterContentElements.isEmpty()) {
            return new HtmlParseResult("", "", bodyElement, "");
        }

        StringBuilder contentHtml = new StringBuilder();
        for (Element element : chapterContentElements) {
            Tag tag = element.tag();
            String html = element.html();
            if (!tag.isEmpty() && StringUtils.trimToNull(html) != null) {
                contentHtml.append(String.format("<%s>%s</%s>",
                        tag.normalName(), html, tag.normalName()));
            }
        }

        String contentText = chapterContentElements.text();
        String bodyHtml = bodyElement.html();

        // 处理原始样式
        String contentOriginalStyle = "";
        if (rules.isUseContentOriginalStyle()) {
            contentOriginalStyle = extractOriginalStyles(headElement, rules);
        }

        return new HtmlParseResult(contentHtml.toString(), contentText, bodyElement, contentOriginalStyle);
    }

    /**
     * 提取原始CSS样式
     */
    private static String extractOriginalStyles(Element headElement, ChapterRules rules) {
        StringBuilder allStyle = new StringBuilder();
        Elements styles = headElement.getElementsByTag("style");

        for (Element style : styles) {
            String styleText = style.html();
            String replacement = rules.getReplaceContentOriginalRegex();
            styleText = styleText.replaceAll(replacement, ConstUtil.NEW_FONT_CLASS_CSS_NAME);
            styleText = styleText.replaceAll(ConstUtil.HTML_TAG_REGEX_STR, "");
            allStyle.append(styleText);
        }

        return "<style>" + allStyle + "</style>";
    }

    /**
     * 处理小说内容（应用各种规则和正则表达式）
     */
    public static String handleContent(String content, SiteBean siteBean) throws Exception {
        if (siteBean == null) {
            return content;
        }

        ChapterRules chapterRules = siteBean.getChapterRules();
        if (chapterRules == null) {
            return content;
        }

        String handleRule = chapterRules.getContentHandleRule();
        String processedContent = processContentByRule(content, handleRule);
        return formatAndApplyRegex(processedContent, chapterRules);
    }

    /**
     * 根据规则处理内容
     */
    private static String processContentByRule(String content, String handleRule) throws Exception {
        if (ScriptCodeUtil.isJavaCodeConfig(handleRule)) {
            return executeContentHandleScript(content, handleRule);
        }
        return content;
    }

    /**
     * 执行内容处理脚本
     */
    private static String executeContentHandleScript(String content, String handleRule) throws Exception {
        if (ScriptCodeUtil.isOldJavaCodeConfig(handleRule)) {
            String renderedScript = StringTemplateEngine.render(handleRule, Map.of("content", content));
            return MethodExecutor.executeMethod(renderedScript).toString();
        } else {
            return (String) ScriptCodeUtil.getScriptCodeExeResult(
                    handleRule,
                    new Class[]{String.class},
                    new Object[]{content},
                    Map.of("content", content)
            );
        }
    }

    /**
     * 格式化内容并应用正则规则
     */
    public static String formatAndApplyRegex(String content, ChapterRules rules) {
        String formattedContent = content.replaceAll("\\n", "<br/>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");

        List<String> contentRegexList = rules.getContentRegexList();
        if (ListUtil.isNotEmpty(contentRegexList)) {
            for (String contentRegex : contentRegexList) {
                String[] regulars = contentRegex.split(ConstUtil.SPLIT_REGEX_REPLACE_FLAG);
                String regex = regulars[0];
                String replacement = regulars.length > 1 ? regulars[1] : "";
                formattedContent = formattedContent.replaceAll(regex, replacement);
            }
        }
        return formattedContent;
    }

    /**
     * HTML解析结果
     */
    public static class HtmlParseResult {
        private final String contentHtml;
        private final String contentText;
        private final Element bodyElement;
        private final String contentOriginalStyle;

        public HtmlParseResult(String contentHtml, String contentText, Element bodyElement, String contentOriginalStyle) {
            this.contentHtml = contentHtml;
            this.contentText = contentText;
            this.bodyElement = bodyElement;
            this.contentOriginalStyle = contentOriginalStyle;
        }

        public String getContentHtml() { return contentHtml; }
        public String getContentText() { return contentText; }
        public Element getBodyElement() { return bodyElement; }
        public String getContentOriginalStyle() { return contentOriginalStyle; }
    }
}
