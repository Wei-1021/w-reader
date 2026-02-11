package com.wei.wreader.pojo;

import java.io.Serializable;

/**
 * 小说站点信息
 *
 * @author weizhanjie
 */
public class BookSiteInfo implements Serializable {
    private static final long serialVersionUID = -95175826485100254L;
    /**
     * 是否启用
     */
    private Boolean isEnabled;
    /**
     * 站点ID
     */
    private String id;
    /**
     * 站点名称
     */
    private String name;
    /**
     * 站点根目录
     */
    private String baseUrl;
    /**
     * 请求头
     */
    private String header;
    /**
     * 搜索小说的url
     */
    private String searchUrl;
    /**
     * 获取小说列表的JSONPath规则
     */
    private String searchDataBookListRule;
    /**
     * 小说列表的HTML标签元素名称
     */
    private String bookListElementName;
    /**
     * 书本列表链接元素CssSelector，规则分段，用@分隔<br>
     * {@code @back}:表示获取到的内容在后面加上@back:之后的内容<br>
     * {@code @front}:表示获取到的内容在前面加上@front:之后的内容
     */
    private String bookListUrlElement;
    /**
     * 小说标题列表元素CssSelector
     */
    private String bookListTitleElement;
    /**
     * 小说目录列表URL
     */
    private String listMainUrl;
    /**
     * 小说目录列表JSONPath规则
     */
    private String listMainUrlDataRule;
    /**
     * 小说目录列表项id字段名称
     */
    private String listMainItemIdField;
    /**
     * 小说目录列表项标题字段名称
     */
    private String listMainItemTitleField;
    /**
     * 小说目录列表的HTML标签元素名称
     */
    private String listMainElementName;
    /**
     * 目录链接元素CssSelector
     */
    private String chapterListUrlElement;
    /**
     * 目录标题元素CssSelector
     */
    private String chapterListTitleElement;
    /**
     * 小说章节内容URL
     */
    private String chapterContentUrl;
    /**
     * 小说内容JSONPath规则
     */
    private String chapterContentUrlDataRule;
    /**
     * 章节内容处理规则
     */
    private String chapterContentHandleRule;
    /**
     * 是否使用原网页的css样式
     */
    private Boolean isContentOriginalStyle;
    /**
    /**
     * 原网页的css样式字符替换正则
     */
    private String replaceContentOriginalRegex;
    /**
    /**
     * 小说内容的HTML标签元素名称
     */
    private String chapterContentElementName;
    /**
     * 小说内容的正则表达式，用@分隔，前面是正则表达式，后面是替换的内容
     */
    private String chapterContentRegex;
    /**
     * 小说id字段名称
     */
    private String bookIdField;
    /**
     * 小说名称字段名称
     */
    private String bookNameField;
    /**
     * 小说URL字段名称
     */
    private String bookUrlField;
    /**
     * 小说作者字段名称
     */
    private String bookAuthorField;
    /**
     * 小说描述字段名称
     */
    private String bookDescField;
    /**
     * 小说图片URL字段名称
     */
    private String bookImgUrlField;
    /**
     * 是否html页面
     */
    private boolean isHtml;

    public Boolean getEnabled() {
        return isEnabled;
    }

    public void setEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSearchDataBookListRule() {
        return searchDataBookListRule;
    }

    public void setSearchDataBookListRule(String searchDataBookListRule) {
        this.searchDataBookListRule = searchDataBookListRule;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getBookListElementName() {
        return bookListElementName;
    }

    public void setBookListElementName(String bookListElementName) {
        this.bookListElementName = bookListElementName;
    }

    public String getBookListUrlElement() {
        return bookListUrlElement;
    }

    public void setBookListUrlElement(String bookListUrlElement) {
        this.bookListUrlElement = bookListUrlElement;
    }

    public String getBookListTitleElement() {
        return bookListTitleElement;
    }

    public void setBookListTitleElement(String bookListTitleElement) {
        this.bookListTitleElement = bookListTitleElement;
    }

    public String getListMainUrl() {
        return listMainUrl;
    }

    public void setListMainUrl(String listMainUrl) {
        this.listMainUrl = listMainUrl;
    }

    public String getListMainUrlDataRule() {
        return listMainUrlDataRule;
    }

    public void setListMainUrlDataRule(String listMainUrlDataRule) {
        this.listMainUrlDataRule = listMainUrlDataRule;
    }

    public String getChapterContentUrlDataRule() {
        return chapterContentUrlDataRule;
    }

    public void setChapterContentUrlDataRule(String chapterContentUrlDataRule) {
        this.chapterContentUrlDataRule = chapterContentUrlDataRule;
    }

    public String getChapterContentHandleRule() {
        return chapterContentHandleRule;
    }

    public void setChapterContentHandleRule(String chapterContentHandleRule) {
        this.chapterContentHandleRule = chapterContentHandleRule;
    }

    public Boolean getContentOriginalStyle() {
        return isContentOriginalStyle;
    }

    /**
     * 是否使用原网页的css样式
     * @return
     */
    public Boolean isContentOriginalStyle() {
        return isContentOriginalStyle;
    }

    public void setContentOriginalStyle(Boolean contentOriginalStyle) {
        isContentOriginalStyle = contentOriginalStyle;
    }

    public String getReplaceContentOriginalRegex() {
        return replaceContentOriginalRegex;
    }

    public void setReplaceContentOriginalRegex(String replaceContentOriginalRegex) {
        this.replaceContentOriginalRegex = replaceContentOriginalRegex;
    }

    public String getListMainElementName() {
        return listMainElementName;
    }

    public void setListMainElementName(String listMainElementName) {
        this.listMainElementName = listMainElementName;
    }

    public String getChapterListUrlElement() {
        return chapterListUrlElement;
    }

    public void setChapterListUrlElement(String chapterListUrlElement) {
        this.chapterListUrlElement = chapterListUrlElement;
    }

    public String getChapterListTitleElement() {
        return chapterListTitleElement;
    }

    public void setChapterListTitleElement(String chapterListTitleElement) {
        this.chapterListTitleElement = chapterListTitleElement;
    }

    public String getListMainItemIdField() {
        return listMainItemIdField;
    }

    public void setListMainItemIdField(String listMainItemIdField) {
        this.listMainItemIdField = listMainItemIdField;
    }

    public String getListMainItemTitleField() {
        return listMainItemTitleField;
    }

    public void setListMainItemTitleField(String listMainItemTitleField) {
        this.listMainItemTitleField = listMainItemTitleField;
    }


    public String getChapterContentUrl() {
        return chapterContentUrl;
    }

    public void setChapterContentUrl(String chapterContentUrl) {
        this.chapterContentUrl = chapterContentUrl;
    }

    public String getChapterContentElementName() {
        return chapterContentElementName;
    }

    public void setChapterContentElementName(String chapterContentElementName) {
        this.chapterContentElementName = chapterContentElementName;
    }

    public String getChapterContentRegex() {
        return chapterContentRegex;
    }

    public void setChapterContentRegex(String chapterContentRegex) {
        this.chapterContentRegex = chapterContentRegex;
    }

    public String getBookIdField() {
        return bookIdField;
    }

    public void setBookIdField(String bookIdField) {
        this.bookIdField = bookIdField;
    }

    public String getBookNameField() {
        return bookNameField;
    }

    public void setBookNameField(String bookNameField) {
        this.bookNameField = bookNameField;
    }

    public String getBookUrlField() {
        return bookUrlField;
    }

    public void setBookUrlField(String bookUrlField) {
        this.bookUrlField = bookUrlField;
    }

    public String getBookAuthorField() {
        return bookAuthorField;
    }

    public void setBookAuthorField(String bookAuthorField) {
        this.bookAuthorField = bookAuthorField;
    }

    public String getBookDescField() {
        return bookDescField;
    }

    public void setBookDescField(String bookDescField) {
        this.bookDescField = bookDescField;
    }

    public String getBookImgUrlField() {
        return bookImgUrlField;
    }

    public void setBookImgUrlField(String bookImgUrlField) {
        this.bookImgUrlField = bookImgUrlField;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setHtml(boolean html) {
        isHtml = html;
    }

    @Override
    public String toString() {
        return "BookSiteInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", searchUrl='" + searchUrl + '\'' +
                ", header='" + header + '\'' +
                ", searchDataBookListRule='" + searchDataBookListRule + '\'' +
                ", bookListElementName='" + bookListElementName + '\'' +
                ", bookListUrlElement='" + bookListUrlElement + '\'' +
                ", bookListTitleElement='" + bookListTitleElement + '\'' +
                ", listMainUrl='" + listMainUrl + '\'' +
                ", listMainUrlDataRule='" + listMainUrlDataRule + '\'' +
                ", listMainItemIdField='" + listMainItemIdField + '\'' +
                ", listMainItemTitleField='" + listMainItemTitleField + '\'' +
                ", listMainElementName='" + listMainElementName + '\'' +
                ", chapterListUrlElement='" + chapterListUrlElement + '\'' +
                ", chapterListTitleElement='" + chapterListTitleElement + '\'' +
                ", chapterContentUrl='" + chapterContentUrl + '\'' +
                ", chapterContentUrlDataRule='" + chapterContentUrlDataRule + '\'' +
                ", chapterContentHandleRule='" + chapterContentHandleRule + '\'' +
                ", isContentOriginalStyle=" + isContentOriginalStyle +
                ", replaceContentOriginalRegex='" + replaceContentOriginalRegex + '\'' +
                ", chapterContentElementName='" + chapterContentElementName + '\'' +
                ", chapterContentRegex='" + chapterContentRegex + '\'' +
                ", bookIdField='" + bookIdField + '\'' +
                ", bookNameField='" + bookNameField + '\'' +
                ", bookUrlField='" + bookUrlField + '\'' +
                ", bookAuthorField='" + bookAuthorField + '\'' +
                ", bookDescField='" + bookDescField + '\'' +
                ", bookImgUrlField='" + bookImgUrlField + '\'' +
                ", isHtml=" + isHtml +
                '}';
    }
}
