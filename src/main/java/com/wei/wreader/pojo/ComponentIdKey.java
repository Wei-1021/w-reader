package com.wei.wreader.pojo;

/**
 * 组件id键值
 * @author weizhanjie
 */
public class ComponentIdKey {
    /**
     * 搜索
     */
    private String searchBook;
    /**
     * 目录
     */
    private String bookDirectory;
    /**
     * 上一章
     */
    private String prevChapter;
    /**
     * 下一章
     */
    private String nextChapter;
    /**
     * 上一行
     */
    private String prevLine;
    /**
     * 下一行
     */
    private String nextLine;
    /**
     * 设置
     */
    private String setting;

    public String getSearchBook() {
        return searchBook;
    }

    public void setSearchBook(String searchBook) {
        this.searchBook = searchBook;
    }

    public String getBookDirectory() {
        return bookDirectory;
    }

    public void setBookDirectory(String bookDirectory) {
        this.bookDirectory = bookDirectory;
    }

    public String getPrevChapter() {
        return prevChapter;
    }

    public void setPrevChapter(String prevChapter) {
        this.prevChapter = prevChapter;
    }

    public String getNextChapter() {
        return nextChapter;
    }

    public void setNextChapter(String nextChapter) {
        this.nextChapter = nextChapter;
    }

    public String getPrevLine() {
        return prevLine;
    }

    public void setPrevLine(String prevLine) {
        this.prevLine = prevLine;
    }

    public String getNextLine() {
        return nextLine;
    }

    public void setNextLine(String nextLine) {
        this.nextLine = nextLine;
    }

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }
}
