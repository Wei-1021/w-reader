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
    /**
     * 章节内容字符串（剔除html标签）
     */
    private String chapterContentStr;
    /**
     * 选择的章节下标
     */
    private int selectedChapterIndex;

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

    public int getSelectedChapterIndex() {
        return selectedChapterIndex;
    }

    public void setSelectedChapterIndex(int selectedChapterIndex) {
        this.selectedChapterIndex = selectedChapterIndex;
    }

    public String getChapterContentStr() {
        return chapterContentStr;
    }

    public void setChapterContentStr(String chapterContentStr) {
        this.chapterContentStr = chapterContentStr;
    }
}
