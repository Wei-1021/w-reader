package com.wei.wreader.pojo;

/**
 * 搜索规则
 * @author weizhanjie
 */
public class SearchRules {
    /**
     * 搜索URL
     */
    private String url;
    /**
     * 获取小说列表的JSONPath规则
     */
    private String dataBookListRule;
    /**
     * 书本列表区块元素CssSelector
     */
    private String bookListElementName;
    /**
     * 书本列表URL元素CssSelector, 规则分段，用@分隔<br>
     *  {@code @back}:表示在获取到的内容的后面加上@back:之后的内容<br>
     *  {@code @front}:表示在获取到的内容的前面加上@front:之后的内容
     */
    private String bookListUrlElement;
    /**
     * 书本标题列表元素CssSelector
     */
    private String bookListTitleElement;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDataBookListRule() {
        return dataBookListRule;
    }

    public void setDataBookListRule(String dataBookListRule) {
        this.dataBookListRule = dataBookListRule;
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

    @Override
    public String toString() {
        return "SearchRules{" +
                "url='" + url + '\'' +
                ", dataBookListRule='" + dataBookListRule + '\'' +
                ", bookListElementName='" + bookListElementName + '\'' +
                ", bookListUrlElement='" + bookListUrlElement + '\'' +
                ", bookListTitleElement='" + bookListTitleElement + '\'' +
                '}';
    }
}
