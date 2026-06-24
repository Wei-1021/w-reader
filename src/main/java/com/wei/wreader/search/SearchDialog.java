package com.wei.wreader.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.content.HtmlContentRenderer;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.model.BookInfo;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.DataLoadType;
import com.wei.wreader.model.SearchBookCallParam;
import com.wei.wreader.model.Settings;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.reader.FontManager;
import com.wei.wreader.reader.ReaderOrchestrator;
import org.jsoup.nodes.Element;
import com.wei.wreader.service.AppConfigService;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.JsonUtil;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.file.FileUtil;
import com.wei.wreader.util.ui.ToolWindowUtil;
import com.wei.wreader.util.yml.ConfigYaml;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索对话框 - 左右分栏布局
 * 左侧：搜索结果（书籍列表）
 * 右侧：选中书籍的章节列表
 */
public class SearchDialog {
    private final Project project;
    private final SearchService searchService;
    private final CacheService cacheService;
    private final AppConfigService appConfig;
    private final SiteRuleService siteRuleService;
    private final CustomSiteUtil customSiteUtil;
    private final ConfigYaml configYaml;
    private final FontManager fontManager;

    private List<SiteBean> siteBeanList;
    private int selectedBookSiteIndex;
    private DialogBuilder dialogBuilder;

    // UI组件 - 顶部
    private ComboBox<String> siteGroupComboBox;
    private ComboBox<String> siteListComboBox;
    private JTextField searchTextField;

    // UI组件 - 左侧（书籍列表）
    private DefaultListModel<String> bookListModel;
    private JBList<String> bookList;
    private List<BookInfo> bookInfoList = new ArrayList<>();

    // UI组件 - 右侧（章节列表）
    private DefaultListModel<String> chapterListModel;
    private JBList<String> chapterList;
    private List<String> chapterUrlList = new ArrayList<>();

    public SearchDialog(Project project) {
        this.project = project;
        this.searchService = new SearchService(project);
        this.cacheService = CacheService.getInstance();
        this.appConfig = AppConfigService.getInstance();
        this.siteRuleService = SiteRuleService.getInstance();
        this.customSiteUtil = CustomSiteUtil.getInstance(project);
        this.configYaml = ConfigYaml.getInstance();
        this.fontManager = new FontManager(cacheService, appConfig);
        fontManager.initializeFontSettings();
        initSiteBeanList();
    }

    /**
     * 初始化站点列表 - 从配置加载所有书源
     */
    private void initSiteBeanList() {
        String selectedRuleKey = siteRuleService.getSelectedCustomSiteRuleKey();

        if (StringUtils.isBlank(selectedRuleKey) ||
                ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(selectedRuleKey)) {
            siteBeanList = FileUtil.readResourcesJsonList(CustomSiteUtil.DEFAULT_SITE_RULE_PATH, SiteBean.class);
        } else {
            Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
            List<SiteBean> customList = siteMap.get(selectedRuleKey);
            siteBeanList = customList != null ? customList : configYaml.getSiteList();
        }

        if (siteBeanList == null) {
            siteBeanList = new ArrayList<>();
        }

        Integer cachedIndex = cacheService.getSelectedBookSiteIndex();
        selectedBookSiteIndex = (cachedIndex != null && cachedIndex >= 0 && cachedIndex < siteBeanList.size()) ? cachedIndex : 0;
    }

    /**
     * 显示搜索对话框
     */
    public void showSearchDialog(BookDirectoryListener listener) {
        SwingUtilities.invokeLater(() -> buildAndShowDialog(listener));
    }

    /**
     * 构建并显示统一搜索界面
     */
    private void buildAndShowDialog(BookDirectoryListener listener) {
        dialogBuilder = new DialogBuilder(project);
        dialogBuilder.setTitle(ConstUtil.WREADER_SEARCH_BOOK_TITLE);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setPreferredSize(JBUI.size(800, 600));
        mainPanel.setBorder(JBUI.Borders.empty(5));

        // ===== 顶部：书源选择区域 =====
        JPanel sourcePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 书源分组下拉框
        siteGroupComboBox = buildSiteGroupComboBox();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        sourcePanel.add(new JLabel("书源分组"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        sourcePanel.add(siteGroupComboBox, gbc);

        // 书源列表下拉框
        siteListComboBox = buildSiteComboBox();
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        sourcePanel.add(new JLabel(ConstUtil.WREADER_SEARCH_BOOK_SITE_TITLE), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        sourcePanel.add(siteListComboBox, gbc);

        // 搜索输入框 + 搜索按钮
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchTextField = new JTextField(20);
        searchTextField.setToolTipText(ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT);
        // 搜索按钮
        JButton searchButton = new JButton("搜索");
        searchButton.addActionListener(e -> executeSearch());
        searchTextField.addActionListener(e -> executeSearch());

        searchPanel.add(searchTextField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        sourcePanel.add(new JLabel(ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        sourcePanel.add(searchPanel, gbc);

        mainPanel.add(sourcePanel, BorderLayout.NORTH);

        // ===== 中部：左右分栏 - 书籍列表 + 章节列表 =====
        JBSplitter splitter = new JBSplitter(false, "w-reader.search.splitter", 0.5f);
        splitter.setFirstComponent(createBookListPanel());
        splitter.setSecondComponent(createChapterListPanel());

        mainPanel.add(splitter, BorderLayout.CENTER);

        // 设置事件监听
        setupBookListListener(listener);
        setupChapterListListener(listener);

        dialogBuilder.setCenterPanel(mainPanel);
        dialogBuilder.setPreferredFocusComponent(searchTextField);
        dialogBuilder.show();
    }

    /**
     * 创建书籍列表面板（左侧）
     */
    private JPanel createBookListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                "搜索结果",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        bookListModel = new DefaultListModel<>();
        bookList = new JBList<>(bookListModel);
        bookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JBScrollPane scrollPane = new JBScrollPane(bookList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建章节列表面板（右侧）
     */
    private JPanel createChapterListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                "章节列表",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        chapterListModel = new DefaultListModel<>();
        chapterList = new JBList<>(chapterListModel);
        chapterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JBScrollPane scrollPane = new JBScrollPane(chapterList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 设置书籍列表选择监听器
     */
    private void setupBookListListener(BookDirectoryListener listener) {
        bookList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int idx = bookList.getSelectedIndex();
            if (idx >= 0 && idx < bookInfoList.size()) {
                handleBookSelection(idx, listener);
            }
        });
    }

    /**
     * 设置章节列表选择监听器
     */
    private void setupChapterListListener(BookDirectoryListener listener) {
        chapterList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int idx = chapterList.getSelectedIndex();
            if (idx < 0 || idx >= chapterListModel.size()) return;
            handleChapterSelection(idx, listener);
        });
    }

    /**
     * 构建书源分组下拉框
     */
    private ComboBox<String> buildSiteGroupComboBox() {
        List<String> groupNames = customSiteUtil.getCustomSiteKeyGroupList();
        String selectedGroupName = siteRuleService.getSelectedCustomSiteRuleKey();

        ComboBox<String> comboBox = new ComboBox<>();
        int selectedIndex = 0;
        for (int i = 0; i < groupNames.size(); i++) {
            comboBox.addItem(groupNames.get(i));
            if (groupNames.get(i).equals(selectedGroupName)) {
                selectedIndex = i;
            }
        }
        comboBox.setSelectedIndex(selectedIndex);

        comboBox.addItemListener(e -> {
            String selected = (String) e.getItem();
            Map<String, List<SiteBean>> groupMap = customSiteUtil.getSiteMap();
            List<SiteBean> list = groupMap.get(selected);
            siteBeanList = (list != null) ? list : new ArrayList<>();
            refreshSiteListComboBox();
            selectedBookSiteIndex = 0;
            if (siteListComboBox.getItemCount() > 0) {
                siteListComboBox.setSelectedIndex(0);
            }
            siteRuleService.setSelectedCustomSiteRuleKey(selected);
        });

        return comboBox;
    }

    /**
     * 构建书源列表下拉框
     */
    private ComboBox<String> buildSiteComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        for (SiteBean site : siteBeanList) {
            comboBox.addItem(site.getName() + "(" + site.getId() + ")");
        }
        if (selectedBookSiteIndex < siteBeanList.size()) {
            comboBox.setSelectedIndex(selectedBookSiteIndex);
        }
        return comboBox;
    }

    /**
     * 刷新书源列表下拉框
     */
    private void refreshSiteListComboBox() {
        siteListComboBox.removeAllItems();
        for (SiteBean site : siteBeanList) {
            siteListComboBox.addItem(site.getName() + "(" + site.getId() + ")");
        }
    }

    /**
     * 执行搜索
     */
    private void executeSearch() {
        String keyword = searchTextField.getText().trim();
        if (StringUtils.isBlank(keyword)) {
            Messages.showWarningDialog("请输入搜索关键词", "提示");
            return;
        }

        int siteIndex = siteListComboBox.getSelectedIndex();
        if (siteIndex < 0 || siteIndex >= siteBeanList.size()) {
            Messages.showWarningDialog("请选择书源", "提示");
            return;
        }

        selectedBookSiteIndex = siteIndex;
        SiteBean selectedSite = siteBeanList.get(siteIndex);

        // 缓存选中的站点信息
        cacheService.setSelectedSiteBean(selectedSite);
        cacheService.setSelectedBookSiteIndex(siteIndex);
        cacheService.setTempSelectedSiteBean(selectedSite);
        cacheService.setTempSelectedBookSiteIndex(siteIndex);

        // 清空结果
        bookListModel.clear();
        bookInfoList.clear();
        chapterListModel.clear();
        chapterUrlList.clear();

        // 执行后台搜索
        String searchUrl = selectedSite.getSearchRules().getUrl();
        if (StringUtils.isBlank(searchUrl)) {
            Messages.showWarningDialog("该书源搜索URL为空", "提示");
            return;
        }

        String fullUrl = buildSearchUrl(searchUrl, keyword);
        new SearchTask(fullUrl, selectedSite).queue();
    }

    /**
     * 构建搜索URL
     */
    private String buildSearchUrl(String searchUrlTemplate, String keyword) {
        if (searchUrlTemplate.contains("${")) {
            return com.wei.wreader.util.comm.StringTemplateEngine.render(
                    searchUrlTemplate, Map.of("key", keyword, "page", 1));
        }
        return searchUrlTemplate.replace("{keyword}", keyword);
    }

    /**
     * 处理书籍选择 - 加载章节列表到右侧面板
     */
    private void handleBookSelection(int selectedIndex, BookDirectoryListener listener) {
        BookInfo selectedBook = bookInfoList.get(selectedIndex);
        cacheService.setTempSelectedBookInfo(selectedBook);
        cacheService.setSelectedBookInfo(selectedBook);

        // 清空章节列表
        chapterListModel.clear();
        chapterUrlList.clear();

        // 清空章节内容缓存
        cacheService.setChapterList(null);
        cacheService.setChapterUrlList(null);
        cacheService.setChapterContentList(null);
        cacheService.setSelectedChapterInfo(null);

        // 加载目录
        searchService.loadBookDirectory(selectedBook,
                chapterNames -> {
                    if (chapterNames == null || chapterNames.isEmpty()) {
                        Messages.showInfoMessage("未找到章节列表", "提示");
                        return;
                    }

                    // 获取章节URL列表
                    List<String> urls = cacheService.getChapterUrlList();
                    if (urls != null) {
                        chapterUrlList.addAll(urls);
                    }

                    // 更新章节列表
                    for (String name : chapterNames) {
                        chapterListModel.addElement(name);
                    }
                },
                chapterUrls -> {}
        );
    }

    /**
     * 处理章节选择 - 获取章节内容
     */
    private void handleChapterSelection(int idx, BookDirectoryListener listener) {
        String chapterTitle = chapterListModel.get(idx);
        String chapterSuffixUrl = (idx < chapterUrlList.size()) ? chapterUrlList.get(idx) : "";
        String chapterUrl = buildFullChapterUrl(chapterSuffixUrl);

        // 缓存选中的章节信息
        ChapterInfo chapterInfo = new ChapterInfo();
        chapterInfo.setChapterTitle(chapterTitle);
        chapterInfo.setChapterUrl(chapterUrl);
        chapterInfo.setSelectedChapterIndex(idx);
        cacheService.setSelectedChapterInfo(chapterInfo);

        // 缓存章节列表和URL列表
        List<String> chapterNames = new ArrayList<>();
        for (int i = 0; i < chapterListModel.size(); i++) {
            chapterNames.add(chapterListModel.get(i));
        }
        cacheService.setChapterList(chapterNames);
        cacheService.setChapterUrlList(chapterUrlList);

        // 缓存选择信息（从临时缓存转为正式缓存）
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        if (siteBean != null) {
            cacheService.setSelectedSiteBean(siteBean);
        }
        cacheService.setSelectedBookSiteIndex(cacheService.getTempSelectedBookSiteIndex());
        BookInfo bookInfo = cacheService.getTempSelectedBookInfo();
        if (bookInfo != null) {
            cacheService.setSelectedBookInfo(bookInfo);
        }

        // 设置数据加载类型为网络加载
        Settings settings = cacheService.getSettings();
        if (settings == null) {
            settings = new Settings();
        }
        settings.setDataLoadType(DataLoadType.NETWORK.toLegacyValue());
        cacheService.setSettings(settings);

        // 获取章节内容
        searchService.searchBookContentRemote(chapterUrl, param -> {
            processChapterContent(param, chapterInfo, idx, chapterNames, chapterUrlList, listener);
        });

        // 如果有listener，通知它
        if (listener != null) {
            listener.onClickItem(idx, chapterNames, chapterInfo, null);
        }
    }

    /**
     * 构建完整的章节URL
     */
    private String buildFullChapterUrl(String suffixUrl) {
        if (StringUtils.isBlank(suffixUrl)) {
            return suffixUrl;
        }
        if (suffixUrl.startsWith(ConstUtil.HTTP_SCHEME) ||
                suffixUrl.startsWith(ConstUtil.HTTPS_SCHEME) ||
                JsonUtil.isValid(suffixUrl)) {
            return suffixUrl;
        }
        SiteBean siteBean = cacheService.getSelectedSiteBean();
        if (siteBean == null) {
            siteBean = cacheService.getTempSelectedSiteBean();
        }
        String baseUrl = (siteBean != null) ? siteBean.getBaseUrl() : "";
        return baseUrl + suffixUrl;
    }

    /**
     * 处理章节内容 - 缓存并更新显示
     */
    private void processChapterContent(SearchBookCallParam param, ChapterInfo chapterInfo,
                                       int selectedIndex, List<String> chapterNames,
                                       List<String> chapterUrls, BookDirectoryListener listener) {
        String fontColorHex = fontManager.getFontColorHex();
        String fontFamily = fontManager.getFontFamily();
        int fontSize = fontManager.getFontSize();

        String rawContent = HtmlContentRenderer.buildCustomStyleContent(param.getChapterContentHtml(),
                fontColorHex, fontFamily, fontSize);
        final String content = (rawContent != null) ? rawContent.replaceAll("(?s)<style[^>]*>.*?</style>", "") : null;
        chapterInfo.setChapterContent(content);
        chapterInfo.setChapterContentStr(param.getChapterContentText());
        chapterInfo.setSelectedChapterIndex(selectedIndex);

        Settings settings = cacheService.getSettings();
        int singleLineChars = (settings != null) ? settings.getSingleLineChars() : 30;
        chapterInfo.initLineNum(1, 2, 1, singleLineChars);

        cacheService.setSelectedChapterInfo(chapterInfo);

        ToolWindowUtil.updateContentText(project, textPane -> {
            if (content != null) {
                textPane.setText(content);
                textPane.setCaretPosition(0);
            }
        });

        // 加载本章节下一页内容（分页加载）
        Element bodyElement = param.getBodyElement();
        if (bodyElement != null) {
            ReaderOrchestrator.getInstance(project).loadThisChapterNextContent(chapterInfo.getChapterUrl(), bodyElement);
        }
    }

    /**
     * 搜索后台任务
     */
    private class SearchTask extends Task.Backgroundable {
        private final String searchUrl;
        private final SiteBean siteBean;
        private String searchResult = "";
        private Exception error;

        SearchTask(String searchUrl, SiteBean siteBean) {
            super(project, "【W-Reader】正在搜索...");
            this.searchUrl = searchUrl;
            this.siteBean = siteBean;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setIndeterminate(true);
            try {
                searchResult = searchService.searchBookListSync(searchUrl, siteBean);
            } catch (Exception e) {
                error = e;
            }
        }

        @Override
        public void onSuccess() {
            if (error != null) {
                Messages.showErrorDialog("搜索失败: " + error.getMessage(), "提示");
                return;
            }
            if (StringUtils.isBlank(searchResult) || "[]".equals(searchResult)) {
                Messages.showInfoMessage(ConstUtil.WREADER_SEARCH_BOOK_ERROR, "提示");
                return;
            }
            List<BookInfo> results = searchService.parseSearchResults(searchResult, siteBean);
            bookInfoList.clear();
            bookListModel.clear();
            for (BookInfo book : results) {
                bookInfoList.add(book);
                bookListModel.addElement(book.getBookName() +
                        (StringUtils.isNotBlank(book.getBookAuthor()) ? " - " + book.getBookAuthor() : ""));
            }
        }
    }
}
