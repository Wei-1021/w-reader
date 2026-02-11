package com.wei.wreader.pojo;

import org.jsoup.nodes.Element;

import java.util.List;

/**
 * 搜索小说时的回调函数参数通用对象
 *
 * @author weizhanjie
 */
public class SearchBookCallParam {
    private String chapterContentHtml;
    private String chapterContentText;
    private Element bodyElement;
    private String bodyContentStr;
    private List<String> tempChapterList;
    private List<String> tempChapterUrlList;

    public String getChapterContentHtml() {
        return chapterContentHtml;
    }

    public void setChapterContentHtml(String chapterContentHtml) {
        this.chapterContentHtml = chapterContentHtml;
    }

    public String getChapterContentText() {
        return chapterContentText;
    }

    public void setChapterContentText(String chapterContentText) {
        this.chapterContentText = chapterContentText;
    }

    public Element getBodyElement() {
        return bodyElement;
    }

    public void setBodyElement(Element bodyElement) {
        this.bodyElement = bodyElement;
    }

    public String getBodyContentStr() {
        return bodyContentStr;
    }

    public void setBodyContentStr(String bodyContentStr) {
        this.bodyContentStr = bodyContentStr;
    }

    public List<String> getTempChapterList() {
        return tempChapterList;
    }

    public void setTempChapterList(List<String> tempChapterList) {
        this.tempChapterList = tempChapterList;
    }

    public List<String> getTempChapterUrlList() {
        return tempChapterUrlList;
    }

    public void setTempChapterUrlList(List<String> tempChapterUrlList) {
        this.tempChapterUrlList = tempChapterUrlList;
    }
}
