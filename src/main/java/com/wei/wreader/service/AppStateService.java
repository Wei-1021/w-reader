package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.wei.wreader.model.*;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

@Service(Service.Level.APP)
public final class AppStateService {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReaderState readerState = new ReaderState();
    private final SiteSelectionState siteSelection = new SiteSelectionState();
    private final FontState fontState = new FontState();

    public static AppStateService getInstance() {
        return ApplicationManager.getApplication().getService(AppStateService.class);
    }

    // --- ReaderState operations ---

    public void updateReaderState(Consumer<ReaderState> updater) {
        lock.writeLock().lock();
        try { updater.accept(readerState); }
        finally { lock.writeLock().unlock(); }
    }

    public <T> T readReaderState(Function<ReaderState, T> reader) {
        lock.readLock().lock();
        try { return reader.apply(readerState); }
        finally { lock.readLock().unlock(); }
    }

    // --- SiteSelectionState operations ---

    public void updateSiteSelection(Consumer<SiteSelectionState> updater) {
        lock.writeLock().lock();
        try { updater.accept(siteSelection); }
        finally { lock.writeLock().unlock(); }
    }

    public <T> T readSiteSelection(Function<SiteSelectionState, T> reader) {
        lock.readLock().lock();
        try { return reader.apply(siteSelection); }
        finally { lock.readLock().unlock(); }
    }

    // --- FontState operations ---

    public void updateFontState(Consumer<FontState> updater) {
        lock.writeLock().lock();
        try { updater.accept(fontState); }
        finally { lock.writeLock().unlock(); }
    }

    public <T> T readFontState(Function<FontState, T> reader) {
        lock.readLock().lock();
        try { return reader.apply(fontState); }
        finally { lock.readLock().unlock(); }
    }

    // --- Inner state classes ---

    public static class ReaderState {
        private BookInfo currentBook = new BookInfo();
        private ChapterInfo currentChapter = new ChapterInfo();
        private int currentChapterIndex = 0;
        private List<String> chapterNames;
        private List<String> chapterUrls;
        private List<String> chapterContents;
        private int scrollBarValue = 0;
        private boolean switchNextChapterSuccess = false;
        private int autoReadLastReadLineNum = 0;

        public BookInfo getCurrentBook() { return currentBook; }
        public void setCurrentBook(BookInfo currentBook) { this.currentBook = currentBook; }
        public ChapterInfo getCurrentChapter() { return currentChapter; }
        public void setCurrentChapter(ChapterInfo currentChapter) { this.currentChapter = currentChapter; }
        public int getCurrentChapterIndex() { return currentChapterIndex; }
        public void setCurrentChapterIndex(int currentChapterIndex) { this.currentChapterIndex = currentChapterIndex; }
        public List<String> getChapterNames() { return chapterNames; }
        public void setChapterNames(List<String> chapterNames) { this.chapterNames = chapterNames; }
        public List<String> getChapterUrls() { return chapterUrls; }
        public void setChapterUrls(List<String> chapterUrls) { this.chapterUrls = chapterUrls; }
        public List<String> getChapterContents() { return chapterContents; }
        public void setChapterContents(List<String> chapterContents) { this.chapterContents = chapterContents; }
        public int getScrollBarValue() { return scrollBarValue; }
        public void setScrollBarValue(int scrollBarValue) { this.scrollBarValue = scrollBarValue; }
        public boolean isSwitchNextChapterSuccess() { return switchNextChapterSuccess; }
        public void setSwitchNextChapterSuccess(boolean switchNextChapterSuccess) { this.switchNextChapterSuccess = switchNextChapterSuccess; }
        public int getAutoReadLastReadLineNum() { return autoReadLastReadLineNum; }
        public void setAutoReadLastReadLineNum(int autoReadLastReadLineNum) { this.autoReadLastReadLineNum = autoReadLastReadLineNum; }
    }

    public static class SiteSelectionState {
        private int selectedSiteIndex = 0;
        private SiteBean selectedSiteBean;
        private SiteBean tempSelectedSiteBean;
        private BookInfoRules selectedBookInfoRules;
        private ChapterRules selectedChapterRules;
        private ListMainRules selectedListMainRules;
        private SearchRules selectedSearchRules;
        private String selectSiteGroupName;
        private List<SiteBean> siteBeanList;

        public int getSelectedSiteIndex() { return selectedSiteIndex; }
        public void setSelectedSiteIndex(int selectedSiteIndex) { this.selectedSiteIndex = selectedSiteIndex; }
        public SiteBean getSelectedSiteBean() { return selectedSiteBean; }
        public void setSelectedSiteBean(SiteBean selectedSiteBean) { this.selectedSiteBean = selectedSiteBean; }
        public SiteBean getTempSelectedSiteBean() { return tempSelectedSiteBean; }
        public void setTempSelectedSiteBean(SiteBean tempSelectedSiteBean) { this.tempSelectedSiteBean = tempSelectedSiteBean; }
        public BookInfoRules getSelectedBookInfoRules() { return selectedBookInfoRules; }
        public void setSelectedBookInfoRules(BookInfoRules selectedBookInfoRules) { this.selectedBookInfoRules = selectedBookInfoRules; }
        public ChapterRules getSelectedChapterRules() { return selectedChapterRules; }
        public void setSelectedChapterRules(ChapterRules selectedChapterRules) { this.selectedChapterRules = selectedChapterRules; }
        public ListMainRules getSelectedListMainRules() { return selectedListMainRules; }
        public void setSelectedListMainRules(ListMainRules selectedListMainRules) { this.selectedListMainRules = selectedListMainRules; }
        public SearchRules getSelectedSearchRules() { return selectedSearchRules; }
        public void setSelectedSearchRules(SearchRules selectedSearchRules) { this.selectedSearchRules = selectedSearchRules; }
        public String getSelectSiteGroupName() { return selectSiteGroupName; }
        public void setSelectSiteGroupName(String selectSiteGroupName) { this.selectSiteGroupName = selectSiteGroupName; }
        public List<SiteBean> getSiteBeanList() { return siteBeanList; }
        public void setSiteBeanList(List<SiteBean> siteBeanList) { this.siteBeanList = siteBeanList; }
    }

    public static class FontState {
        private String fontFamily = "Source Han Sans SC Normal";
        private int fontSize = 16;
        private String fontColorHex = "#cccccc";

        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        public String getFontColorHex() { return fontColorHex; }
        public void setFontColorHex(String fontColorHex) { this.fontColorHex = fontColorHex; }
    }
}
