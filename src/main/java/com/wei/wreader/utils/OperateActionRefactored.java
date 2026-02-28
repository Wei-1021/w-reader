package com.wei.wreader.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.jayway.jsonpath.JsonPath;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.pojo.*;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.CustomSiteRuleCacheServer;
import com.wei.wreader.utils.comm.*;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.data.JsonUtil;
import com.wei.wreader.utils.data.ListUtil;
import com.wei.wreader.utils.data.StringUtil;
import com.wei.wreader.utils.file.EpubReaderComplete;
import com.wei.wreader.utils.file.FileUtil;
import com.wei.wreader.utils.http.HttpUtil;
import com.wei.wreader.utils.tts.EdgeTTS;
import com.wei.wreader.utils.tts.VoiceRole;
import com.wei.wreader.utils.ui.MessageDialogUtil;
import com.wei.wreader.utils.ui.ToolWindowUtil;
import com.wei.wreader.utils.yml.ConfigYaml;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.epub.EpubReader;
import io.documentnode.epub4j.util.IOUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作工具类 - 重构版本
 * <p>
 * 重构目标：
 * 1. 改善巨型类(God Class)问题，按功能职责分离
 * 2. 提升代码可读性和可维护性
 * 3. 保持原有功能完整性和兼容性
 * 4. 添加详细的中文注释说明
 * <p>
 * 主要功能模块：
 * - 小说阅读控制（章节切换、内容加载）
 * - 本地文件处理（TXT/EPUB格式）
 * - 自动阅读功能
 * - 文本转语音(TTS)
 * - 字体和样式管理
 * - 缓存和状态管理
 * <p>
 * 设计原则：
 * - 单一职责原则：每个方法只负责一个功能
 * - 依赖注入：通过构造函数注入依赖
 * - 状态管理：集中管理应用状态
 * - 异常处理：统一的错误处理机制
 *
 * @author weizhanjie
 * @version 2.0
 */
public class OperateActionRefactored {

    // ==================== 常量定义 ====================
    // 任务标题常量
    private static final String LOAD_FILE_TASK_TITLE = "【W-Reader】正在读取文件...";
    private static final String GET_CONTENT_TASK_TITLE = "【W-Reader】正在获取内容...";
    private static final String LOAD_NEXT_CONTENT_TITLE = "【W-Reader】加载本章节下一页内容";

    // 默认值常量
    private static final String DEFAULT_FONT_FAMILY = ConstUtil.DEFAULT_FONT_FAMILY;
    private static final int DEFAULT_FONT_SIZE = ConstUtil.DEFAULT_FONT_SIZE;
    private static final String DEFAULT_FONT_COLOR_HEX = ConstUtil.DEFAULT_FONT_COLOR_HEX;

    // ==================== 依赖服务 ====================
    // 核心服务依赖（通过构造函数注入）
    private final ConfigYaml configYaml;
    private final CacheService cacheService;
    private final CustomSiteRuleCacheServer customSiteRuleCacheServer;
    private final CustomSiteUtil customSiteUtil;
    private final Project project;

    // ==================== 状态管理 ====================
    // 单例实例和项目引用
    private static OperateActionRefactored instance;
    private static Project mProject;

    // 应用设置和配置
    private Settings settings;

    // 数据状态变量
    private List<String> bookNameList = new ArrayList<>();
    private List<BookInfo> bookInfoList = new ArrayList<>();
    private List<String> chapterList = new ArrayList<>();
    private List<String> chapterUrlList = new ArrayList<>();
    private List<String> chapterContentList = new ArrayList<>();
    private int currentChapterIndex = 0;

    // 字体和样式设置
    private String fontFamily = DEFAULT_FONT_FAMILY;
    private int fontSize = DEFAULT_FONT_SIZE;
    private String fontColorHex = DEFAULT_FONT_COLOR_HEX;
    private String chapterContentHtml = "";
    private String chapterContentText = "";
    private String contentOriginalStyle = "";

    // 站点相关信息
    private static String baseUrl;
    private static int selectedBookSiteIndex = 0;
    private List<SiteBean> siteBeanList;
    private static String selectSiteGroupName;
    private static SiteBean selectedSiteBean;
    private static SearchRules selectedSearchRules;
    private static ListMainRules selectedListMainRules;
    private static ChapterRules selectedChapterRules;
    private static BookInfoRules selectedBookInfoRules;
    private static SiteBean tempSelectedSiteBean;

    // 当前选中的书籍和章节信息
    private BookInfo selectBookInfo = new BookInfo();
    private ChapterInfo currentChapterInfo = new ChapterInfo();
    private DialogBuilder searchBookDialogBuilder;

    // ==================== 服务组件 ====================
    // 定时任务执行器
    private static ScheduledExecutorService executorService;

    // TTS语音服务
    private static EdgeTTS edgeTTS;

    // 后台任务引用
    private Task.Backgroundable nextContentTask;

    // 状态标志
    private static boolean IS_SWITCH_NEXT_CHAPTER_SUCCESS = false;

    // ==================== 构造函数和单例模式 ====================

    /**
     * 获取单例实例
     *
     * @param project IntelliJ项目实例
     * @return OperateActionUtilRefactored实例
     */
    public static OperateActionRefactored getInstance(Project project) {
        if (instance == null || !project.equals(mProject) || mProject.isDisposed()) {
            instance = new OperateActionRefactored(project);
        }
        instance.initData();
        return instance;
    }

    /**
     * 私有构造函数 - 依赖注入模式
     *
     * @param project IntelliJ项目实例
     */
    private OperateActionRefactored(Project project) {
        this.project = project;
        this.mProject = project;

        // 初始化依赖服务
        this.configYaml = new ConfigYaml();
        this.cacheService = CacheService.getInstance();
        this.customSiteUtil = CustomSiteUtil.getInstance(project);
        this.customSiteRuleCacheServer = CustomSiteRuleCacheServer.getInstance();
    }

    // ==================== 数据初始化方法 ====================

    /**
     * 初始化应用数据和状态
     * 包括设置、字体、站点信息、缓存数据等
     */
    private void initData() {
        try {
            // 初始化应用设置
            initializeSettings();

            // 初始化字体和颜色设置
            initializeFontSettings();

            // 初始化站点信息
            initializeSiteInfo();

            // 初始化缓存数据
            initializeCachedData();

            // 初始化当前状态
            initializeCurrentState();

        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_INIT_ERROR, "Error");
            e.printStackTrace();
        }
    }

    /**
     * 初始化应用设置
     */
    private void initializeSettings() {
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }
        if (StringUtils.isBlank(settings.getCharset())) {
            settings.setCharset(configYaml.getSettings().getCharset());
        }
    }

    /**
     * 初始化字体和颜色设置
     */
    private void initializeFontSettings() {
        // 字体族设置
        fontFamily = cacheService.getFontFamily();
        if (fontFamily == null || fontFamily.isEmpty() || "JetBrains Mono".equals(fontFamily)) {
            fontFamily = DEFAULT_FONT_FAMILY;
            cacheService.setFontFamily(fontFamily);
        }

        // 字体大小设置
        fontSize = cacheService.getFontSize();
        if (fontSize == 0) {
            fontSize = DEFAULT_FONT_SIZE;
            cacheService.setFontSize(fontSize);
        }

        // 字体颜色设置
        fontColorHex = cacheService.getFontColorHex();
        if (fontColorHex == null || fontColorHex.isEmpty()) {
            // 获取当前UI主题的前景色
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
            Color defaultForeground = scheme.getDefaultForeground();
            fontColorHex = String.format("#%02x%02x%02x",
                    defaultForeground.getRed(),
                    defaultForeground.getGreen(),
                    defaultForeground.getBlue());
            cacheService.setFontColorHex(fontColorHex);
        }
    }

    /**
     * 初始化站点信息
     */
    private void initializeSiteInfo() {
        String selectedCustomSiteRuleKey = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();

        // 根据选择的规则确定站点列表
        if (StringUtils.isBlank(selectedCustomSiteRuleKey) ||
                ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(selectedCustomSiteRuleKey)) {
            siteBeanList = configYaml.getSiteList();
        } else {
            Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
            siteBeanList = siteMap.get(selectedCustomSiteRuleKey);
        }

        // 初始化选中的站点索引
        Integer selectedBookSiteIndexTemp = cacheService.getSelectedBookSiteIndex();
        if (selectedBookSiteIndexTemp == null) {
            selectedBookSiteIndex = 0;
            cacheService.setSelectedBookSiteIndex(0);
        } else {
            selectedBookSiteIndex = selectedBookSiteIndexTemp;
        }

        // 边界检查和站点信息设置
        if (selectedBookSiteIndex > siteBeanList.size()) {
            selectedBookSiteIndex = 0;
            cacheService.setSelectedBookSiteIndex(0);
        }

        selectedSiteBean = siteBeanList.get(selectedBookSiteIndex);
        if (selectedSiteBean == null) {
            selectedSiteBean = siteBeanList.get(selectedBookSiteIndex);
            cacheService.setSelectedSiteBean(selectedSiteBean);
        }

        // 设置各规则引用
        selectedSearchRules = selectedSiteBean.getSearchRules();
        selectedListMainRules = selectedSiteBean.getListMainRules();
        selectedChapterRules = selectedSiteBean.getChapterRules();
        selectedBookInfoRules = selectedSiteBean.getBookInfoRules();

        // 设置基础URL
        baseUrl = selectedSiteBean.getBaseUrl();
    }

    /**
     * 初始化缓存数据
     */
    private void initializeCachedData() {
        // 初始化选中的书籍信息
        selectBookInfo = cacheService.getSelectedBookInfo();

        // 初始化章节信息
        currentChapterInfo = cacheService.getSelectedChapterInfo();
        if (currentChapterInfo == null) {
            currentChapterInfo = new ChapterInfo();
        }
        currentChapterIndex = currentChapterInfo.getSelectedChapterIndex();

        // 初始化章节列表数据
        chapterList = cacheService.getChapterList();
        chapterUrlList = cacheService.getChapterUrlList();
    }

    /**
     * 初始化当前状态
     */
    private void initializeCurrentState() {
        chapterContentHtml = currentChapterInfo.getChapterContent();
        chapterContentText = currentChapterInfo.getChapterContentStr();

        // 如果没有内容，设置初始文本
        if (chapterContentHtml == null || chapterContentHtml.isEmpty()) {
            chapterContentHtml = "<pre>" + ConstUtil.WREADER_TOOL_WINDOW_CONTENT_INIT_TEXT + "</pre>";
        } else {
            updateContentText();
        }
    }

    // ==================== 目录显示和章节切换方法 ====================

    /**
     * 显示当前小说目录
     *
     * @param listener 目录项点击监听器
     */
    public void showBookDirectory(BookDirectoryListener listener) {
        SwingUtilities.invokeLater(() -> {
            // 检查数据加载模式和数据完整性
            int dataLoadType = settings.getDataLoadType();

            // 本地加载模式检查
            if (dataLoadType == Settings.DATA_LOAD_TYPE_LOCAL) {
                if (ListUtil.isEmpty(cacheService.getChapterContentList())) {
                    Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CHAPTER_LIST_ERROR, "提示");
                    return;
                }
            }

            // 章节列表检查
            if (ListUtil.isEmpty(chapterList)) {
                Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CHAPTER_LIST_ERROR, "提示");
                return;
            }

            // 创建目录列表组件
            JBList<String> chapterListJBList = createChapterListComponent();
            chapterListJBList.setSelectedIndex(currentChapterIndex);
            chapterListJBList.ensureIndexIsVisible(currentChapterIndex);

            // 添加选择监听器
            chapterListJBList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    handleChapterSelection((ListSelectionEvent) e, chapterListJBList, listener, dataLoadType);
                }
            });

            // 显示目录对话框
            JBScrollPane scrollPane = new JBScrollPane(chapterListJBList);
            scrollPane.setPreferredSize(new Dimension(400, 500));
            MessageDialogUtil.showMessage(project, "目录", scrollPane);
        });
    }

    /**
     * 创建章节列表组件
     *
     * @return 配置好的JBList组件
     */
    private JBList<String> createChapterListComponent() {
        JBList<String> chapterListJBList = new JBList<>(chapterList);
        chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chapterListJBList.setBorder(JBUI.Borders.empty());
        return chapterListJBList;
    }

    /**
     * 处理章节选择事件
     *
     * @param event             选择事件
     * @param chapterListJBList 章节列表组件
     * @param listener          回调监听器
     * @param dataLoadType      数据加载模式
     */
    private void handleChapterSelection(ListSelectionEvent event,
                                        JBList<String> chapterListJBList,
                                        BookDirectoryListener listener,
                                        int dataLoadType) {
        switch (dataLoadType) {
            case Settings.DATA_LOAD_TYPE_NETWORK:
                loadBookDirectoryRemote(chapterListJBList, listener);
                break;
            case Settings.DATA_LOAD_TYPE_LOCAL:
                loadBookDirectoryLocal(chapterListJBList, listener);
                break;
        }
    }

    /**
     * 远程加载小说目录
     *
     * @param chapterListJBList 章节列表组件
     * @param listener          回调监听器
     */
    public void loadBookDirectoryRemote(JBList<String> chapterListJBList, BookDirectoryListener listener) {
        // 获取选中的章节索引和信息
        int selectedIndex = chapterListJBList.getSelectedIndex();
        currentChapterIndex = selectedIndex;

        String chapterTitle = chapterList.get(currentChapterIndex);
        if (ListUtil.isEmpty(chapterUrlList)) {
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
            return;
        }

        // 构建章节URL
        String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
        String chapterUrl = buildFullChapterUrl(chapterSuffixUrl);

        // 更新章节信息
        currentChapterInfo.setChapterTitle(chapterTitle);
        currentChapterInfo.setChapterUrl(chapterUrl);

        // 远程获取章节内容
        searchBookContentRemote(chapterUrl, (searchBookCallParam) -> {
            chapterContentHtml = searchBookCallParam.getChapterContentHtml();
            chapterContentText = searchBookCallParam.getChapterContentText();
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);

            if (listener != null) {
                listener.onClickItem(selectedIndex, chapterList, currentChapterInfo, searchBookCallParam.getBodyElement());
            }
        });
    }

    /**
     * 本地加载小说目录
     *
     * @param chapterListJBList 章节列表组件
     * @param listener          回调监听器
     */
    public void loadBookDirectoryLocal(JBList<String> chapterListJBList, BookDirectoryListener listener) {
        int selectedIndex = chapterListJBList.getSelectedIndex();
        currentChapterIndex = selectedIndex;

        chapterContentList = cacheService.getChapterContentList();
        if (chapterContentList != null && !chapterContentList.isEmpty()) {
            String chapterTitle = chapterList.get(currentChapterIndex);
            currentChapterInfo.setChapterTitle(chapterTitle);

            // 获取并处理章节内容
            chapterContentHtml = chapterContentList.get(currentChapterIndex);
            chapterContentText = processChapterContentText(chapterContentHtml);

            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);

            if (listener != null) {
                listener.onClickItem(selectedIndex, chapterList, currentChapterInfo, null);
            }
        }
    }

    /**
     * 构建完整的章节URL
     *
     * @param chapterSuffixUrl 章节URL后缀
     * @return 完整的章节URL
     */
    private String buildFullChapterUrl(String chapterSuffixUrl) {
        if (chapterSuffixUrl.startsWith(ConstUtil.HTTP_SCHEME) ||
                chapterSuffixUrl.startsWith(ConstUtil.HTTPS_SCHEME) ||
                JsonUtil.isValid(selectBookInfo.getBookUrl())) {
            return chapterSuffixUrl;
        }
        return baseUrl + chapterSuffixUrl;
    }

    /**
     * 处理章节内容文本（去除HTML标签等）
     *
     * @param contentHtml HTML格式的内容
     * @return 处理后的纯文本
     */
    private String processChapterContentText(String contentHtml) {
        Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
        String text = pattern.matcher(contentHtml).replaceAll("　");
        text = StringUtils.normalizeSpace(text);
        return StringEscapeUtils.unescapeHtml4(text);
    }

    // ==================== 章节导航方法 ====================

    /**
     * 切换到上一个章节
     *
     * @param runnable 切换完成后的回调
     */
    public void prevPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        try {
            // 检查是否已经是第一章
            if (currentChapterIndex <= 0) {
                return;
            }

            currentChapterIndex--;
            String chapterTitle = chapterList.get(currentChapterIndex);
            currentChapterInfo.setChapterTitle(chapterTitle);

            int dataLoadType = settings.getDataLoadType();
            if (dataLoadType == Settings.DATA_LOAD_TYPE_NETWORK) {
                loadPrevChapterNetwork(runnable);
            } else if (dataLoadType == Settings.DATA_LOAD_TYPE_LOCAL) {
                loadPrevChapterLocal(runnable);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 网络模式加载上一章
     */
    private void loadPrevChapterNetwork(BiConsumer<ChapterInfo, Element> runnable) {
        if (ListUtil.isEmpty(chapterUrlList)) {
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
            return;
        }

        String prevChapterSuffixUrl = chapterUrlList.get(currentChapterIndex);
        String prevChapterUrl = buildFullChapterUrl(prevChapterSuffixUrl);
        currentChapterInfo.setChapterUrl(prevChapterUrl);

        searchBookContentRemote(prevChapterUrl, (searchBookCallParam) -> {
            chapterContentHtml = searchBookCallParam.getChapterContentHtml();
            chapterContentText = searchBookCallParam.getChapterContentText();
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);
            runnable.accept(currentChapterInfo, searchBookCallParam.getBodyElement());
        });
    }

    /**
     * 本地模式加载上一章
     */
    private void loadPrevChapterLocal(BiConsumer<ChapterInfo, Element> runnable) {
        chapterContentList = cacheService.getChapterContentList();
        if (chapterContentList != null && !chapterContentList.isEmpty()) {
            chapterContentHtml = chapterContentList.get(currentChapterIndex);
            chapterContentText = processChapterContentText(chapterContentHtml);
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);
        }
        runnable.accept(currentChapterInfo, null);
    }

    /**
     * 切换到下一个章节
     *
     * @param runnable 切换完成后的回调
     */
    public void nextPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        try {
            IS_SWITCH_NEXT_CHAPTER_SUCCESS = false;
            int dataLoadType = settings.getDataLoadType();

            if (dataLoadType == Settings.DATA_LOAD_TYPE_NETWORK) {
                loadNextChapterNetwork(runnable);
            } else if (dataLoadType == Settings.DATA_LOAD_TYPE_LOCAL) {
                loadNextChapterLocal(runnable);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 网络模式加载下一章
     */
    private void loadNextChapterNetwork(BiConsumer<ChapterInfo, Element> runnable) {
        if (ListUtil.isEmpty(chapterUrlList)) {
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
            return;
        }

        // 检查是否已经是最后一章
        if (currentChapterIndex >= chapterUrlList.size() - 1) {
            return;
        }

        currentChapterIndex++;
        String chapterTitle = chapterList.get(currentChapterIndex);
        currentChapterInfo.setChapterTitle(chapterTitle);

        String nextChapterSuffixUrl = chapterUrlList.get(currentChapterIndex);
        String nextChapterUrl = buildFullChapterUrl(nextChapterSuffixUrl);
        currentChapterInfo.setChapterUrl(nextChapterUrl);

        searchBookContentRemote(nextChapterUrl, (searchBookCallParam) -> {
            chapterContentHtml = searchBookCallParam.getChapterContentHtml();
            chapterContentText = searchBookCallParam.getChapterContentText();
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);
            IS_SWITCH_NEXT_CHAPTER_SUCCESS = true;
            runnable.accept(currentChapterInfo, searchBookCallParam.getBodyElement());
        });
    }

    /**
     * 本地模式加载下一章
     */
    private void loadNextChapterLocal(BiConsumer<ChapterInfo, Element> runnable) {
        chapterContentList = cacheService.getChapterContentList();
        if (currentChapterIndex >= chapterContentList.size() - 1) {
            return;
        }

        currentChapterIndex++;
        String chapterTitle = chapterList.get(currentChapterIndex);
        currentChapterInfo.setChapterTitle(chapterTitle);

        if (!chapterContentList.isEmpty()) {
            chapterContentHtml = chapterContentList.get(currentChapterIndex);
            chapterContentText = processChapterContentText(chapterContentHtml);
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);
            IS_SWITCH_NEXT_CHAPTER_SUCCESS = true;
        }
        runnable.accept(currentChapterInfo, null);
    }

    // ==================== 内容加载和处理方法 ====================

    /**
     * 远程获取小说内容
     *
     * @param url      内容URL
     * @param callback 获取成功后的回调
     */
    public void searchBookContentRemote(String url, Consumer<SearchBookCallParam> callback) {
        new ContentLoadTask(url, callback).queue();
    }

    /**
     * 内容加载后台任务内部类
     */
    private class ContentLoadTask extends Task.Backgroundable {
        private final String url;
        private final Consumer<SearchBookCallParam> callback;
        private String chapterContent = "";
        private Element bodyElement;
        private String bodyElementStr;

        public ContentLoadTask(String url, Consumer<SearchBookCallParam> callback) {
            super(project, GET_CONTENT_TASK_TITLE);
            this.url = url;
            this.callback = callback;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText(GET_CONTENT_TASK_TITLE);
            indicator.setIndeterminate(true);

            SiteBean siteBean = cacheService.getSelectedSiteBean();
            ChapterRules chapterRules = siteBean.getChapterRules();

            // 根据配置决定使用API还是HTML方式加载内容
            if (shouldUseApiMethod(chapterRules)) {
                loadContentViaApi(indicator, siteBean, chapterRules);
            } else {
                loadContentViaHtml(indicator, siteBean, chapterRules);
            }

            indicator.setFraction(1.0);
        }


        /**
         * 判断是否应该使用API方式加载内容
         */
        private boolean shouldUseApiMethod(ChapterRules rules) {
            String contentUrl = rules.getUrl();
            String dataRule = rules.getUrlDataRule();
            return StringUtils.isNotBlank(contentUrl) && StringUtils.isNotBlank(dataRule);
        }

        /**
         * 通过API方式加载内容
         */
        private void loadContentViaApi(ProgressIndicator indicator, SiteBean siteBean, ChapterRules rules) {
            HttpRequestBase requestBase = HttpUtil.commonRequest(url);
            requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

            try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = httpResponse.getEntity();
                    String result = EntityUtils.toString(entity);
                    JsonObject menuListJson = new Gson().fromJson(result, JsonObject.class);

                    Object readJson = JsonPath.read(menuListJson.toString(), rules.getUrlDataRule());
                    chapterContent = readJson.toString();
                    bodyElementStr = result;
                }
            } catch (Exception e) {
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                throw new RuntimeException(e);
            }
        }

        /**
         * 通过HTML方式加载内容
         */
        private void loadContentViaHtml(ProgressIndicator indicator, SiteBean siteBean, ChapterRules rules) {
            try {
                Document document = Jsoup.connect(url)
                        .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                        .get();

                Element headElement = document.head();
                boolean isContentOriginalStyle = rules.isUseContentOriginalStyle();

                // 处理原始样式
                if (isContentOriginalStyle) {
                    extractOriginalStyles(headElement, rules);
                }

                bodyElement = document.body();
                Elements chapterContentElements = bodyElement.select(rules.getContentElementName());

                if (chapterContentElements.isEmpty()) {
                    Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_BOOK_CONTENT_ERROR, "提示");
                    return;
                }

                chapterContent = buildContentHtml(chapterContentElements);
                chapterContentText = chapterContentElements.text();
                bodyElementStr = bodyElement.html();

            } catch (IOException e) {
                e.printStackTrace();
                Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR + "\n" + e.getMessage(), "提示");
            }
        }

        /**
         * 提取原始CSS样式
         */
        private void extractOriginalStyles(Element headElement, ChapterRules rules) {
            StringBuilder allStyle = new StringBuilder();
            Elements styles = headElement.getElementsByTag("style");

            for (Element style : styles) {
                String styleText = style.html();
                String replacement = rules.getReplaceContentOriginalRegex();
                styleText = styleText.replaceAll(replacement, ConstUtil.NEW_FONT_CLASS_CSS_NAME);
                styleText = styleText.replaceAll(ConstUtil.HTML_TAG_REGEX_STR, "");
                allStyle.append(styleText);
            }

            contentOriginalStyle = "<style>" + allStyle + "</style>";
        }

        /**
         * 构建内容HTML
         */
        private String buildContentHtml(Elements elements) {
            StringBuilder contentHtml = new StringBuilder();
            for (Element element : elements) {
                Tag tag = element.tag();
                String html = element.html();
                if (!tag.isEmpty() && StringUtils.trimToNull(html) != null) {
                    contentHtml.append(String.format("<%s>%s</%s>",
                            tag.normalName(), html, tag.normalName()));
                }
            }
            return contentHtml.toString();
        }

        @Override
        public void onSuccess() {
            try {
                chapterContent = handleContent(chapterContent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            chapterContent = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" +
                    fontColorHex + ";\">" + currentChapterInfo.getChapterTitle() +
                    "</h3>" + chapterContent;
            chapterContentHtml = chapterContent;

            Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
            chapterContentText = pattern.matcher(chapterContentHtml).replaceAll("　");
            chapterContentText = StringUtils.normalizeSpace(chapterContentText);
            chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);

            SearchBookCallParam searchBookCallParam = new SearchBookCallParam();
            searchBookCallParam.setBodyElement(bodyElement);
            searchBookCallParam.setBodyContentStr(bodyElementStr);
            searchBookCallParam.setChapterContentHtml(chapterContent);
            searchBookCallParam.setChapterContentText(chapterContentText);
            callback.accept(searchBookCallParam);
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            error.printStackTrace();
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
        }
    }

    /**
     * 处理小说内容（应用各种规则和正则表达式）
     *
     * @param content 原始内容
     * @return 处理后的内容
     * @throws Exception 处理过程中的异常
     */
    public String handleContent(String content) throws Exception {
        SiteBean siteBean = cacheService.getSelectedSiteBean();
        ChapterRules chapterRules = siteBean.getChapterRules();
        String handleRule = chapterRules.getContentHandleRule();

        // 根据配置规则处理内容
        String processedContent = processContentByRule(content, handleRule);
        return formatAndApplyRegex(processedContent, chapterRules);
    }


    /**
     * 根据规则处理内容
     *
     * @param content    原始内容
     * @param handleRule 处理规则
     * @return 处理后的内容
     * @throws Exception 处理异常
     */
    private String processContentByRule(String content, String handleRule) throws Exception {
        // 判断是否为脚本代码配置，是则执行脚本返回处理后的内容，否则返回原始内容
        return ScriptCodeUtil.isJavaCodeConfig(handleRule) ?
                executeContentHandleScript(content, handleRule) :
                content;
    }

    /**
     * 执行内容处理脚本
     */
    private String executeContentHandleScript(String content, String handleRule) throws Exception {
        // 判断是否为旧版Java代码配置
        if (ScriptCodeUtil.isOldJavaCodeConfig(handleRule)) {
            String renderedScript = StringTemplateEngine.render(handleRule, Map.of("content", content));
            return MethodExecutor.executeMethod(renderedScript).toString();
        } else {
            return (String) ScriptCodeUtil.getScriptCodeExeResult(
                    handleRule,
                    new Class[]{String.class},
                    new Object[]{content},
                    Map.of("content", content)
            );
        }
    }
    // ==================== 下一页内容加载方法 ====================

    /**
     * 加载本章节下一页的内容
     *
     * @param chapterUrl  章节URL
     * @param bodyElement 页面body元素
     */
    public void loadThisChapterNextContent(String chapterUrl, Element bodyElement) {
        loadThisChapterNextContent(chapterUrl, bodyElement.html());
    }

    /**
     * 加载本章节下一页的内容
     *
     * @param chapterUrl     章节URL
     * @param bodyElementStr 页面body元素字符串
     */
    public void loadThisChapterNextContent(String chapterUrl, String bodyElementStr) {
        // 检查显示类型
        if (settings.getDisplayType() != Settings.DISPLAY_TYPE_SIDEBAR) {
            return;
        }

        SiteBean siteBean = cacheService.getSelectedSiteBean();
        ChapterRules chapterRules = siteBean.getChapterRules();

        // 验证必要配置
        if (chapterRules == null || StringUtils.isEmpty(chapterRules.getNextContentUrl())) {
            return;
        }

        // 检查是否为脚本配置
        if (!ScriptCodeUtil.isJavaCodeConfig(chapterRules.getNextContentUrl())) {
            return;
        }

        // 取消之前的任务
        if (nextContentTask != null) {
            nextContentTask.onCancel();
        }

        nextContentTask = new NextContentLoadTask(chapterUrl, bodyElementStr);
        nextContentTask.queue();
    }

    /**
     * 下一页内容加载任务内部类
     */
    private class NextContentLoadTask extends Task.Backgroundable {
        private final String chapterUrl;
        private final String initialBodyContent;
        private volatile boolean isRunning = true;
        private String returnResult;
        private final String nextContentUrl;
        private final StringBuilder nextContent = new StringBuilder();

        public NextContentLoadTask(String chapterUrl, String bodyContent) {
            super(project, LOAD_NEXT_CONTENT_TITLE);
            this.chapterUrl = chapterUrl;
            this.initialBodyContent = bodyContent;
            this.nextContentUrl = getNextContentUrl();
            this.returnResult = nextContentUrl;
        }

        private String getNextContentUrl() {
            SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
            ChapterRules chapterRules = selectedSiteBean.getChapterRules();
            return chapterRules.getNextContentUrl();
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setIndeterminate(true);

            try {
                String baseUrl = selectedSiteBean.getBaseUrl();
                String previousContentUrl = "";
                AtomicReference<String> previousPageContent = new AtomicReference<>(initialBodyContent);
                int pageCount = 0;

                while (isRunning) {
                    indicator.checkCanceled();
                    pageCount++;
                    indicator.setText2("正在加载第 " + pageCount + " 页...");

                    // 执行动态代码获取下一页URL
                    returnResult = executeNextContentScript(chapterUrl, pageCount,
                            previousContentUrl, previousPageContent.get());

                    // 如果没有更多内容则停止
                    if (StringUtils.isBlank(returnResult)) {
                        isRunning = false;
                        break;
                    }

                    returnResult = UrlUtil.buildFullURL(baseUrl, returnResult);
                    previousContentUrl = returnResult;

                    // 请求并追加内容
                    nextContent.append(requestContent(returnResult, previousPageContent::set));

                    Thread.sleep(1000); // 添加延迟避免请求过于频繁
                }
            } catch (ProcessCanceledException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog(
                            "本章节下一页内容加载失败: " + e.getMessage(),
                            "提示"
                    );
                });
            }
        }

        /**
         * 执行下一页内容脚本
         */
        private String executeNextContentScript(String chapterUrl, int pageCount,
                                                String previousUrl, String previousContent) {
            try {
                return (String) ScriptCodeUtil.getScriptCodeExeResult(
                        nextContentUrl,
                        new Class[]{String.class, int.class, String.class, String.class},
                        new Object[]{chapterUrl, pageCount, previousUrl, previousContent},
                        Map.of(
                                "chapterUrl", chapterUrl,
                                "loadingPage", pageCount,
                                "preContentUrl", previousUrl,
                                "prePageContent", previousContent
                        )
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onCancel() {
            isRunning = false;
            super.onCancel();
        }

        @Override
        public void onSuccess() {
            ToolWindowUtil.updateContentText(project, contentTextPanel -> {
                ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();

                String text = nextContent.toString();
                int caretPosition = contentTextPanel.getCaretPosition();
                text = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" +
                        fontColorHex + ";\">" + selectedChapterInfo.getChapterTitle() + "</h3>" + text;
                String newText = getContent(text);
                contentTextPanel.setText(newText);
                contentTextPanel.setCaretPosition(caretPosition);

                updateChapterInfoWithContent(text);
            });
        }

        /**
         * 使用新内容更新章节信息
         */
        private void updateChapterInfoWithContent(String text) {
            SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
            ChapterRules chapterRules = selectedSiteBean.getChapterRules();

            Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
            String chapterContentText = pattern.matcher(text).replaceAll("　");
            chapterContentText = StringUtils.normalizeSpace(chapterContentText);
            chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);
            chapterContentText = formatAndApplyRegex(chapterContentText, chapterRules);

            ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
            selectedChapterInfo.setChapterContent(text);
            selectedChapterInfo.setChapterContentStr(chapterContentText);
            cacheService.setSelectedChapterInfo(selectedChapterInfo);
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            if (!(error instanceof ProcessCanceledException)) {
                super.onThrowable(error);
            }
        }
    }

    // ==================== 本地文件加载方法 ====================

    /**
     * 加载本地文件
     *
     * @param regex TXT文件章节分割正则表达式
     */
    public void loadLocalFile(String regex) {
        initData();

        // 创建文件选择器
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false,
                false, false, false, false);
        fileChooserDescriptor.setTitle("选择文本文件");
        fileChooserDescriptor.setDescription(ConstUtil.WREADER_LOAD_LOCAL_TIP);

        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, null, null);
        if (virtualFile != null) {
            processSelectedFile(virtualFile, regex);
        }
    }

    /**
     * 处理选中的文件
     */
    private void processSelectedFile(VirtualFile virtualFile, String regex) {
        String filePath = virtualFile.getPath();
        String fileName = virtualFile.getName();
        String fileExtension = virtualFile.getExtension();

        List<String> allowFileExtensions = configYaml.getAllowFileExtension();
        if (fileExtension == null || !allowFileExtensions.contains(fileExtension)) {
            String message = String.format(ConstUtil.WREADER_ONLY_SUPPORTED_FILE_TYPE,
                    allowFileExtensions.toString());
            Messages.showMessageDialog(message, "提示", Messages.getInformationIcon());
            return;
        }

        File file = new File(filePath);

        // 停止相关服务并重置状态
        stopTTS();
        cacheService.setEditorMessageVerticalScrollValue(0);
        clearCacheData();

        // 启动文件加载任务
        Task.Backgroundable loadLocalFileTask = new LocalFileLoadTask(file, fileExtension, regex);
        loadLocalFileTask.queue();
    }

    /**
     * 清空缓存数据
     */
    private void clearCacheData() {
        cacheService.setChapterList(null);
        cacheService.setChapterContentList(null);
        cacheService.setSelectedChapterInfo(null);
        cacheService.setSelectedBookInfo(null);
        cacheService.setChapterUrlList(null);
    }

    /**
     * 本地文件加载任务内部类
     */
    private class LocalFileLoadTask extends Task.Backgroundable {
        private final File file;
        private final String fileExtension;
        private final String regex;

        public LocalFileLoadTask(File file, String fileExtension, String regex) {
            super(project, LOAD_FILE_TASK_TITLE);
            this.file = file;
            this.fileExtension = fileExtension;
            this.regex = regex;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText(LOAD_FILE_TASK_TITLE);
            indicator.setIndeterminate(true);

            if (ConstUtil.FILE_TYPE_TXT.equalsIgnoreCase(fileExtension)) {
                loadFileTypeTxt(file, regex);
            } else if (ConstUtil.FILE_TYPE_EPUB.equalsIgnoreCase(fileExtension)) {
                loadFileTypeEpub(file);
            }
        }

        @Override
        public void onSuccess() {
            cacheService.setSelectedChapterInfo(ChapterInfo.initEmptyChapterInfo());
            currentChapterIndex = 0;

            settings.setDataLoadType(Settings.DATA_LOAD_TYPE_LOCAL);
            cacheService.setSettings(settings);

            ToolWindowUtil.updateContentText(project, "");
            Messages.showMessageDialog(ConstUtil.WREADER_LOAD_SUCCESS, "提示", Messages.getInformationIcon());
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            super.onThrowable(error);
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
        }
    }

    /**
     * 加载TXT格式文件
     *
     * @param file  文件对象
     * @param regex 章节分割正则表达式
     */
    public void loadFileTypeTxt(File file, String regex) {
        String textRegex = StringUtils.isEmpty(regex) ? ConstUtil.TEXT_FILE_DIR_REGEX : regex;
        String charset = settings.getCharset();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {

            StringBuilder contentBuilder = new StringBuilder();
            String line;
            List<String> chapterList = new ArrayList<>();
            List<String> chapterContentList = new ArrayList<>();

            // 按正则表达式分割章节
            while ((line = reader.readLine()) != null) {
                Pattern pattern = Pattern.compile(textRegex);
                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    if (!chapterList.isEmpty()) {
                        chapterContentList.add(contentBuilder.toString());
                    }
                    chapterList.add(line);
                    contentBuilder.setLength(0);
                }
                contentBuilder.append(line).append("<br>");
            }
            chapterContentList.add(contentBuilder.toString());

            // 缓存章节数据
            cacheService.setChapterList(chapterList);
            cacheService.setChapterContentList(chapterContentList);
            this.chapterList = chapterList;
            this.chapterContentList = chapterContentList;

            // 创建并缓存书籍信息
            BookInfo bookInfo = new BookInfo();
            String fileName = file.getName();
            bookInfo.setBookName(fileName);
            bookInfo.setBookDesc(fileName);
            cacheService.setSelectedBookInfo(bookInfo);

        } catch (IOException e) {
            e.printStackTrace();
            Messages.showMessageDialog(ConstUtil.WREADER_LOAD_FAIL, "提示", Messages.getInformationIcon());
        }
    }

    /**
     * 加载EPUB格式文件
     *
     * @param file EPUB文件对象
     */
    public void loadFileTypeEpub(File file) {
        String charset = settings.getCharset();
        boolean isShowLocalImg = settings.isShowLocalImg();

        try (FileInputStream fis = new FileInputStream(file)) {
            // 获取并清空临时目录
            String tempDirPath = getTempDirectoryPath();
            FileUtils.deleteDirectory(new File(tempDirPath));

            // 读取EPUB文件
            EpubReader epubReader = new EpubReader();
            Book book = epubReader.readEpub(fis, charset);

            // 处理图片资源
            Map<String, String> imgTempPathMap = new HashMap<>();
            Map<String, Integer> imgTempWidthMap = new HashMap<>();

            if (isShowLocalImg) {
                processEpubImages(book, tempDirPath, imgTempPathMap, imgTempWidthMap);
            }

            // 读取章节内容
            List<String> chapterList = new ArrayList<>();
            List<String> chapterContentList = new ArrayList<>();

            EpubReaderComplete.readEpub(book, resMap -> {
                String title = resMap.get("title");
                String content = resMap.get("content");
                content = StringUtil.extractBodyContent(content);

                if (isShowLocalImg) {
                    content = StringUtil.replaceImageLinks(content, imgTempPathMap, imgTempWidthMap);
                }

                chapterList.add(title);
                chapterContentList.add(content);
            });

            // 缓存数据
            cacheService.setChapterList(chapterList);
            cacheService.setChapterContentList(chapterContentList);
            this.chapterList = chapterList;
            this.chapterContentList = chapterContentList;

            // 保存书籍元数据
            saveBookMetadata(book);

        } catch (IOException e) {
            e.printStackTrace();
            Messages.showMessageDialog(ConstUtil.WREADER_LOAD_FAIL, "提示", Messages.getInformationIcon());
        }
    }

    /**
     * 获取临时目录路径
     */
    private String getTempDirectoryPath() {
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempDirFile = new File(tempDir);
        return tempDirFile.getAbsolutePath() + File.separator + ConstUtil.WREADER_ID +
                File.separator + "images" + File.separator;
    }

    /**
     * 处理EPUB中的图片资源
     */
    private void processEpubImages(Book book, String tempDirPath,
                                   Map<String, String> imgPathMap,
                                   Map<String, Integer> imgWidthMap) throws IOException {
        Map<String, Resource> resourceMap = book.getResources().getResourceMap();

        for (Map.Entry<String, Resource> entry : resourceMap.entrySet()) {
            Resource resource = entry.getValue();
            String key = entry.getKey();

            if ((resource.getMediaType() != null &&
                    resource.getMediaType().getName().startsWith("image/")) ||
                    FileUtil.isUnsupportedImageFormat(key)) {

                processImageResource(resource, key, tempDirPath, imgPathMap, imgWidthMap);
            }
        }
    }

    /**
     * 处理单个图片资源
     */
    private void processImageResource(Resource resource, String key, String tempDirPath,
                                      Map<String, String> imgPathMap,
                                      Map<String, Integer> imgWidthMap) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] data = IOUtil.toByteArray(inputStream);
            String filePath = tempDirPath + key;

            // 转换不支持的图片格式
            if (FileUtil.isUnsupportedImageFormat(key)) {
                filePath = FileUtil.convertImgToJPG(data, filePath);
            } else {
                FileUtils.writeByteArrayToFile(new File(filePath), data);
            }

            if (StringUtils.isNotBlank(filePath)) {
                BufferedImage originalImage = ImageIO.read(new File(filePath));
                imgWidthMap.put(key, originalImage.getWidth());
                imgPathMap.put(key, "file:///" + filePath.replace("\\", "/"));
            }
        }
    }

    /**
     * 保存书籍元数据
     */
    private void saveBookMetadata(Book book) {
        BookInfo bookInfo = new BookInfo();
        String bookName = ListUtil.listToString(book.getMetadata().getTitles());
        bookInfo.setBookName(bookName);

        List<String> descriptions = book.getMetadata().getDescriptions();
        String bookDesc = descriptions != null && !descriptions.isEmpty() ?
                ListUtil.listToString(descriptions) : bookName;
        bookInfo.setBookDesc(bookDesc);

        String author = ListUtil.listToString(book.getMetadata().getAuthors());
        bookInfo.setBookAuthor(author);

        cacheService.setSelectedBookInfo(bookInfo);
    }

    // ==================== 自动阅读功能 ====================

    /**
     * 自动阅读下一行功能
     * 如果已经在运行则停止，否则启动自动阅读
     */
    public void autoReadNextLine() {
        // 如果已经在运行则停止
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            return;
        }

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            return;
        }

        AtomicReference<List<String>> chapterContentList =
                new AtomicReference<>(selectedChapterInfo.getChapterContentList());
        if (chapterContentList.get() == null || chapterContentList.get().isEmpty()) {
            return;
        }

        // 初始化执行器
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }

        // 获取自动阅读时间间隔
        float autoReadTime = settings.getAutoReadTime();
        if (autoReadTime <= 0f) {
            autoReadTime = 5f;
        }

        // 创建自动阅读任务
        Runnable readNextLineTask = createAutoReadTask(selectedChapterInfo);
        long autoReadTimeMillis = (long) (autoReadTime * 1000);

        // 调度任务
        executorService.scheduleAtFixedRate(readNextLineTask,
                autoReadTimeMillis,
                autoReadTimeMillis,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 创建自动阅读任务
     */
    private Runnable createAutoReadTask(ChapterInfo selectedChapterInfo) {
        return () -> {
            int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();
            int contentLength = selectedChapterInfo.getChapterContentList() == null ?
                    0 : selectedChapterInfo.getChapterContentList().size();

            if (lastReadLineNum < contentLength) {
                IS_SWITCH_NEXT_CHAPTER_SUCCESS = false;
                WReaderStatusBarWidget.nextLine(project);
            } else {
                if (!IS_SWITCH_NEXT_CHAPTER_SUCCESS) {
                    stopTTS();
                    cacheService.setEditorMessageVerticalScrollValue(0);
                    WReaderStatusBarWidget.nextChapter(project);
                }
            }
        };
    }

    /**
     * 停止定时器
     */
    public void executorServiceShutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // ==================== TTS语音功能 ====================

    /**
     * 小说内容文本转语音
     *
     * @throws Exception TTS处理异常
     */
    public void ttsChapterContent() throws Exception {
        String chapterContent = currentChapterInfo.getChapterContentStr();
        if (StringUtils.isBlank(chapterContent)) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_BOOK_CONTENT_ERROR,
                    MessageDialogUtil.TITLE_INFO);
            return;
        }

        // 如果已经在播放则停止
        if (edgeTTS != null) {
            edgeTTS.dispose();
            edgeTTS = null;
            return;
        }

        // 初始化TTS服务
        initializeTTSService();

        // 设置TTS参数
        configureTTSParameters();

        // 开始语音合成
        edgeTTS.synthesize(chapterContent);
        edgeTTS.start();
    }

    /**
     * 初始化TTS服务
     */
    private void initializeTTSService() {
        try {
            edgeTTS = new EdgeTTS();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize TTS service", e);
        }
    }

    /**
     * 配置TTS参数
     */
    private void configureTTSParameters() {
        // 音色设置
        String voiceRole = settings.getVoiceRole();
        if (StringUtils.isBlank(voiceRole)) {
            voiceRole = configYaml.getSettings().getVoiceRole();
        }
        VoiceRole voiceRoleEnum = VoiceRole.getByNickName(voiceRole);

        // 音频超时时间
        int audioTimeout = settings.getAudioTimeout();
        if (audioTimeout <= 0) {
            audioTimeout = configYaml.getSettings().getAudioTimeout();
        }

        // 语速
        Float rate = settings.getRate();
        if (rate == null || rate <= 0) {
            rate = configYaml.getSettings().getRate();
        }

        // 音量
        Integer volume = settings.getVolume();
        if (volume == null || volume < 0) {
            volume = configYaml.getSettings().getVolume();
        }

        // 风格
        String audioStyle = settings.getAudioStyle();
        audioStyle = StringUtils.isBlank(audioStyle) ?
                configYaml.getSettings().getAudioStyle() : audioStyle;

        // 应用配置
        edgeTTS.setVoiceRole(voiceRoleEnum)
                .setStyleName(audioStyle)
                .setRate(rate.toString())
                .setVolume(volume.toString());
    }

    /**
     * 停止语音播放
     */
    public void stopTTS() {
        if (edgeTTS != null) {
            edgeTTS.dispose();
            edgeTTS = null;
        }
    }

    // ==================== 字体和样式管理 ====================

    /**
     * 字体缩小
     */
    public void fontSizeSub() {
        fontFamily = cacheService.getFontFamily();
        if (fontSize == 0) {
            fontSize = cacheService.getFontSize();
        }
        if (fontSize <= 1) {
            return;
        }

        fontSize--;
        cacheService.setFontSize(fontSize);
    }

    /**
     * 字体放大
     */
    public void fontSizeAdd() {
        fontFamily = cacheService.getFontFamily();
        if (fontSize == 0) {
            fontSize = cacheService.getFontSize();
        }
        fontSize++;
        cacheService.setFontSize(fontSize);
    }

    /**
     * 改变字体颜色
     */
    public void changeFontColor() {
        fontColorHex = cacheService.getFontColorHex();
        Color currentFontColor = Color.decode(fontColorHex);
        Color color = JColorChooser.showDialog(null, "选择颜色", currentFontColor);

        if (color != null) {
            fontColorHex = String.format("#%02x%02x%02x",
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue());
            cacheService.setFontColorHex(fontColorHex);
        }
    }

    /**
     * 更新内容显示
     */
    public void updateContentText() {
        try {
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    updateSidebarContent();
                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    updateStatusBarContent();
                    break;
                case Settings.DISPLAY_TYPE_TERMINAL:
                    // 终端显示暂未实现
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新侧边栏内容
     */
    private void updateSidebarContent() {
        ChapterInfo selectedChapterInfoTemp = cacheService.getSelectedChapterInfo();
        selectedChapterInfoTemp.initLineNum(1, 1, 1);
        cacheService.setSelectedChapterInfo(selectedChapterInfoTemp);

        ToolWindowUtil.updateContentText(project, textPane -> {
            SiteBean siteBean = cacheService.getSelectedSiteBean();
            ChapterRules chapterRules = siteBean.getChapterRules();

            String chapterContent = cacheService.getSelectedChapterInfo().getChapterContent();
            boolean isContentOriginalStyle = chapterRules.isUseContentOriginalStyle();

            String styledContent = isContentOriginalStyle ?
                    buildOriginalStyleContent(chapterContent) :
                    buildCustomStyleContent(chapterContent);

            textPane.setText(styledContent);
            textPane.setCaretPosition(0);
        });
    }

    /**
     * 构建原始样式内容
     */
    private String buildOriginalStyleContent(String chapterContent) {
        chapterContent = String.format("""
                <div class="%s" style="color:%s;font-size:%dpx;">%s</div>
                """, ConstUtil.NEW_FONT_CLASS_NAME, fontColorHex, fontSize, chapterContent);

        return StringUtil.buildFullHtml(selectBookInfo.getBookName(),
                contentOriginalStyle, chapterContent);
    }

    /**
     * 构建自定义样式内容
     */
    private String buildCustomStyleContent(String chapterContent) {
        return String.format("""
                <div style="color:%s;font-family:'%s';font-size:%dpx;">%s</div>
                """, fontColorHex, fontFamily, fontSize, chapterContent);
    }

    /**
     * 更新状态栏内容
     */
    private void updateStatusBarContent() {
        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        selectedChapterInfo.initLineNum(1, 1, 1);
        WReaderStatusBarWidget.update(project, "");
    }

    // ==================== 内容处理辅助方法 ====================

    /**
     * 分割章节内容
     */
    public void splitChapterContent() {
        String chapterContentStr = currentChapterInfo.getChapterContentStr();
        List<String> chapterContentSplitList = currentChapterInfo.getChapterContentList();
        int singleLineChars = settings.getSingleLineChars();

        if (chapterContentSplitList == null || chapterContentSplitList.isEmpty()) {
            chapterContentSplitList = StringUtil.splitStringByMaxCharList(
                    chapterContentStr, singleLineChars);
        }
        currentChapterInfo.setChapterContentList(chapterContentSplitList);
    }

    /**
     * 请求内容（用于下一页加载）
     */
    private String requestContent(String url, Consumer<String> call) {
        String content = "";
        ChapterRules chapterRules = selectedSiteBean.getChapterRules();

        try {
            if (chapterRules.isUseNextContentApi()) {
                content = requestContentViaApi(url, call, chapterRules);
            } else {
                content = requestContentViaHtml(url, call, chapterRules);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return formatAndApplyRegex(content, chapterRules);
    }

    /**
     * 通过API请求内容
     */
    private String requestContentViaApi(String url, Consumer<String> call, ChapterRules rules) {
        HttpRequestBase requestBase = HttpUtil.commonRequest(url);
        requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);
                JsonObject resJson = new Gson().fromJson(result, JsonObject.class);

                Object readJson = JsonPath.read(resJson.toString(), rules.getNextContentApiDataRule());
                call.accept(resJson.toString());
                return (String) readJson;
            }
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }
        return "";
    }

    /**
     * 通过HTML请求内容
     */
    private String requestContentViaHtml(String url, Consumer<String> call, ChapterRules rules) {
        try {
            Document document = Jsoup.connect(url)
                    .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                    .get();

            Element bodyElement = document.body();
            call.accept(bodyElement.html());

            Elements chapterContentElements = bodyElement.select(rules.getContentElementName());
            if (chapterContentElements.isEmpty()) {
                return "";
            }

            StringBuilder contentHtml = new StringBuilder();
            for (Element element : chapterContentElements) {
                Tag tag = element.tag();
                String html = element.html();
                if (!tag.isEmpty() && StringUtils.trimToNull(html) != null) {
                    contentHtml.append(String.format("<%s>%s</%s>",
                            tag.normalName(), html, tag.normalName()));
                }
            }
            return contentHtml.toString();

        } catch (IOException e) {
            Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }
    }

    /**
     * 格式化内容并应用正则规则
     */
    private String formatAndApplyRegex(String content, ChapterRules rules) {
        String formattedContent = content.replaceAll("\\n", "<br/>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");

        List<String> contentRegexList = rules.getContentRegexList();
        if (ListUtil.isNotEmpty(contentRegexList)) {
            for (String contentRegex : contentRegexList) {
                String[] regulars = contentRegex.split(ConstUtil.SPLIT_REGEX_REPLACE_FLAG);
                String regex = regulars[0];
                String replacement = regulars.length > 1 ? regulars[1] : "";
                formattedContent = formattedContent.replaceAll(regex, replacement);
            }
        }
        return formattedContent;
    }

    /**
     * 获取带样式的内容
     */
    private static String getContent(String text) {
        CacheService cacheService = CacheService.getInstance();
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();

        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";

        return "<div style=\"" + style + "\">" + text + "</div>";
    }

    // ==================== 鼠标事件处理方法 ====================

    /**
     * 获取鼠标点击的文档位置
     *
     * @param contentTextPane 文本面板
     * @param e               鼠标事件
     * @return 点击位置
     */
    public int getClickedPosition(JTextPane contentTextPane, MouseEvent e) {
        Point p = e.getPoint();
        return contentTextPane.viewToModel2D(p);
    }

    /**
     * 获取指定位置的HTML标签
     *
     * @param textPane 文本面板
     * @param pos      位置
     * @return HTML标签字符串
     */
    public String getHTMLTagAtPosition(JTextPane textPane, int pos) {
        javax.swing.text.Document doc = textPane.getDocument();
        if (!(doc instanceof HTMLDocument htmlDoc)) {
            return null;
        }

        javax.swing.text.Element element = htmlDoc.getCharacterElement(pos);
        List<javax.swing.text.Element> elements = collectElements(element);
        Collections.reverse(elements);

        return buildHTMLElementString(elements);
    }

    /**
     * 收集包含指定位置的所有元素
     */
    private static List<javax.swing.text.Element> collectElements(javax.swing.text.Element element) {
        List<javax.swing.text.Element> elements = new ArrayList<>();
        javax.swing.text.Element current = element;

        while (current != null && current.getName() != null) {
            elements.add(current);
            current = current.getParentElement();
        }
        return elements;
    }

    /**
     * 构建HTML元素字符串
     */
    private static String buildHTMLElementString(List<javax.swing.text.Element> elements) {
        StringBuilder sb = new StringBuilder();
        for (javax.swing.text.Element e : elements) {
            AttributeSet attrs = e.getAttributes();
            HTML.Tag tag = (HTML.Tag) attrs.getAttribute(StyleConstants.NameAttribute);
            if (tag != null) {
                sb.append("<").append(tag);
                appendElementAttributes(attrs, sb);
                sb.append(">");
            }
        }
        return sb.toString();
    }

    /**
     * 添加元素属性
     */
    private static void appendElementAttributes(AttributeSet attrs, StringBuilder sb) {
        Enumeration<?> names = attrs.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            if (name instanceof HTML.Attribute attr) {
                Object value = attrs.getAttribute(attr);
                if (value != null) {
                    sb.append(" ").append(attr).append("=\"").append(value).append("\"");
                }
            }
        }
    }

    // ==================== 缓存处理方法 ====================

    /**
     * 处理缓存数据
     */
    private void handleCache() {
        cacheService.setSelectedSiteBean(selectedSiteBean);
        cacheService.setSelectedBookSiteIndex(selectedBookSiteIndex);
        cacheService.setSelectedBookInfo(selectBookInfo);
        cacheService.setChapterList(chapterList);
        cacheService.setChapterUrlList(chapterUrlList);
        cacheService.setSelectedChapterInfo(currentChapterInfo);
        customSiteRuleCacheServer.setSelectedCustomSiteRuleKey(selectSiteGroupName);
    }

    /**
     * 取消操作时的通用处理
     */
    private void commCancelOperationHandle() {
        initData();
    }
}