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
     * 本章节下一页内容的链接（针对某些网站会把一章内容分成多页的情况）。<br>
     * 可以是请求链接，也可以用代码，然后返回一个链接
     */
    private String nextContentUrl;
    /**
     * 本章节下一页内容是否为使用API请求的方式获取。<br>
     * true: 使用API请求方式获取。<br>
     * false: 使用HTML页面获取。
     */
    private boolean useNextContentApi;
    /**
     * 本章节下一页内容为API请求时，获取章节内融的JSONPath规则
     */
    private String nextContentApiDataRule;
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

    public String getNextContentUrl() {
        return nextContentUrl;
    }

    public void setNextContentUrl(String nextContentUrl) {
        this.nextContentUrl = nextContentUrl;
    }

    public boolean isUseNextContentApi() {
        return useNextContentApi;
    }

    public void setUseNextContentApi(boolean useNextContentApi) {
        this.useNextContentApi = useNextContentApi;
    }

    public String getNextContentApiDataRule() {
        return nextContentApiDataRule;
    }

    public void setNextContentApiDataRule(String nextContentApiDataRule) {
        this.nextContentApiDataRule = nextContentApiDataRule;
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
                ", nextContentUrl='" + nextContentUrl + '\'' +
                ", useNextContentApi=" + useNextContentApi +
                ", nextContentApiDataRule='" + nextContentApiDataRule + '\'' +
                ", contentRegexList=" + contentRegexList +
                '}';
    }
}
