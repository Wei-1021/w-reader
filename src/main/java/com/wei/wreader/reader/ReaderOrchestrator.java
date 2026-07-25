package com.wei.wreader.reader;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.content.ContentFormatter;
import com.wei.wreader.content.ContentParser;
import com.wei.wreader.content.HtmlContentRenderer;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.model.*;
import com.wei.wreader.service.AppConfigService;
import com.wei.wreader.service.AppStateService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.tts.TtsService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.ListUtil;
import com.wei.wreader.util.file.FileUtil;
import com.wei.wreader.util.ui.ToolWindowUtil;
import com.wei.wreader.widget.ReaderStatusBarWidget;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 阅读器协调器 - 协调所有阅读器操作
 * 替代原来的OperateActionRefactored类
 */
@Service(Service.Level.PROJECT)
public final class ReaderOrchestrator {
    private static final Logger LOG = Logger.getInstance(ReaderOrchestrator.class);

    private final Project project;
    private final CacheService cacheService;
    private final AppConfigService appConfig;
    private final SiteRuleService siteRuleService;
    private final AppStateService appState;
    private final CustomSiteUtil customSiteUtil;
    private final TtsService ttsService;
    private final ChapterNavigator chapterNavigator;
    private final AutoReadController autoReadController;
    private final AutoScrollController autoScrollController;
    private final FontManager fontManager;

    public static ReaderOrchestrator getInstance(Project project) {
        return project.getService(ReaderOrchestrator.class);
    }

    public ReaderOrchestrator(Project project) {
        this.project = project;
        this.cacheService = CacheService.getInstance();
        this.appConfig = AppConfigService.getInstance();
        this.siteRuleService = SiteRuleService.getInstance();
        this.appState = AppStateService.getInstance();
        this.customSiteUtil = CustomSiteUtil.getInstance(project);
        this.ttsService = new TtsService(project);
        this.chapterNavigator = new ChapterNavigator(project, cacheService, appConfig, siteRuleService, appState, customSiteUtil);
        this.autoReadController = AutoReadController.getInstance();
        this.autoReadController.init(project, cacheService, appState);
        this.autoScrollController = AutoScrollController.getInstance();
        this.autoScrollController.init(project, cacheService);
        this.fontManager = new FontManager(cacheService, appConfig);

        initialize();
    }

    /**
     * 初始化阅读器状态
     */
    private void initialize() {
        try {
            // 初始化设置
            Settings settings = cacheService.getSettings();
            if (settings == null) {
                settings = appConfig.getSettings();
                cacheService.setSettings(settings);
            }
            if (StringUtils.isBlank(settings.getCharset())) {
                settings.setCharset(appConfig.getSettings().getCharset());
            }

            // 初始化字体
            fontManager.initializeFontSettings();

            // 初始化站点信息
            initializeSiteInfo();

            // 初始化缓存数据
            initializeCachedData();

        } catch (Exception e) {
            LOG.error("Failed to initialize ReaderOrchestrator", e);
        }
    }

    /**
     * 初始化站点信息
     */
    private void initializeSiteInfo() {
        String selectedCustomSiteRuleKey = siteRuleService.getSelectedCustomSiteRuleKey();

        List<SiteBean> siteBeanList;
        if (StringUtils.isBlank(selectedCustomSiteRuleKey)) {
            siteBeanList = FileUtil.readResourcesJsonList(CustomSiteUtil.DEFAULT_SITE_RULE_PATH, SiteBean.class);
        } else if(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(selectedCustomSiteRuleKey)) {
            Map<String, List<SiteBean>> customSiteRuleGroupMap = siteRuleService.getCustomSiteRuleGroupMap();
            if (customSiteRuleGroupMap == null || customSiteRuleGroupMap.isEmpty()) {
                siteBeanList = FileUtil.readResourcesJsonList(CustomSiteUtil.DEFAULT_SITE_RULE_PATH, SiteBean.class);
            } else {
                siteBeanList = customSiteRuleGroupMap.get(selectedCustomSiteRuleKey);
            }
        } else {
            Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
            List<SiteBean> customList = siteMap.get(selectedCustomSiteRuleKey);
            siteBeanList = customList != null ?
                    customList :
                    FileUtil.readResourcesJsonList(CustomSiteUtil.DEFAULT_SITE_RULE_PATH, SiteBean.class);
        }
        final List<SiteBean> finalSiteBeanList = siteBeanList;

        Integer selectedIndex = cacheService.getSelectedBookSiteIndex();
        if (selectedIndex == null) {
            selectedIndex = 0;
            cacheService.setSelectedBookSiteIndex(0);
        }

        if (selectedIndex >= siteBeanList.size()) {
            selectedIndex = 0;
            cacheService.setSelectedBookSiteIndex(0);
        }

        final int selectedBookSiteIndex = selectedIndex;
        SiteBean selectedSiteBean = siteBeanList.get(selectedBookSiteIndex);
        cacheService.setSelectedSiteBean(selectedSiteBean);
        cacheService.setSelectedBookInfoRules(selectedSiteBean.getBookInfoRules());
        cacheService.setSelectedChapterRules(selectedSiteBean.getChapterRules());

        appState.updateSiteSelection(state -> {
            state.setSiteBeanList(finalSiteBeanList);
            state.setSelectedSiteIndex(selectedBookSiteIndex);
            state.setSelectedSiteBean(selectedSiteBean);
            state.setSelectedSearchRules(selectedSiteBean.getSearchRules());
            state.setSelectedListMainRules(selectedSiteBean.getListMainRules());
            state.setSelectedChapterRules(selectedSiteBean.getChapterRules());
            state.setSelectedBookInfoRules(selectedSiteBean.getBookInfoRules());
        });
    }

    /**
     * 初始化缓存数据
     */
    private void initializeCachedData() {
        BookInfo bookInfo = cacheService.getSelectedBookInfo();
        if (bookInfo == null) {
            bookInfo = new BookInfo();
        }
        final BookInfo finalBookInfo = bookInfo;

        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
        if (chapterInfo == null) {
            chapterInfo = new ChapterInfo();
        }
        final ChapterInfo finalChapterInfo = chapterInfo;

        List<String> chapterList = cacheService.getChapterList();
        List<String> chapterUrlList = cacheService.getChapterUrlList();

        appState.updateReaderState(state -> {
            state.setCurrentBook(finalBookInfo);
            state.setCurrentChapter(finalChapterInfo);
            state.setCurrentChapterIndex(finalChapterInfo.getSelectedChapterIndex());
            state.setChapterNames(chapterList);
            state.setChapterUrls(chapterUrlList);
        });
    }

    // --- 章节导航 ---

    public void prevPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        chapterNavigator.prevPageChapter(runnable);
    }

    public void nextPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        chapterNavigator.nextPageChapter(runnable);
    }

    public void showBookDirectory(BookDirectoryListener listener) {
        chapterNavigator.showBookDirectory(listener);
    }

    // --- 内容加载 ---

    public void searchBookContentRemote(String url, Consumer<SearchBookCallParam> callback) {
        chapterNavigator.searchBookContentRemote(url, callback);
    }

    public void loadLocalFile(String regex) {
        chapterNavigator.loadLocalFile(regex);
    }

    // --- TTS ---

    public void ttsChapterContent() {
        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
        if (chapterInfo != null) {
            ttsService.speakChapterContent(chapterInfo.getChapterContentStr());
        }
    }

    public void stopTTS() {
        ttsService.stopTTS();
    }

    // --- 自动阅读 ---

    public void autoReadNextLine() {
        autoReadController.autoReadNextLine();
    }

    public void toggleAutoScroll() {
        autoScrollController.toggleAutoScroll();
    }

    public void stopAutoScroll() {
        autoScrollController.stopAutoScroll();
    }

    public void executorServiceShutdown() {
        autoReadController.shutdown();
        autoScrollController.shutdown();
    }

    // --- 字体管理 ---

    public void fontSizeSub() {
        fontManager.fontSizeSub();
        updateContentText();
    }

    public void fontSizeAdd() {
        fontManager.fontSizeAdd();
        updateContentText();
    }

    public void changeFontColor() {
        fontManager.changeFontColor();
        updateContentText();
    }

    // --- 内容显示 ---

    public void updateContentText() {
        updateContentText("");
    }

    public void updateContentText(String text) {
        try {
            Settings settings = cacheService.getSettings();
            if (settings == null) return;

            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    updateSidebarContent();
                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    ReaderStatusBarWidget.update(project);
                    break;
            }
        } catch (Exception e) {
            LOG.error("Failed to update content text", e);
        }
    }

    private void updateSidebarContent() {
        ToolWindowUtil.updateContentText(project, textPane -> {
            SiteBean siteBean = cacheService.getSelectedSiteBean();
            boolean isContentOriginalStyle = false;
            if (siteBean != null && siteBean.getChapterRules() != null) {
                isContentOriginalStyle = siteBean.getChapterRules().isUseContentOriginalStyle();
            }

            String chapterContent = cacheService.getSelectedChapterInfo().getChapterContent();
            chapterContent = ContentFormatter.removeStyleTags(chapterContent);

            String fontColorHex = fontManager.getFontColorHex();
            String fontFamily = fontManager.getFontFamily();
            int fontSize = fontManager.getFontSize();
            BookInfo bookInfo = cacheService.getSelectedBookInfo();

            String styledContent;
            if (isContentOriginalStyle) {
                String contentOriginalStyle = appState.readFontState(AppStateService.FontState::getFontColorHex);
                styledContent = HtmlContentRenderer.buildOriginalStyleContent(
                        chapterContent, "", fontColorHex, fontSize, bookInfo);
            } else {
                styledContent = HtmlContentRenderer.buildCustomStyleContent(
                        chapterContent, fontColorHex, fontFamily, fontSize);
            }

            textPane.setText(styledContent);
            textPane.setCaretPosition(0);
        });
    }

    // --- Getters ---

    public Project getProject() { return project; }
    public CacheService getCacheService() { return cacheService; }
    public TtsService getTtsService() { return ttsService; }
    public FontManager getFontManager() { return fontManager; }
    public ChapterNavigator getChapterNavigator() { return chapterNavigator; }
    public AutoReadController getAutoReadController() { return autoReadController; }
    public AutoScrollController getAutoScrollController() { return autoScrollController; }

    /**
     * 加载本章节下一页内容
     */
    public void loadThisChapterNextContent(String chapterUrl, Element bodyElement) {
        chapterNavigator.loadThisChapterNextContent(chapterUrl, bodyElement);
    }

    /**
     * 分割章节内容
     */
    public void splitChapterContent() {
        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
        if (chapterInfo == null) return;
        String chapterContentStr = chapterInfo.getChapterContentStr();
        List<String> chapterContentSplitList = chapterInfo.getChapterContentList();
        Settings settings = cacheService.getSettings();
        int singleLineChars = settings.getSingleLineChars();
        if (chapterContentSplitList == null || chapterContentSplitList.isEmpty()) {
            chapterContentSplitList = com.wei.wreader.util.data.StringUtil.splitStringByMaxCharList(chapterContentStr, singleLineChars);
        }
        chapterInfo.setChapterContentList(chapterContentSplitList);
    }

    /**
     * 获取鼠标点击的文档位置
     */
    public int getClickedPosition(javax.swing.JTextPane contentTextPane, java.awt.event.MouseEvent e) {
        java.awt.Point p = e.getPoint();
        return contentTextPane.viewToModel2D(p);
    }

    /**
     * 获取指定位置的HTML标签
     */
    public String getHTMLTagAtPosition(javax.swing.JTextPane textPane, int pos) {
        javax.swing.text.Document doc = textPane.getDocument();
        if (!(doc instanceof javax.swing.text.html.HTMLDocument htmlDoc)) {
            return null;
        }
        javax.swing.text.Element element = htmlDoc.getCharacterElement(pos);
        List<javax.swing.text.Element> elements = new java.util.ArrayList<>();
        javax.swing.text.Element current = element;
        while (current != null && current.getName() != null) {
            elements.add(current);
            current = current.getParentElement();
        }
        java.util.Collections.reverse(elements);
        StringBuilder sb = new StringBuilder();
        for (javax.swing.text.Element e : elements) {
            javax.swing.text.AttributeSet attrs = e.getAttributes();
            javax.swing.text.html.HTML.Tag tag = (javax.swing.text.html.HTML.Tag) attrs.getAttribute(javax.swing.text.StyleConstants.NameAttribute);
            if (tag != null) {
                sb.append("<").append(tag);
                java.util.Enumeration<?> names = attrs.getAttributeNames();
                while (names.hasMoreElements()) {
                    Object name = names.nextElement();
                    if (name instanceof javax.swing.text.html.HTML.Attribute attr) {
                        Object value = attrs.getAttribute(attr);
                        if (value != null) {
                            sb.append(" ").append(attr).append("=\"").append(value).append("\"");
                        }
                    }
                }
                sb.append(">");
            }
        }
        return sb.toString();
    }
}