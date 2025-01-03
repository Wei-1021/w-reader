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
     * 搜索小说的url
     */
    private String searchUrl;
    /**
     * 搜索小说的名称参数
     */
    private String searchBookNameParam;
    /**
     * 获取小说列表的JSONPath规则
     */
    private String searchDataBookListRule;
    /**
     * 获取小说列表每一列中书本信息的JSONPath规则（因为有些会将书本信息写入跟深一层的结构中）
     */
    private String searchDataBookListInfoDataRule;
    /**
     * 小说id域
     */
    private String bookDataId;
    /**
     * 小说列表的HTML标签元素名称
     */
    private String bookListElementName;
    /**
     * 小说列表的HTML标签类型（class, id）
     */
    private String bookListElementType;
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
     * 小说目录列表的HTML标签类型（class, id）
     */
    private String listMainElementType;
    /**
     * 小说章节内容URL
     */
    private String chapterContentUrl;
    /**
     * 小说内容JSONPath规则
     */
    private String chapterContentUrlDataRule;
    /**
    /**
     * 小说内容的HTML标签元素名称
     */
    private String chapterContentElementName;
    /**
     * 小说内容的HTML标签类型（class, id）
     */
    private String chapterContentElementType;
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
    /**
     * 是否路径参数
     */
    private boolean isPathParam;

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

    public String getSearchBookNameParam() {
        return searchBookNameParam;
    }

    public void setSearchBookNameParam(String searchBookNameParam) {
        this.searchBookNameParam = searchBookNameParam;
    }

    public String getSearchDataBookListRule() {
        return searchDataBookListRule;
    }

    public void setSearchDataBookListRule(String searchDataBookListRule) {
        this.searchDataBookListRule = searchDataBookListRule;
    }

    public String getSearchDataBookListInfoDataRule() {
        return searchDataBookListInfoDataRule;
    }

    public void setSearchDataBookListInfoDataRule(String searchDataBookListInfoDataRule) {
        this.searchDataBookListInfoDataRule = searchDataBookListInfoDataRule;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getBookDataId() {
        return bookDataId;
    }

    public void setBookDataId(String bookDataId) {
        this.bookDataId = bookDataId;
    }

    public String getBookListElementName() {
        return bookListElementName;
    }

    public void setBookListElementName(String bookListElementName) {
        this.bookListElementName = bookListElementName;
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

    public String getListMainElementName() {
        return listMainElementName;
    }

    public void setListMainElementName(String listMainElementName) {
        this.listMainElementName = listMainElementName;
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

    public String getListMainElementType() {
        return listMainElementType;
    }

    public void setListMainElementType(String listMainElementType) {
        this.listMainElementType = listMainElementType;
    }

    public String getBookListElementType() {
        return bookListElementType;
    }

    public void setBookListElementType(String bookListElementType) {
        this.bookListElementType = bookListElementType;
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

    public String getChapterContentElementType() {
        return chapterContentElementType;
    }

    public void setChapterContentElementType(String chapterContentElementType) {
        this.chapterContentElementType = chapterContentElementType;
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

    public boolean isPathParam() {
        return isPathParam;
    }

    public void setPathParam(boolean pathParam) {
        isPathParam = pathParam;
    }

    @Override
    public String toString() {
        return "BookSiteInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", searchUrl='" + searchUrl + '\'' +
                ", searchBookNameParam='" + searchBookNameParam + '\'' +
                ", searchDataBookListRule='" + searchDataBookListRule + '\'' +
                ", searchDataBookListInfoDataRule='" + searchDataBookListInfoDataRule + '\'' +
                ", bookDataId='" + bookDataId + '\'' +
                ", bookListElementName='" + bookListElementName + '\'' +
                ", bookListElementType='" + bookListElementType + '\'' +
                ", listMainUrl='" + listMainUrl + '\'' +
                ", listMainUrlDataRule='" + listMainUrlDataRule + '\'' +
                ", listMainItemIdField='" + listMainItemIdField + '\'' +
                ", listMainItemTitleField='" + listMainItemTitleField + '\'' +
                ", listMainElementName='" + listMainElementName + '\'' +
                ", listMainElementType='" + listMainElementType + '\'' +
                ", chapterContentUrl='" + chapterContentUrl + '\'' +
                ", chapterContentUrlDataRule='" + chapterContentUrlDataRule + '\'' +
                ", chapterContentElementName='" + chapterContentElementName + '\'' +
                ", chapterContentElementType='" + chapterContentElementType + '\'' +
                ", bookIdField='" + bookIdField + '\'' +
                ", bookNameField='" + bookNameField + '\'' +
                ", bookUrlField='" + bookUrlField + '\'' +
                ", bookAuthorField='" + bookAuthorField + '\'' +
                ", bookDescField='" + bookDescField + '\'' +
                ", bookImgUrlField='" + bookImgUrlField + '\'' +
                ", isHtml=" + isHtml +
                ", isPathParam=" + isPathParam +
                '}';
    }
}
