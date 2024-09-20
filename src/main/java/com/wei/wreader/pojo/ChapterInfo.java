package com.wei.wreader.pojo;

import java.io.Serializable;
import java.util.List;

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
    /**
     * 最后阅读的行数
     */
    private int lastReadLineNum;
    private List<String> chapterContentList;

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

    public int getLastReadLineNum() {
        return lastReadLineNum;
    }

    public void setLastReadLineNum(int lastReadLineNum) {
        this.lastReadLineNum = lastReadLineNum;
    }

    public List<String> getChapterContentList() {
        return chapterContentList;
    }

    public void setChapterContentList(List<String> chapterContentList) {
        this.chapterContentList = chapterContentList;
    }
}
