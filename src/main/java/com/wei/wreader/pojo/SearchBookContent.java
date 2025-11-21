package com.wei.wreader.pojo;

import org.jsoup.nodes.Element;

public class SearchBookContent {
    private String chapterContentHtml;
    private String chapterContentText;
    private Element bodyElement;

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
}
