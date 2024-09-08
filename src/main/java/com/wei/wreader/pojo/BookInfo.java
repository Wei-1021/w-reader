package com.wei.wreader.pojo;

import java.io.Serializable;

/**
 * 小说基本信息实体
 * @author weizhanjie
 */
public class BookInfo implements Serializable {
    private static final long serialVersionUID = 8952225148502135L;
    private String bookName;
    private String bookUrl;
    private String bookAuthor;
    private String bookDesc;
    private String bookImgUrl;

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getBookDesc() {
        return bookDesc;
    }

    public void setBookDesc(String bookDesc) {
        this.bookDesc = bookDesc;
    }

    public String getBookImgUrl() {
        return bookImgUrl;
    }

    public void setBookImgUrl(String bookImgUrl) {
        this.bookImgUrl = bookImgUrl;
    }

    @Override
    public String toString() {
        return "BookInfo{" +
                "bookName='" + bookName + '\'' +
                ", bookUrl='" + bookUrl + '\'' +
                ", bookAuthor='" + bookAuthor + '\'' +
                ", bookDesc='" + bookDesc + '\'' +
                ", bookImgUrl='" + bookImgUrl + '\'' +
                '}';
    }
}
