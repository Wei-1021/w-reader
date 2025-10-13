package com.wei.wreader.pojo;

import java.util.List;

/**
 * 章节规则
 * @author weizhanjie
 */
public class ChapterRules {
    /**
     * 章节内容url
     */
    private String url;
    /**
     * 章节内容JSONPath规则
     */
    private String urlDataRule;
    /**
     * 章节内容处理规则
     */
    private String contentHandleRule;
    /**
     * 是否使用原网页的css样式
     */
    private boolean useContentOriginalStyle;
    /**
     * 原网页的css样式字符替换正则
     */
    private String replaceContentOriginalRegex;
    /**
     * 章节内容的HTML标签元素名称
     */
    private String contentElementName;
    /**
     * 小说内容的正则表达式集合，每项用@replace:分隔，前面是正则表达式，后面是替换的内容
     */
    private List<String> contentRegexList;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlDataRule() {
        return urlDataRule;
    }

    public void setUrlDataRule(String urlDataRule) {
        this.urlDataRule = urlDataRule;
    }

    public String getContentHandleRule() {
        return contentHandleRule;
    }

    public void setContentHandleRule(String contentHandleRule) {
        this.contentHandleRule = contentHandleRule;
    }

    public boolean isUseContentOriginalStyle() {
        return useContentOriginalStyle;
    }

    public void setUseContentOriginalStyle(boolean useContentOriginalStyle) {
        this.useContentOriginalStyle = useContentOriginalStyle;
    }

    public String getReplaceContentOriginalRegex() {
        return replaceContentOriginalRegex;
    }

    public void setReplaceContentOriginalRegex(String replaceContentOriginalRegex) {
        this.replaceContentOriginalRegex = replaceContentOriginalRegex;
    }

    public String getContentElementName() {
        return contentElementName;
    }

    public void setContentElementName(String contentElementName) {
        this.contentElementName = contentElementName;
    }

    public List<String> getContentRegexList() {
        return contentRegexList;
    }

    public void setContentRegexList(List<String> contentRegexList) {
        this.contentRegexList = contentRegexList;
    }

    @Override
    public String toString() {
        return "ChapterRules{" +
                "url='" + url + '\'' +
                ", urlDataRule='" + urlDataRule + '\'' +
                ", contentHandleRule='" + contentHandleRule + '\'' +
                ", useContentOriginalStyle=" + useContentOriginalStyle +
                ", replaceContentOriginalRegex='" + replaceContentOriginalRegex + '\'' +
                ", contentElementName='" + contentElementName + '\'' +
                ", contentRegexList=" + contentRegexList +
                '}';
    }
}
