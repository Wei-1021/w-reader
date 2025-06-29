package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.BookSiteInfo;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 缓存信息数据持久化服务
 *
 * @author weizhanjie
 */
@Service(Service.Level.APP)
@State(name = "SelectInfoService", storages = {@Storage("w-reader-cache.xml")})
public final class CacheService implements PersistentStateComponent<CacheService> {
    /**
     * 设置信息
     */
    private Settings settings;
    /**
     * 字体
     */
    private String fontFamily;
    /**
     * 字体大小
     */
    private int fontSize;
    /**
     * 字体颜色
     */
    private String fontColorHex;
    /**
     * 小说信息
     */
    private BookInfo selectedBookInfo;
    /**
     * 编辑器消息垂直滚动条位置
     */
    private int editorMessageVerticalScrollValue;
    /**
     * 是否隐藏文字
     */
    private boolean isHideText;
    /**
     * 选中的站点信息下标
     */
    private Integer selectedBookSiteIndex;
    /**
     * 选中的站点信息
     */
    private BookSiteInfo selectedBookSiteInfo;
    /**
     * 临时选中的站点信息
     */
    private BookSiteInfo tempSelectedBookSiteInfo;
    /**
     * 选中的章节信息
     */
    private ChapterInfo selectedChapterInfo;
    /**
     * 章节名称列表
     */
    private List<String> chapterList;
    /**
     * 章节链接列表
     */
    private List<String> chapterUrlList;
    /**
     * 章节内容列表（本地加载时使用）
     */
    private List<String> chapterContentList;

    private static CacheService instance;

    public static CacheService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(CacheService.class);
        }
        return instance;
    }

    @Nullable
    @Override
    public CacheService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CacheService state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public BookInfo getSelectedBookInfo() {
        return selectedBookInfo;
    }

    public void setSelectedBookInfo(BookInfo bookInfo) {
        this.selectedBookInfo = bookInfo;
    }

    public Integer getSelectedBookSiteIndex() {
        return selectedBookSiteIndex;
    }

    public void setSelectedBookSiteIndex(Integer selectedBookSiteIndex) {
        this.selectedBookSiteIndex = selectedBookSiteIndex;
    }

    public BookSiteInfo getSelectedBookSiteInfo() {
        return selectedBookSiteInfo;
    }

    public void setSelectedBookSiteInfo(BookSiteInfo selectedBookSiteInfo) {
        this.selectedBookSiteInfo = selectedBookSiteInfo;
    }

    public BookSiteInfo getTempSelectedBookSiteInfo() {
        return tempSelectedBookSiteInfo;
    }

    public void setTempSelectedBookSiteInfo(BookSiteInfo tempSelectedBookSiteInfo) {
        this.tempSelectedBookSiteInfo = tempSelectedBookSiteInfo;
    }

    public ChapterInfo getSelectedChapterInfo() {
        return selectedChapterInfo;
    }

    public void setSelectedChapterInfo(ChapterInfo selectedChapterInfo) {
        this.selectedChapterInfo = selectedChapterInfo;
    }

    public List<String> getChapterUrlList() {
        return chapterUrlList;
    }

    public void setChapterUrlList(List<String> chapterUrlList) {
        this.chapterUrlList = chapterUrlList;
    }

    public List<String> getChapterList() {
        return chapterList;
    }

    public void setChapterList(List<String> chapterList) {
        this.chapterList = chapterList;
    }

    public List<String> getChapterContentList() {
        return chapterContentList;
    }

    public void setChapterContentList(List<String> chapterContentList) {
        this.chapterContentList = chapterContentList;
    }

    public String getFontColorHex() {
        return fontColorHex;
    }

    public void setFontColorHex(String fontColorHex) {
        this.fontColorHex = fontColorHex;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public boolean isHideText() {
        return this.isHideText;
    }

    public void setHideText(boolean hideText) {
        this.isHideText = hideText;
    }

    public int getEditorMessageVerticalScrollValue() {
        return editorMessageVerticalScrollValue;
    }

    public void setEditorMessageVerticalScrollValue(int editorMessageVerticalScrollValue) {
        this.editorMessageVerticalScrollValue = editorMessageVerticalScrollValue;
    }
}
