package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.wei.wreader.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service(Service.Level.APP)
@State(name = "SelectInfoService", storages = {@Storage("w-reader-cache.xml")})
public final class CacheService implements PersistentStateComponent<CacheService> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private Settings settings;
    private String fontFamily;
    private int fontSize;
    private String fontColorHex;
    private boolean fontBold = true;
    private BookInfo selectedBookInfo;
    private int editorMessageVerticalScrollValue;
    private boolean isHideText;
    private Integer selectedBookSiteIndex;
    private BookSiteInfo selectedBookSiteInfo;
    private BookSiteInfo tempSelectedBookSiteInfo;
    private ChapterInfo selectedChapterInfo;
    private Integer tempSelectedBookSiteIndex;
    private BookInfo tempSelectedBookInfo;
    private List<String> chapterList;
    private List<String> chapterUrlList;
    private List<String> chapterContentList;
    private SiteBean selectedSiteBean;
    private SiteBean tempSelectedSiteBean;
    private BookInfoRules selectedBookInfoRules;
    private ChapterRules selectedChapterRules;

    public static CacheService getInstance() {
        return ApplicationManager.getApplication().getService(CacheService.class);
    }

    @Override
    public @NotNull CacheService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CacheService state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // --- All getters/setters with read/write lock protection ---

    public Settings getSettings() {
        lock.readLock().lock();
        try { return settings; } finally { lock.readLock().unlock(); }
    }
    public void setSettings(Settings settings) {
        lock.writeLock().lock();
        try { this.settings = settings; } finally { lock.writeLock().unlock(); }
    }

    public BookInfo getSelectedBookInfo() {
        lock.readLock().lock();
        try { return selectedBookInfo; } finally { lock.readLock().unlock(); }
    }
    public void setSelectedBookInfo(BookInfo bookInfo) {
        lock.writeLock().lock();
        try { this.selectedBookInfo = bookInfo; } finally { lock.writeLock().unlock(); }
    }

    public Integer getSelectedBookSiteIndex() {
        lock.readLock().lock();
        try { return selectedBookSiteIndex; } finally { lock.readLock().unlock(); }
    }
    public void setSelectedBookSiteIndex(Integer selectedBookSiteIndex) {
        lock.writeLock().lock();
        try { this.selectedBookSiteIndex = selectedBookSiteIndex; } finally { lock.writeLock().unlock(); }
    }

    public BookSiteInfo getSelectedBookSiteInfo() {
        lock.readLock().lock();
        try { return selectedBookSiteInfo; } finally { lock.readLock().unlock(); }
    }
    public void setSelectedBookSiteInfo(BookSiteInfo selectedBookSiteInfo) {
        lock.writeLock().lock();
        try { this.selectedBookSiteInfo = selectedBookSiteInfo; } finally { lock.writeLock().unlock(); }
    }

    public BookSiteInfo getTempSelectedBookSiteInfo() {
        lock.readLock().lock();
        try { return tempSelectedBookSiteInfo; } finally { lock.readLock().unlock(); }
    }
    public void setTempSelectedBookSiteInfo(BookSiteInfo tempSelectedBookSiteInfo) {
        lock.writeLock().lock();
        try { this.tempSelectedBookSiteInfo = tempSelectedBookSiteInfo; } finally { lock.writeLock().unlock(); }
    }

    public ChapterInfo getSelectedChapterInfo() {
        lock.writeLock().lock();
        try {
            if (selectedChapterInfo == null) { selectedChapterInfo = new ChapterInfo(); }
            return selectedChapterInfo;
        } finally { lock.writeLock().unlock(); }
    }
    public void setSelectedChapterInfo(ChapterInfo selectedChapterInfo) {
        lock.writeLock().lock();
        try { this.selectedChapterInfo = selectedChapterInfo; } finally { lock.writeLock().unlock(); }
    }

    public List<String> getChapterUrlList() {
        lock.readLock().lock();
        try { return chapterUrlList; } finally { lock.readLock().unlock(); }
    }
    public void setChapterUrlList(List<String> chapterUrlList) {
        lock.writeLock().lock();
        try { this.chapterUrlList = chapterUrlList; } finally { lock.writeLock().unlock(); }
    }

    public List<String> getChapterList() {
        lock.readLock().lock();
        try { return chapterList; } finally { lock.readLock().unlock(); }
    }
    public void setChapterList(List<String> chapterList) {
        lock.writeLock().lock();
        try { this.chapterList = chapterList; } finally { lock.writeLock().unlock(); }
    }

    public List<String> getChapterContentList() {
        lock.readLock().lock();
        try { return chapterContentList; } finally { lock.readLock().unlock(); }
    }
    public void setChapterContentList(List<String> chapterContentList) {
        lock.writeLock().lock();
        try { this.chapterContentList = chapterContentList; } finally { lock.writeLock().unlock(); }
    }

    public String getFontColorHex() {
        lock.readLock().lock();
        try { return fontColorHex; } finally { lock.readLock().unlock(); }
    }
    public void setFontColorHex(String fontColorHex) {
        lock.writeLock().lock();
        try { this.fontColorHex = fontColorHex; } finally { lock.writeLock().unlock(); }
    }

    public boolean isFontBold() {
        lock.readLock().lock();
        try { return fontBold; } finally { lock.readLock().unlock(); }
    }
    public void setFontBold(boolean fontBold) {
        lock.writeLock().lock();
        try { this.fontBold = fontBold; } finally { lock.writeLock().unlock(); }
    }

    public int getFontSize() {
        lock.readLock().lock();
        try { return fontSize; } finally { lock.readLock().unlock(); }
    }
    public void setFontSize(int fontSize) {
        lock.writeLock().lock();
        try { this.fontSize = fontSize; } finally { lock.writeLock().unlock(); }
    }

    public String getFontFamily() {
        lock.readLock().lock();
        try { return fontFamily; } finally { lock.readLock().unlock(); }
    }
    public void setFontFamily(String fontFamily) {
        lock.writeLock().lock();
        try { this.fontFamily = fontFamily; } finally { lock.writeLock().unlock(); }
    }

    public boolean isHideText() {
        lock.readLock().lock();
        try { return this.isHideText; } finally { lock.readLock().unlock(); }
    }
    public void setHideText(boolean hideText) {
        lock.writeLock().lock();
        try { this.isHideText = hideText; } finally { lock.writeLock().unlock(); }
    }

    public int getEditorMessageVerticalScrollValue() {
        lock.readLock().lock();
        try { return editorMessageVerticalScrollValue; } finally { lock.readLock().unlock(); }
    }
    public void setEditorMessageVerticalScrollValue(int editorMessageVerticalScrollValue) {
        lock.writeLock().lock();
        try { this.editorMessageVerticalScrollValue = editorMessageVerticalScrollValue; } finally { lock.writeLock().unlock(); }
    }

    public SiteBean getSelectedSiteBean() {
        lock.readLock().lock();
        try { return selectedSiteBean; } finally { lock.readLock().unlock(); }
    }
    public void setSelectedSiteBean(SiteBean selectedSiteBean) {
        lock.writeLock().lock();
        try { this.selectedSiteBean = selectedSiteBean; } finally { lock.writeLock().unlock(); }
    }

    public SiteBean getTempSelectedSiteBean() {
        lock.readLock().lock();
        try { return tempSelectedSiteBean; } finally { lock.readLock().unlock(); }
    }
    public void setTempSelectedSiteBean(SiteBean tempSelectedSiteBean) {
        lock.writeLock().lock();
        try { this.tempSelectedSiteBean = tempSelectedSiteBean; } finally { lock.writeLock().unlock(); }
    }

    public BookInfoRules getSelectedBookInfoRules() {
        lock.readLock().lock();
        try { return selectedBookInfoRules; } finally { lock.readLock().unlock(); }
    }
    public void setSelectedBookInfoRules(BookInfoRules selectedBookInfoRules) {
        lock.writeLock().lock();
        try { this.selectedBookInfoRules = selectedBookInfoRules; } finally { lock.writeLock().unlock(); }
    }

    public ChapterRules getSelectedChapterRules() {
        lock.readLock().lock();
        try { return selectedChapterRules; } finally { lock.readLock().unlock(); }
    }
    public void setSelectedChapterRules(ChapterRules selectedChapterRules) {
        lock.writeLock().lock();
        try { this.selectedChapterRules = selectedChapterRules; } finally { lock.writeLock().unlock(); }
    }

    public Integer getTempSelectedBookSiteIndex() {
        lock.readLock().lock();
        try { return tempSelectedBookSiteIndex; } finally { lock.readLock().unlock(); }
    }
    public void setTempSelectedBookSiteIndex(Integer tempSelectedBookSiteIndex) {
        lock.writeLock().lock();
        try { this.tempSelectedBookSiteIndex = tempSelectedBookSiteIndex; } finally { lock.writeLock().unlock(); }
    }

    public BookInfo getTempSelectedBookInfo() {
        lock.readLock().lock();
        try { return tempSelectedBookInfo; } finally { lock.readLock().unlock(); }
    }
    public void setTempSelectedBookInfo(BookInfo tempSelectedBookInfo) {
        lock.writeLock().lock();
        try { this.tempSelectedBookInfo = tempSelectedBookInfo; } finally { lock.writeLock().unlock(); }
    }
}
