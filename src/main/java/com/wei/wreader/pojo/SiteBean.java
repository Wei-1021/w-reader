package com.wei.wreader.pojo;

/**
 * 站点信息
 * @author weizhanjie
 */
public class SiteBean {
    /** 是否启用 */
    private boolean enabled;
    /** 站点id */
    private String id;
    /** 站点名称 */
    private String name;
    /** 站点地址 */
    private String baseUrl;
    /** 请求头 */
    private String header;
    /** 搜索规则 */
    private SearchRules searchRules;
    /** 目录规则 */
    private ListMainRules listMainRules;
    /** 章节规则 */
    private ChapterRules chapterRules;
    /** 书籍信息规则 */
    private BookInfoRules bookInfoRules;
    /** 是否为html */
    private boolean hasHtml;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public SearchRules getSearchRules() {
        return searchRules;
    }

    public void setSearchRules(SearchRules searchRules) {
        this.searchRules = searchRules;
    }

    public ListMainRules getListMainRules() {
        return listMainRules;
    }

    public void setListMainRules(ListMainRules listMainRules) {
        this.listMainRules = listMainRules;
    }

    public ChapterRules getChapterRules() {
        return chapterRules;
    }

    public void setChapterRules(ChapterRules chapterRules) {
        this.chapterRules = chapterRules;
    }

    public BookInfoRules getBookInfoRules() {
        return bookInfoRules;
    }

    public void setBookInfoRules(BookInfoRules bookInfoRules) {
        this.bookInfoRules = bookInfoRules;
    }

    public boolean isHasHtml() {
        return hasHtml;
    }

    public void setHasHtml(boolean hasHtml) {
        this.hasHtml = hasHtml;
    }

    @Override
    public String toString() {
        return "SiteBean{" +
                "enabled=" + enabled +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", header='" + header + '\'' +
                ", searchRules=" + searchRules +
                ", listMainRules=" + listMainRules +
                ", chapterRules=" + chapterRules +
                ", bookInfoRules=" + bookInfoRules +
                ", hasHtml=" + hasHtml +
                '}';
    }
}
