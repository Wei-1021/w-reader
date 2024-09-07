package com.wei.wreader.pojo;

import java.io.Serializable;

/**
 * 小说章节信息
 * @author weizhanjie
 */
public class ChapterInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String chapterUrl;
    private String chapterTitle;
    private String chapterContent;

    public String getChapterUrl() {
        return chapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getChapterContent() {
        return chapterContent;
    }

    public void setChapterContent(String chapterContent) {
        this.chapterContent = chapterContent;
    }
}
