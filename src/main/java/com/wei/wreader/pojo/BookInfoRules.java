package com.wei.wreader.pojo;

/**
 * 书本信息规则
 *
 * @author weizhanjie
 */
public class BookInfoRules {
    /**
     * 书本ID字段
     */
    private String bookIdField;
    /**
     * 书本名称字段
     */
    private String bookNameField;
    /**
     * 书本链接字段
     */
    private String bookUrlField;
    /**
     * 书本作者字段
     */
    private String bookAuthorField;
    /**
     * 书本描述字段
     */
    private String bookDescField;
    /**
     * 书本封面字段
     */
    private String bookImgUrlField;

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

    @Override
    public String toString() {
        return "BookInfoRules{" +
                "bookIdField='" + bookIdField + '\'' +
                ", bookNameField='" + bookNameField + '\'' +
                ", bookUrlField='" + bookUrlField + '\'' +
                ", bookAuthorField='" + bookAuthorField + '\'' +
                ", bookDescField='" + bookDescField + '\'' +
                ", bookImgUrlField='" + bookImgUrlField + '\'' +
                '}';
    }
}
