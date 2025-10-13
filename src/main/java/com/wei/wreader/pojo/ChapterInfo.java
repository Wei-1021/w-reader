package com.wei.wreader.pojo;

import java.io.Serializable;
import java.util.List;

/**
 * 小说章节信息
 *
 * @author weizhanjie
 */
public class ChapterInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String chapterUrl;
    /**
     * 章节标题
     */
    private String chapterTitle;
    /**
     * 章节内容
     */
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
    /**
     * 上一行阅读的行数
     */
    private int prevReadLineNum;
    /**
     * 下一行阅读的行数
     */
    private int nextReadLineNum;
    /**
     * 将章节内容按指定字符长度分割为集合
     */
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

    public int getPrevReadLineNum() {
        return prevReadLineNum;
    }

    public void setPrevReadLineNum(int prevReadLineNum) {
        this.prevReadLineNum = prevReadLineNum;
    }

    public int getNextReadLineNum() {
        return nextReadLineNum;
    }

    public void setNextReadLineNum(int nextReadLineNum) {
        this.nextReadLineNum = nextReadLineNum;
    }

    public List<String> getChapterContentList() {
        return chapterContentList;
    }

    public void setChapterContentList(List<String> chapterContentList) {
        this.chapterContentList = chapterContentList;
    }

    /**
     * 初始化章节信息
     *
     * @return
     */
    public static ChapterInfo initEmptyChapterInfo() {
        ChapterInfo chapterInfoTemp = new ChapterInfo();
        chapterInfoTemp.setChapterTitle("");
        chapterInfoTemp.setChapterContent("");
        chapterInfoTemp.setSelectedChapterIndex(0);
        chapterInfoTemp.setChapterContentStr("");
        chapterInfoTemp.setLastReadLineNum(1);
        chapterInfoTemp.setPrevReadLineNum(1);
        chapterInfoTemp.setNextReadLineNum(2);
        chapterInfoTemp.setChapterContentList(null);
        return chapterInfoTemp;
    }

    /**
     * 初始化章节信息
     *
     * @param chapterContentHtml 原始章节内容
     * @param chapterContentText 提取成纯文字的章节内容
     * @param chapterIndex       章节索引
     * @return
     */
    public void initChapterInfo(String chapterContentHtml,
                                String chapterContentText,
                                int chapterIndex) {
        this.setChapterContent(chapterContentHtml);
        this.setChapterContentStr(chapterContentText);
        this.setSelectedChapterIndex(chapterIndex);
        this.setPrevReadLineNum(1);
        this.setNextReadLineNum(2);
        this.setLastReadLineNum(1);
        this.setChapterContentList(null);
    }

    /**
     * 初始化章节行数信息
     * @param prevReadLineNum
     * @param nextReadLineNum
     * @param lastReadLineNum
     */
    public void initLineNum(int prevReadLineNum, int nextReadLineNum, int lastReadLineNum) {
        this.setPrevReadLineNum(prevReadLineNum);
        this.setNextReadLineNum(nextReadLineNum);
        this.setLastReadLineNum(lastReadLineNum);
        this.setChapterContentList(null);
    }
}
