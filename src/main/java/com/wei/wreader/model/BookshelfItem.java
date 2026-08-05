package com.wei.wreader.model;

import java.io.Serializable;
import java.time.Instant;

public class BookshelfItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uniqueKey;
    private String siteGroupKey;
    private String siteId;
    private String bookId;

    private String bookName;
    private String bookAuthor;
    private String bookDesc;
    private String bookImgUrl;
    private String bookUrl;

    private int chapterIndex;
    private String chapterTitle;
    private int scrollBarValue;
    private int lastReadLineNum;
    private int totalChapters;

//    private Instant addedAt;
//    private Instant lastReadAt;
    private Long addedAt;
    private Long lastReadAt;

    private boolean inShelf;
    private int dataLoadType;

    public BookshelfItem() {
    }

    public static String buildUniqueKey(String siteGroupKey, String siteId, String bookId) {
        return siteGroupKey + ":" + siteId + ":" + bookId;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public String getSiteGroupKey() {
        return siteGroupKey;
    }

    public void setSiteGroupKey(String siteGroupKey) {
        this.siteGroupKey = siteGroupKey;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
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

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public int getScrollBarValue() {
        return scrollBarValue;
    }

    public void setScrollBarValue(int scrollBarValue) {
        this.scrollBarValue = scrollBarValue;
    }

    public int getLastReadLineNum() {
        return lastReadLineNum;
    }

    public void setLastReadLineNum(int lastReadLineNum) {
        this.lastReadLineNum = lastReadLineNum;
    }

    public int getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Long addedAt) {
        this.addedAt = addedAt;
    }

    public Long getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Long lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public boolean isInShelf() {
        return inShelf;
    }

    public void setInShelf(boolean inShelf) {
        this.inShelf = inShelf;
    }

    public int getDataLoadType() {
        return dataLoadType;
    }

    public void setDataLoadType(int dataLoadType) {
        this.dataLoadType = dataLoadType;
    }

    public void copyFromBookInfo(BookInfo bookInfo) {
        if (bookInfo == null) return;
        this.bookId = bookInfo.getBookId();
        this.bookName = bookInfo.getBookName();
        this.bookAuthor = bookInfo.getBookAuthor();
        this.bookDesc = bookInfo.getBookDesc();
        this.bookImgUrl = bookInfo.getBookImgUrl();
        this.bookUrl = bookInfo.getBookUrl();
    }

    public BookInfo toBookInfo() {
        BookInfo bookInfo = new BookInfo();
        bookInfo.setBookId(this.bookId);
        bookInfo.setBookName(this.bookName);
        bookInfo.setBookAuthor(this.bookAuthor);
        bookInfo.setBookDesc(this.bookDesc);
        bookInfo.setBookImgUrl(this.bookImgUrl);
        bookInfo.setBookUrl(this.bookUrl);
        return bookInfo;
    }

    @Override
    public String toString() {
        return "BookshelfItem{" +
                "uniqueKey='" + uniqueKey + '\'' +
                ", siteGroupKey='" + siteGroupKey + '\'' +
                ", bookName='" + bookName + '\'' +
                ", bookAuthor='" + bookAuthor + '\'' +
                ", chapterIndex=" + chapterIndex +
                ", chapterTitle='" + chapterTitle + '\'' +
                ", totalChapters=" + totalChapters +
                ", inShelf=" + inShelf +
                ", lastReadAt=" + lastReadAt +
                '}';
    }
}