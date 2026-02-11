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
import com.wei.wreader.utils.comm.DynamicCodeExecutor;
import com.wei.wreader.utils.comm.MethodExecutor;
import com.wei.wreader.utils.comm.StringTemplateEngine;
import com.wei.wreader.utils.comm.UrlUtil;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作工具类
 *
 * @author weizhanjie
 */
public class OperateActionUtil {
    //region 属性参数
    private static ConfigYaml configYaml;
    private static CacheService cacheService;
    private static CustomSiteRuleCacheServer customSiteRuleCacheServer;
    private CustomSiteUtil customSiteUtil;
    private Settings settings;
    private static Project mProject;
    private static OperateActionUtil instance;
    private static ScheduledExecutorService executorService;
    /**
     * 是否已成功切换下一个章节
     */
    private static boolean IS_SWITCH_NEXT_CHAPTER_SUCCESS = false;
    /**
     * 书本名称列表
     */
    private List<String> bookNameList = new ArrayList<>();
    /**
     * 书本信息列表
     */
    private List<BookInfo> bookInfoList = new ArrayList<>();
    /**
     * 章节名称列表
     */
    private List<String> chapterList = new ArrayList<>();
    /**
     * 章节链接列表
     */
    private List<String> chapterUrlList = new ArrayList<>();
    /**
     * 章节内容列表
     */
    private List<String> chapterContentList = new ArrayList<>();
    /**
     * 当前章节索引
     */
    private int currentChapterIndex = 0;
    /**
     * font fontFamily
     */
    private String fontFamily = ConstUtil.DEFAULT_FONT_FAMILY;
    /**
     * font size
     */
    private int fontSize = ConstUtil.DEFAULT_FONT_SIZE;
    /**
     * font color
     */
    private String fontColorHex = ConstUtil.DEFAULT_FONT_COLOR_HEX;
    /**
     * 当前章节内容--Html
     */
    private String chapterContentHtml = "";
    /**
     * 当前章节内容--纯文字
     */
    private String chapterContentText = "";
    /**
     * 内容页面原始样式
     */
    private String contentOriginalStyle = "";
    /**
     * 基础网址
     */
    private static String baseUrl;
    /**
     * 选中的站点信息下标(默认第一个)
     */
    private static int selectedBookSiteIndex = 0;
    // TODO: 新版配置文件测试--START
    /**
     * 站点信息列表(new)
     */
    private List<SiteBean> siteBeanList;
    /**
     * 选中的站点分组名称
     */
    private static String selectSiteGroupName;
    /**
     * 选中的站点信息(默认第一个)
     */
    private static SiteBean selectedSiteBean;
    /**
     * 选中的站点信息中的搜索规则
     */
    private static SearchRules selectedSearchRules;
    /**
     * 选中的站点信息中的目录规则
     */
    private static ListMainRules selectedListMainRules;
    /**
     * 选中的站点信息中的章节规则
     */
    private static ChapterRules selectedChapterRules;
    /**
     * 选中的站点信息中的书籍信息规则
     */
    private static BookInfoRules selectedBookInfoRules;
    /**
     * 选中的站点信息--临时缓存搜索前的站点信息
     */
    private static SiteBean tempSelectedSiteBean;
    // TODO: 新版配置文件测试--END

    /**
     * 当前小说信息
     */
    private BookInfo selectBookInfo = new BookInfo();
    /**
     * 当前章节信息
     */
    private ChapterInfo currentChapterInfo = new ChapterInfo();
    /**
     * 搜索小说对话框
     */
    private DialogBuilder searchBookDialogBuilder;


    //endregion

    /**
     * 单例
     *
     * @return
     */
    public static OperateActionUtil getInstance(Project project) {
        if (instance == null || !project.equals(mProject) || mProject.isDisposed()) {
            instance = new OperateActionUtil(project);
        }
        instance.initData();
        return instance;
    }

    public OperateActionUtil(Project project) {
        configYaml = new ConfigYaml();
        cacheService = CacheService.getInstance();
        customSiteUtil = CustomSiteUtil.getInstance(project);
        customSiteRuleCacheServer = CustomSiteRuleCacheServer.getInstance();
        mProject = project;
    }

    /**
     * 初始化数据
     */
    private void initData() {
        try {
            // settings
            settings = cacheService.getSettings();
            if (settings == null) {
                settings = configYaml.getSettings();
            }
            if (StringUtils.isBlank(settings.getCharset())) {
                settings.setCharset(configYaml.getSettings().getCharset());
            }

            // 加载字体信息
            fontFamily = cacheService.getFontFamily();
            if (fontFamily == null || fontFamily.isEmpty() || "JetBrains Mono".equals(fontFamily)) {
                fontFamily = ConstUtil.DEFAULT_FONT_FAMILY;
                cacheService.setFontFamily(fontFamily);
            }
            fontSize = cacheService.getFontSize();
            if (fontSize == 0) {
                fontSize = ConstUtil.DEFAULT_FONT_SIZE;
                cacheService.setFontSize(fontSize);
            }
            fontColorHex = cacheService.getFontColorHex();
            if (fontColorHex == null || fontColorHex.isEmpty()) {
                // 获取主题色
                EditorColorsScheme schemeForCurrentUITheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
                // 获取主题前景色
                Color defaultForeground = schemeForCurrentUITheme.getDefaultForeground();
                fontColorHex = String.format("#%02x%02x%02x", defaultForeground.getRed(), defaultForeground.getGreen(),
                        defaultForeground.getBlue());
                cacheService.setFontColorHex(fontColorHex);
            }

            String selectedCustomSiteRuleKey = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();
            if (StringUtils.isBlank(selectedCustomSiteRuleKey) ||
                    ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(selectedCustomSiteRuleKey)) {
                // 站点列表信息
                siteBeanList = configYaml.getSiteList();
            } else {
                // 站点列表信息
                Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
                siteBeanList = siteMap.get(selectedCustomSiteRuleKey);
            }

            // 加载持久化数据--站点信息
            Integer selectedBookSiteIndexTemp = cacheService.getSelectedBookSiteIndex();
            if (selectedBookSiteIndexTemp == null) {
                selectedBookSiteIndex = 0;
                cacheService.setSelectedBookSiteIndex(0);
            } else {
                selectedBookSiteIndex = selectedBookSiteIndexTemp;
            }

            if (selectedBookSiteIndex > siteBeanList.size()) {
                selectedBookSiteIndex = 0;
                cacheService.setSelectedBookSiteIndex(0);
            }
            selectedSiteBean = siteBeanList.get(selectedBookSiteIndex);
            if (selectedSiteBean == null) {
                selectedSiteBean = siteBeanList.get(selectedBookSiteIndex);
                cacheService.setSelectedSiteBean(selectedSiteBean);
            }
            selectedSearchRules = selectedSiteBean.getSearchRules();
            selectedListMainRules = selectedSiteBean.getListMainRules();
            selectedChapterRules = selectedSiteBean.getChapterRules();
            selectedBookInfoRules = selectedSiteBean.getBookInfoRules();

            // 加载持久化数据--小说信息
            selectBookInfo = cacheService.getSelectedBookInfo();
            // 加载持久化数据--章节信息
            currentChapterInfo = cacheService.getSelectedChapterInfo();
            if (currentChapterInfo == null) {
                currentChapterInfo = new ChapterInfo();
            }
            currentChapterIndex = currentChapterInfo.getSelectedChapterIndex();
            // 加载持久化数据--目录名称列表
            chapterList = cacheService.getChapterList();
            // 加载持久化数据--目录章节链接列表
            chapterUrlList = cacheService.getChapterUrlList();

            // 选择的站点基础网址
            baseUrl = selectedSiteBean.getBaseUrl();
            chapterContentHtml = currentChapterInfo.getChapterContent();
            chapterContentText = currentChapterInfo.getChapterContentStr();
            if (chapterContentHtml == null || chapterContentHtml.isEmpty()) {
                chapterContentHtml = "<pre>" + ConstUtil.WREADER_TOOL_WINDOW_CONTENT_INIT_TEXT + "</pre>";
            } else {
                updateContentText();
            }
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_INIT_ERROR, "Error");
            e.printStackTrace();
        }
    }

    /**
     * 显示当前小说目录
     */
    public void showBookDirectory(BookDirectoryListener listener) {
        SwingUtilities.invokeLater(() -> {
            // 获取数据加载模式
            int dataLoadType = settings.getDataLoadType();
            // 判断数据加载模式是否为本地加载
            if (dataLoadType == Settings.DATA_LOAD_TYPE_LOCAL) {
                // 当数据加载模式为本地加载时，判断缓存的章节列表是否为空，为空则跳出方法
                if (ListUtil.isEmpty(cacheService.getChapterContentList())) {
                    Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CHAPTER_LIST_ERROR, "提示");
                    return;
                }
            }

            if (ListUtil.isEmpty(chapterList)) {
                Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CHAPTER_LIST_ERROR, "提示");
                return;
            }

            // 构建目录列表组件
            JBList<String> chapterListJBList = new JBList<>(chapterList);
            chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            chapterListJBList.setBorder(JBUI.Borders.empty());

            JBScrollPane jScrollPane = new JBScrollPane(chapterListJBList);
            chapterListJBList.setSelectedIndex(currentChapterIndex);
            chapterListJBList.ensureIndexIsVisible(currentChapterIndex);
            jScrollPane.setPreferredSize(new Dimension(400, 500));
            chapterListJBList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    switch (dataLoadType) {
                        case Settings.DATA_LOAD_TYPE_NETWORK:
                            loadBookDirectoryRemote(chapterListJBList, listener);
                            break;
                        case Settings.DATA_LOAD_TYPE_LOCAL:
                            loadBookDirectoryLocal(chapterListJBList, listener);
                            break;
                    }
                }
            });
            MessageDialogUtil.showMessage(mProject, "目录", jScrollPane);

        });
    }

    /**
     * 更新内容
     */
    public void updateContentText() {
        try {
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    // 清空缓存
                    ChapterInfo selectedChapterInfoTemp = cacheService.getSelectedChapterInfo();
                    selectedChapterInfoTemp.initLineNum(1, 1, 1);
                    cacheService.setSelectedChapterInfo(selectedChapterInfoTemp);

                    // 获取工具窗口
                    ToolWindowUtil.updateContentText(mProject, (textPane) -> {
                        // 设置内容
                        String fontColorHex = cacheService.getFontColorHex();
                        String fontFamily = cacheService.getFontFamily();
                        int fontSize = cacheService.getFontSize();
                        String chapterContent = cacheService.getSelectedChapterInfo().getChapterContent();
                        // 是否使用原网页的css样式
                        boolean isContentOriginalStyle = selectedChapterRules.isUseContentOriginalStyle();
                        if (isContentOriginalStyle) {
                            // 设置内容
                            chapterContent = String.format("""
                                            <div class="%s" style="color:%s;font-size:%dpx;">%s</div>
                                            """,
                                    ConstUtil.NEW_FONT_CLASS_NAME, fontColorHex, fontSize, chapterContent);

                            // 构建完整html结构
                            chapterContent = StringUtil.buildFullHtml(selectBookInfo.getBookName(), contentOriginalStyle,
                                    chapterContent);
                        } else {
                            // 设置内容
                            chapterContent = String.format("""
                                            <div style="color:%s;font-family:'%s';font-size:%dpx;">%s</div>
                                            """,
                                    fontColorHex, fontFamily, fontSize, chapterContent);
                        }

                        textPane.setText(chapterContent);
                        // 设置光标位置
                        textPane.setCaretPosition(0);
                    });

                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
                    selectedChapterInfo.initLineNum(1, 1, 1);
                    WReaderStatusBarWidget.update(mProject, "");
                    break;
                case Settings.DISPLAY_TYPE_TERMINAL:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Task.Backgroundable nextContentTask;
    /**
     * 加载本章节下一页的内容
     *
     * @param bodyElement 文章内容html页面{@code <body></body>}部分的元素
     */
    public void loadThisChapterNextContent(String chapterUrl, Element bodyElement) {
        loadThisChapterNextContent(chapterUrl, bodyElement.html());
    }
    /**
     * 加载本章节下一页的内容
     *
     * @param bodyElementStr 文章内容html页面{@code <body></body>}部分的元素
     */
    public void loadThisChapterNextContent(String chapterUrl, String bodyElementStr) {
        if (settings.getDisplayType() != Settings.DATA_LOAD_TYPE_NETWORK) {
            return;
        }

        ChapterRules chapterRules = selectedSiteBean.getChapterRules();
        if (chapterRules == null) {
            return;
        }

        String nextContentUrl = chapterRules.getNextContentUrl();
        if (StringUtils.isEmpty(nextContentUrl)) {
            return;
        }

        boolean isCodeConfig = nextContentUrl.startsWith(ConstUtil.JAVA_CODE_CONFIG_START_LABEL) &&
                nextContentUrl.endsWith(ConstUtil.JAVA_CODE_CONFIG_END_LABEL);
        if (!isCodeConfig) {
            return;
        }

        if (nextContentTask != null) {
            nextContentTask.onCancel();
        }

        nextContentTask = new Task.Backgroundable(mProject, "【W-Reader】加载本章节下一页内容") {

            private volatile boolean isRunning = true;
            private String returnResult = nextContentUrl;

            final StringBuilder nextContent = new StringBuilder();

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                try {
                    String baseUrl = selectedSiteBean.getBaseUrl();
                    String preContentUrlTemp = "";
                    AtomicReference<String> prePageContent = new AtomicReference<>(bodyElementStr);
                    int pageCount = 0;
                    while (isRunning && StringUtils.isNotEmpty(returnResult)) {
                        // 检查用户是否取消了操作
                        progressIndicator.checkCanceled();

                        // 更新进度信息
                        pageCount += 1;
                        progressIndicator.setText2("正在加载第 " + pageCount + " 页...");

                        // 执行动态代码
                        returnResult = (String) DynamicCodeExecutor.executeMethod(
                                nextContentUrl,
                                "execute",
                                new Class[]{String.class, int.class, String.class, String.class},
                                new Object[]{chapterUrl, pageCount, preContentUrlTemp, prePageContent.get(),}
                        );

                        if (StringUtils.isBlank(returnResult)) {
                            isRunning = false;
                            break;
                        }

                        returnResult = UrlUtil.buildFullURL(baseUrl, returnResult);
                        preContentUrlTemp = returnResult;
                        // 在 UI 线程中插入内容
                        nextContent.append(requestContent(returnResult, prePageContent::set));

                        // 添加小延迟，避免过于频繁的请求
                        Thread.sleep(1000);
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

            @Override
            public void onCancel() {
                isRunning = false;

                super.onCancel();
            }

            @Override
            public void onSuccess() {
                ToolWindowUtil.updateContentText(mProject, contentTextPanel -> {
                    ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();

                    String text = nextContent.toString();
                    int caretPosition = contentTextPanel.getCaretPosition();
                    text = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" + fontColorHex + ";\">" +
                            selectedChapterInfo.getChapterTitle() + "</h3>" + text;
                    String newText = getContent(text);
                    contentTextPanel.setText(newText);
                    contentTextPanel.setCaretPosition(caretPosition);

                    SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
                    ChapterRules chapterRules = selectedSiteBean.getChapterRules();
                    // 剔除
                    Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
                    String chapterContentText = pattern.matcher(text).replaceAll("　");
                    chapterContentText = StringUtils.normalizeSpace(chapterContentText);
                    chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);
                    // 将换行符和制表符替换成html对应代码
                    chapterContentText = chapterContentText.replaceAll("\\n", "<br/>")
                            .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
                    // 章节内容处理规则
                    List<String> contentRegexList = chapterRules.getContentRegexList();
                    if (ListUtil.isNotEmpty(contentRegexList)) {
                        for (String contentRegex : contentRegexList) {
                            String[] regulars = contentRegex.split(ConstUtil.SPLIT_REGEX_REPLACE_FLAG);
                            String regex = regulars[0];
                            String replacement = regulars.length > 1 ? regulars[1] : "";
                            chapterContentText = chapterContentText.replaceAll(regex, replacement);
                        }
                    }

                    selectedChapterInfo.setChapterContent(text);
                    selectedChapterInfo.setChapterContentStr(chapterContentText);
                    cacheService.setSelectedChapterInfo(selectedChapterInfo);
                });
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                if (!(error instanceof ProcessCanceledException)) {
                    super.onThrowable(error);
                }
            }
        };
        nextContentTask.queue();
    }

    /**
     * 远程加载小说目录
     *
     * @param chapterListJBList
     * @param listener
     */
    public void loadBookDirectoryRemote(JBList<String> chapterListJBList, BookDirectoryListener listener) {
        // 获取选中的章节索引
        int selectedIndex = chapterListJBList.getSelectedIndex();
        currentChapterIndex = selectedIndex;
        // 提取章节名称和章节链接
        String chapterTitle = chapterList.get(currentChapterIndex);
        // 提取章节链接
        if (ListUtil.isEmpty(chapterUrlList)) {
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
            return;
        }
        String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
        String chapterUrl = chapterSuffixUrl;
        if (!chapterSuffixUrl.startsWith(ConstUtil.HTTP_SCHEME) &&
                !chapterSuffixUrl.startsWith(ConstUtil.HTTPS_SCHEME) &&
                !JsonUtil.isValid(selectBookInfo.getBookUrl())) {
            chapterUrl = baseUrl + chapterSuffixUrl;
        }
        currentChapterInfo.setChapterTitle(chapterTitle);
        currentChapterInfo.setChapterUrl(chapterUrl);
        // 搜索章节内容
        searchBookContentRemote(chapterUrl, (searchBookCallParam) -> {
            // 初始化并缓存当前章节信息
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
     * @param chapterListJBList
     * @param listener
     */
    public void loadBookDirectoryLocal(JBList<String> chapterListJBList, BookDirectoryListener listener) {
        // 获取选中的章节索引
        int selectedIndex = chapterListJBList.getSelectedIndex();
        currentChapterIndex = selectedIndex;
        chapterContentList = cacheService.getChapterContentList();
        if (chapterContentList != null && !chapterContentList.isEmpty()) {
            // 提取章节名称和章节链接
            String chapterTitle = chapterList.get(currentChapterIndex);
            currentChapterInfo.setChapterTitle(chapterTitle);
            // 提取章节内容
            chapterContentHtml = chapterContentList.get(currentChapterIndex);
            Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
            chapterContentText = pattern.matcher(chapterContentHtml).replaceAll("　");
            chapterContentText = StringUtils.normalizeSpace(chapterContentText);
            chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);

            // 初始化并缓存当前章节信息
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);
            if (listener != null) {
                listener.onClickItem(selectedIndex, chapterList, currentChapterInfo, null);
            }
        }
    }

    /**
     * 远程获取小说内容
     *
     * @param url
     * @param call 获取成功后执行的方法
     * @throws IOException
     */
    public void searchBookContentRemote(String url, Consumer<SearchBookCallParam> call) {
        new Task.Backgroundable(mProject, "【W-Reader】正在获取内容...") {
            String chapterContent = "";

            Element bodyElement;
            String bodyElementStr;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // 在后台线程执行耗时操作
                progressIndicator.setText("【W-Reader】正在获取内容...");
                // 设置进度条为不确定模式
                progressIndicator.setIndeterminate(true);

                String chapterContentUrl = selectedChapterRules.getUrl();
                String chapterContentUrlDataRule = selectedChapterRules.getUrlDataRule();
                if (StringUtils.isNotBlank(chapterContentUrl) && StringUtils.isNotBlank(chapterContentUrlDataRule)) {
                    HttpRequestBase requestBase = HttpUtil.commonRequest(url);
                    requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
                    try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
                        if (httpResponse.getStatusLine().getStatusCode() == 200) {
                            HttpEntity entity = httpResponse.getEntity();
                            String result = EntityUtils.toString(entity);
                            Gson gson = new Gson();
                            JsonObject memuListJson = gson.fromJson(result, JsonObject.class);

                            // 使用jsonpath获取内容
                            Object readJson = JsonPath.read(memuListJson.toString(), chapterContentUrlDataRule);

                            chapterContent = readJson.toString();
                            bodyElementStr = result;
                        }
                    } catch (Exception e) {
                        Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                        throw new RuntimeException(e);
                    }
                } else {
                    Document document = null;
                    try {
                        document = Jsoup.connect(url)
                                .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                                .get();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR + "\n" + e.getMessage(), "提示");
                    }
                    // 头部
                    Element headElement = document.head();
                    // 获取页面原样式
                    boolean isContentOriginalStyle = selectedChapterRules.isUseContentOriginalStyle();
                    if (isContentOriginalStyle) {
                        // 获取页面<style></style>中的CSS样式
                        StringBuilder allStyle = new StringBuilder();
                        Elements styles = headElement.getElementsByTag("style");
                        for (Element style : styles) {
                            String styleText = style.html();
                            String replacement = selectedChapterRules.getReplaceContentOriginalRegex();
                            styleText = styleText.replaceAll(replacement, ConstUtil.NEW_FONT_CLASS_CSS_NAME);
                            // 去除样式中的HTML标签
                            styleText = styleText.replaceAll(ConstUtil.HTML_TAG_REGEX_STR, "");
                            allStyle.append(styleText);
                        }

                        contentOriginalStyle = "<style>" + allStyle + "</style>";
                    }

                    // 页面展示主体
                    bodyElement = document.body();
                    // 获取小说内容
                    // 小说内容的HTML标签类型（class, id）
                    String chapterContentElementName = selectedChapterRules.getContentElementName();
                    Elements chapterContentElements = bodyElement.select(chapterContentElementName);
                    if (chapterContentElements.isEmpty()) {
                        Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_BOOK_CONTENT_ERROR, "提示");
                        return;
                    }

                    StringBuilder contentHtml = new StringBuilder();
                    for (Element element : chapterContentElements) {
                        Tag tag = element.tag();
                        String html = element.html();
                        if (tag.isEmpty() || StringUtils.trimToNull(html) == null) {
                            continue;
                        }

                        contentHtml.append(String.format("<%s>%s</%s>", tag.normalName(), html, tag.normalName()));
                    }

                    chapterContent = contentHtml.toString();
                    chapterContentText = chapterContentElements.text();
                    bodyElementStr = bodyElement.html();
                }

                progressIndicator.setFraction(1.0);
            }

            @Override
            public void onSuccess() {
                // 处理内容
                try {
                    chapterContent = handleContent(chapterContent);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                chapterContent = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" + fontColorHex + ";\">" +
                        currentChapterInfo.getChapterTitle() + "</h3>" + chapterContent;
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
                call.accept(searchBookCallParam);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                error.printStackTrace();
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            }
        }.queue();
    }

    /**
     * 上一个章节
     */
    public void prevPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        try {
            // 判断是否是第一章
            if (currentChapterIndex <= 0) {
                return;
            }

            // 获取上一章的索引
            currentChapterIndex = currentChapterIndex - 1;
            String chapterTitle = chapterList.get(currentChapterIndex);
            currentChapterInfo.setChapterTitle(chapterTitle);

            int dataLoadType = settings.getDataLoadType();
            if (dataLoadType == Settings.DATA_LOAD_TYPE_NETWORK) {
                if (ListUtil.isEmpty(chapterUrlList)) {
                    Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
                    return;
                }

                // 获取上一章的链接
                String prevChapterSuffixUrl = chapterUrlList.get(currentChapterIndex);
                String prevChapterUrl = prevChapterSuffixUrl;
                if (!prevChapterSuffixUrl.startsWith(ConstUtil.HTTP_SCHEME) &&
                        !prevChapterSuffixUrl.startsWith(ConstUtil.HTTPS_SCHEME) &&
                        !JsonUtil.isValid(selectBookInfo.getBookUrl())) {
                    prevChapterUrl = baseUrl + prevChapterSuffixUrl;
                }
                currentChapterInfo.setChapterUrl(prevChapterUrl);
                // 远程搜索上一章内容
                searchBookContentRemote(prevChapterUrl, (searchBookCallParam) -> {
                    // 初始化并缓存章节信息
                    currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
                    cacheService.setSelectedChapterInfo(currentChapterInfo);

                    runnable.accept(currentChapterInfo, searchBookCallParam.getBodyElement());
                });
            } else if (dataLoadType == Settings.DATA_LOAD_TYPE_LOCAL) {
                chapterContentList = cacheService.getChapterContentList();
                if (chapterContentList != null && !chapterContentList.isEmpty()) {
                    // 提取章节内容
                    chapterContentHtml = chapterContentList.get(currentChapterIndex);
                    Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
                    chapterContentText = pattern.matcher(chapterContentHtml).replaceAll("　");
                    chapterContentText = StringUtils.normalizeSpace(chapterContentText);
                    chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);

                    // 初始化并缓存章节信息
                    currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
                    cacheService.setSelectedChapterInfo(currentChapterInfo);
                }

                runnable.accept(currentChapterInfo, null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下一个章节
     */
    public void nextPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        try {
            IS_SWITCH_NEXT_CHAPTER_SUCCESS = false;
            int dataLoadType = settings.getDataLoadType();
            if (dataLoadType == Settings.DATA_LOAD_TYPE_NETWORK) {
                if (ListUtil.isEmpty(chapterUrlList)) {
                    Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
                    return;
                }

                // 设置下一章的章节索引
                if (currentChapterIndex >= chapterUrlList.size() - 1) {
                    return;
                }
                currentChapterIndex = currentChapterIndex + 1;

                // 获取下一章的章节标题
                String chapterTitle = chapterList.get(currentChapterIndex);
                currentChapterInfo.setChapterTitle(chapterTitle);

                // 获取下一章的章节链接
                String nextChapterSuffixUrl = chapterUrlList.get(currentChapterIndex);
                String nextChapterUrl = nextChapterSuffixUrl;
                if (!nextChapterSuffixUrl.startsWith(ConstUtil.HTTP_SCHEME) &&
                        !nextChapterSuffixUrl.startsWith(ConstUtil.HTTPS_SCHEME) &&
                        !JsonUtil.isValid(selectBookInfo.getBookUrl())) {
                    nextChapterUrl = baseUrl + nextChapterSuffixUrl;
                }
                currentChapterInfo.setChapterUrl(nextChapterUrl);
                // 远程获取下一章内容
                searchBookContentRemote(nextChapterUrl, (searchBookCallParam) -> {
                    // 初始化并缓存章节信息
                    currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
                    cacheService.setSelectedChapterInfo(currentChapterInfo);

                    // 标记为已成功切换下一个章节
                    IS_SWITCH_NEXT_CHAPTER_SUCCESS = true;

                    runnable.accept(currentChapterInfo, searchBookCallParam.getBodyElement());
                });
            } else if (dataLoadType == Settings.DATA_LOAD_TYPE_LOCAL) {
                // 获取章节内容列表
                chapterContentList = cacheService.getChapterContentList();
                // 获取下一章的章节索引
                if (currentChapterIndex >= chapterContentList.size() - 1) {
                    return;
                }
                currentChapterIndex = currentChapterIndex + 1;

                if (!chapterContentList.isEmpty()) {
                    // 获取下一章的章节标题
                    String chapterTitle = chapterList.get(currentChapterIndex);
                    currentChapterInfo.setChapterTitle(chapterTitle);
                    // 提取下一章的章节内容
                    chapterContentHtml = chapterContentList.get(currentChapterIndex);
                    Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
                    chapterContentText = pattern.matcher(chapterContentHtml).replaceAll("　");
                    chapterContentText = StringUtils.normalizeSpace(chapterContentText);
                    chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);

                    // 标记为已成功切换下一个章节
                    IS_SWITCH_NEXT_CHAPTER_SUCCESS = true;

                    // 初始化并缓存章节信息
                    currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
                    cacheService.setSelectedChapterInfo(currentChapterInfo);
                }

                runnable.accept(currentChapterInfo, null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

        fontSize = fontSize - 1;
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
        fontSize = fontSize + 1;
        cacheService.setFontSize(fontSize);
    }

    /**
     * 改变字体颜色
     */
    public void changeFontColor() {
        fontColorHex = cacheService.getFontColorHex();
        // 获取当前字体颜色
        Color currentFontColor = Color.decode(fontColorHex);
        Color color = JColorChooser.showDialog(null, "选择颜色", currentFontColor);
        if (color != null) {
            // 将选择的颜色转换为16进制字符串
            fontColorHex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            cacheService.setFontColorHex(fontColorHex);
        }
    }

    //region 加载本地文件
    /**
     * 加载本地文件
     *
     * @param regex 提取TXT文件中章节标题和章节内容的正则表达式
     */
    public void loadLocalFile(String regex) {
        initData();

        // 文件选择器
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false,
                false, false, false, false);
        fileChooserDescriptor.setTitle("选择文本文件");
        fileChooserDescriptor.setDescription(ConstUtil.WREADER_LOAD_LOCAL_TIP);
        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, null, null);
        if (virtualFile != null) {
            String filePath = virtualFile.getPath();
            String fileName = virtualFile.getName();
            // 获取文件后缀
            String fileExtension = virtualFile.getExtension();
            List<String> allowFileExtensions = configYaml.getAllowFileExtension();
            if (fileExtension == null || !allowFileExtensions.contains(fileExtension)) {
                String message = String.format(ConstUtil.WREADER_ONLY_SUPPORTED_FILE_TYPE, allowFileExtensions.toString());
                Messages.showMessageDialog(message, "提示", Messages.getInformationIcon());
                return;
            }
            File file = new File(filePath);

            // 停止语音
            stopTTS();
            // 重置编辑器消息垂直滚动条位置
            cacheService.setEditorMessageVerticalScrollValue(0);

            // 清空缓存数据
            cacheService.setChapterList(null);
            cacheService.setChapterContentList(null);
            cacheService.setSelectedChapterInfo(null);
            cacheService.setSelectedBookInfo(null);
            cacheService.setChapterUrlList(null);

            Task.Backgroundable loadLocalFileTask = new Task.Backgroundable(mProject, "【W-Reader】正在读取文件...") {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    // 在后台线程执行耗时操作
                    progressIndicator.setText("【W-Reader】正在读取文件...");
                    // 设置进度条为不确定模式
                    progressIndicator.setIndeterminate(true);

                    // 读取文件内容
                    if (ConstUtil.FILE_TYPE_TXT.equalsIgnoreCase(fileExtension)) {
                        loadFileTypeTxt(file, regex);
                    } else if (ConstUtil.FILE_TYPE_EPUB.equalsIgnoreCase(fileExtension)) {
                        loadFileTypeEpub(file);
                    }
                }

                @Override
                public void onSuccess() {
                    // 重置选中章节信息
                    cacheService.setSelectedChapterInfo(ChapterInfo.initEmptyChapterInfo());
                    currentChapterIndex = 0;

                    // 设置数据加载模式
                    settings.setDataLoadType(Settings.DATA_LOAD_TYPE_LOCAL);
                    cacheService.setSettings(settings);

                    // 重置内容面板
                    ToolWindowUtil.updateContentText(mProject, "");

                    Messages.showMessageDialog(ConstUtil.WREADER_LOAD_SUCCESS, "提示", Messages.getInformationIcon());
                }

                @Override
                public void onThrowable(@NotNull Throwable error) {
                    super.onThrowable(error);
                    Messages.showErrorDialog(ConstUtil.WREADER_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
                }
            };
            loadLocalFileTask.queue();
        }
    }

    /**
     * 加载本地文件--txt格式
     *
     * @param file
     * @param regex 提取章节标题和章节内容的正则表达式
     */
    public void loadFileTypeTxt(File file, String regex) {
        String textRegex = StringUtils.isEmpty(regex) ? ConstUtil.TEXT_FILE_DIR_REGEX : regex;
        // 读取文件内容
        String charset = settings.getCharset();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
            // 使用正则表达式提取章节标题和章节内容
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            List<String> chapterList = new ArrayList<>();
            List<String> chapterContentList = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                Pattern pattern = Pattern.compile(textRegex);
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // 当前章节列表为空，说明contentBuilder缓存的内容不是小说的正文，非正文内容无需添加
                    if (!chapterList.isEmpty()) {
                        // 将contentBuilder中缓存的内容添加到章节内容列表中
                        chapterContentList.add(contentBuilder.toString());
                    }
                    chapterList.add(line);
                    // 清空内容构建器
                    contentBuilder.setLength(0);
                }
                contentBuilder.append(line).append("<br>");
            }
            chapterContentList.add(contentBuilder.toString());

            cacheService.setChapterList(chapterList);
            cacheService.setChapterContentList(chapterContentList);
            this.chapterList = chapterList;
            this.chapterContentList = chapterContentList;

            // 创建一个BookInfo对象，并设置文件名、文件路径和内容
            BookInfo bookInfo = new BookInfo();
            // 分离文件名和后缀
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
     * 加载本地文件--epub格式
     *
     * @param file
     */
    public void loadFileTypeEpub(File file) {
        // 读取文件内容
        String charset = settings.getCharset();
        boolean isShowLocalImg = settings.isShowLocalImg();
        try (FileInputStream fis = new FileInputStream(file)) {
            // 获取临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            File tempDirFile = new File(tempDir);
            String tempDirPath = tempDirFile.getAbsolutePath() + File.separator + ConstUtil.WREADER_ID + File.separator +
                    "images" + File.separator;
            // 清空临时目录中的图片文件
            FileUtils.deleteDirectory(new File(tempDirPath));

            // 创建一个EpubReader对象，用于解析EPUB文件
            EpubReader epubReader = new EpubReader();
            // 使用EpubReader对象读取EPUB文件，并获取一个Book对象
            Book book = epubReader.readEpub(fis, charset);
            // 存储图片临时路径
            Map<String, String> imgTempPathMap = new HashMap<>();
            // 存储图片的宽度信息
            Map<String, Integer> imgTempWidthMap = new HashMap<>();
            // 获取图片是否显示标志
            if (isShowLocalImg) {
                // 获取Epub中所有资源(包括章节信息以及各种静态资源)
                Map<String, Resource> resourceMap = book.getResources().getResourceMap();
                // 遍历资源列表，将图片保存至本地临时文件中
                for (Map.Entry<String, Resource> entry : resourceMap.entrySet()) {
                    Resource resource = entry.getValue();
                    String key = entry.getKey();
                    if ((resource.getMediaType() != null && resource.getMediaType().getName().startsWith("image/")) ||
                            FileUtil.isUnsupportedImageFormat(key)) {
                        try (InputStream inputStream = resource.getInputStream()) {
                            byte[] data = IOUtil.toByteArray(inputStream);
                            String filePath = tempDirPath + entry.getKey();
                            // 如果图片格式为JTextPanel不支持展示的格式，则将其转换为JPG格式，反之则直接保存至本地临时文件内
                            if (FileUtil.isUnsupportedImageFormat(key)) {
                                filePath = FileUtil.convertImgToJPG(data, filePath);
                            } else {
                                FileUtils.writeByteArrayToFile(new File(filePath), data);
                            }

                            if (StringUtils.isNotBlank(filePath)) {
                                BufferedImage originalImage = ImageIO.read(new File(filePath));
                                imgTempWidthMap.put(entry.getKey(), originalImage.getWidth());

                                imgTempPathMap.put(entry.getKey(), "file:///" + filePath.replace("\\", "/"));
                            }
                        }
                    }
                }
            }

            // 创建两个列表，分别存储章节标题和章节内容
            List<String> chapterList = new ArrayList<>();
            List<String> chapterContentList = new ArrayList<>();

            // 遍历章节列表
            EpubReaderComplete.readEpub(book, resMap -> {
                String title = resMap.get("title");
                String content = resMap.get("content");
                // 获取<body>标签中的内容
                content = StringUtil.extractBodyContent(content);
                if (isShowLocalImg) {
                    content = StringUtil.replaceImageLinks(content, imgTempPathMap, imgTempWidthMap);
                }

                chapterList.add(title);
                chapterContentList.add(content);
            });
            cacheService.setChapterList(chapterList);
            cacheService.setChapterContentList(chapterContentList);
            this.chapterList = chapterList;
            this.chapterContentList = chapterContentList;

            // 保存书本信息
            // 创建一个BookInfo对象
            BookInfo bookInfo = new BookInfo();
            String bookName = ListUtil.listToString(book.getMetadata().getTitles());
            bookInfo.setBookName(bookName);
            List<String> descriptions = book.getMetadata().getDescriptions();
            String bookDesc = bookName;
            if (descriptions != null && !descriptions.isEmpty()) {
                bookDesc = ListUtil.listToString(book.getMetadata().getDescriptions());
            }
            bookInfo.setBookDesc(bookDesc);
            String author = ListUtil.listToString(book.getMetadata().getAuthors());
            bookInfo.setBookAuthor(author);
            cacheService.setSelectedBookInfo(bookInfo);
        } catch (IOException e) {
            e.printStackTrace();
            Messages.showMessageDialog(ConstUtil.WREADER_LOAD_FAIL, "提示", Messages.getInformationIcon());
        }
    }
    //endregion

    //region auto read next line

    /**
     * 自动阅读下一行，如果已执行，再次调用则会停止
     */
    public void autoReadNextLine() {
        // 如果executorService不为空且未关闭，则关闭
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            return;
        }

        // 获取当前选中章节信息
        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            return;
        }

        // 获取当前选中章节内容
        AtomicReference<List<String>> chapterContentList = new AtomicReference<>(selectedChapterInfo.getChapterContentList());
        if (chapterContentList.get() == null || chapterContentList.get().isEmpty()) {
            return;
        }

        // 创建一个定时任务执行器
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }

        // 获取章节内容长度
        AtomicInteger len = new AtomicInteger(chapterContentList.get().size());
        // 获取自动阅读时间
        float autoReadTime = settings.getAutoReadTime();
        if (autoReadTime <= 0f) {
            autoReadTime = 5f;
        }

        // 创建一个任务，每过autoReadTime设定的时间就执行一次
        Runnable readNextLineTask = () -> {
            int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();
            len.set(selectedChapterInfo.getChapterContentList() == null ? 0 : selectedChapterInfo.getChapterContentList().size());

            if (lastReadLineNum < len.get()) {
                IS_SWITCH_NEXT_CHAPTER_SUCCESS = false;
                WReaderStatusBarWidget.nextLine(mProject);
            } else {
                if (!IS_SWITCH_NEXT_CHAPTER_SUCCESS) {
                    // 停止语音
                    stopTTS();
                    // 重置编辑器消息垂直滚动条位置
                    cacheService.setEditorMessageVerticalScrollValue(0);
//                    nextPageChapter();
                    WReaderStatusBarWidget.nextChapter(mProject);
                }
            }
        };

        long autoReadTimeMillis = (long) (autoReadTime * 1000);
        // 调度任务，每过autoReadTime秒执行一次
        executorService.scheduleAtFixedRate(readNextLineTask, autoReadTimeMillis, autoReadTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止定时器
     */
    public void executorServiceShutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    //endregion

    //region TTS
    private static EdgeTTS edgeTTS;

    /**
     * 小说内容文本转语音
     */
    public void ttsChapterContent() throws Exception {
        // 获取所选择的小说内容
        String chapterContent = currentChapterInfo.getChapterContentStr();
        if (StringUtils.isBlank(chapterContent)) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_BOOK_CONTENT_ERROR, MessageDialogUtil.TITLE_INFO);
            return;
        }

        if (edgeTTS != null) {
            edgeTTS.dispose();
            edgeTTS = null;
            return;
        }

        edgeTTS = new EdgeTTS();

        // 音色
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
        audioStyle = StringUtils.isBlank(audioStyle) ? configYaml.getSettings().getAudioStyle() : audioStyle;

        edgeTTS.setVoiceRole(voiceRoleEnum)
                .setStyleName(audioStyle)
                .setRate(rate.toString())
                .setVolume(volume.toString());

        edgeTTS.synthesize(chapterContent);
        edgeTTS.start();

    }

    /**
     * 停止语音
     */
    public void stopTTS() {
        if (edgeTTS != null) {
            edgeTTS.dispose();
            edgeTTS = null;
        }
    }
    //endregion

    /**
     * 处理小说内容
     *
     * @param content 内容处理规则配置信息 or 小说内容
     * @return
     */
    public String handleContent(String content) throws Exception {
        String result = "";
        String chapterContentHandleRule = selectedChapterRules.getContentHandleRule();
        if (chapterContentHandleRule.startsWith(ConstUtil.JAVA_CODE_CONFIG_START_LABEL) &&
                chapterContentHandleRule.endsWith(ConstUtil.JAVA_CODE_CONFIG_END_LABEL)) {

            // 判断是否是新版的动态代码
            if (chapterContentHandleRule.contains(ConstUtil.CODE_CONFIG_CODE_START) &&
                    chapterContentHandleRule.contains(ConstUtil.CODE_CONFIG_CODE_END)) {
                // 执行动态代码（新版）
                result = (String) DynamicCodeExecutor.executeMethod(chapterContentHandleRule,
                        "execute",
                        new Class[]{String.class},
                        new Object[]{content});
            } else {
                // 兼容旧版的动态代码执行器
                chapterContentHandleRule = StringTemplateEngine.render(chapterContentHandleRule, new HashMap<>() {{
                    put("content", content);
                }});
                // 执行配置中的方法
                result = MethodExecutor.executeMethod(chapterContentHandleRule).toString();

            }

            // 将换行符和制表符替换成html对应代码
            result = result.replaceAll("\\n", "<br/>")
                    .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
        } else {
            result = content;
            String chapterContentUrl = selectedChapterRules.getUrl();
            String chapterContentUrlDataRule = selectedChapterRules.getUrlDataRule();
            if (StringUtils.isBlank(chapterContentHandleRule) &&
                    StringUtils.isNotBlank(chapterContentUrl) &&
                    StringUtils.isNotBlank(chapterContentUrlDataRule)) {
                // 将换行符和制表符替换成html对应代码
                result = content.replaceAll("\\n", "<br/>")
                        .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
            }
        }

        // 章节内容处理规则
        List<String> contentRegexList = selectedChapterRules.getContentRegexList();
        if (ListUtil.isNotEmpty(contentRegexList)) {
            for (String contentRegex : contentRegexList) {
                String[] regulars = contentRegex.split(ConstUtil.SPLIT_REGEX_REPLACE_FLAG);
                String regex = regulars[0];
                String replacement = regulars.length > 1 ? regulars[1] : "";
                result = result.replaceAll(regex, replacement);
            }
        }

        return result;
    }

    /**
     * 分割章节内容
     */
    public void splitChapterContent() {
        String chapterContentStr = currentChapterInfo.getChapterContentStr();
        // 获取将章节内容按指定字符长度分割的集合
        List<String> chapterContentSplitList = currentChapterInfo.getChapterContentList();
        int singleLineChars = settings.getSingleLineChars();

        // 当chapterContentSplitList为空时, 按照单行最大字数将字符串分割成数组
        if (chapterContentSplitList == null || chapterContentSplitList.isEmpty()) {
            chapterContentSplitList = StringUtil.splitStringByMaxCharList(chapterContentStr, singleLineChars);
        }
        currentChapterInfo.setChapterContentList(chapterContentSplitList);
    }

    private String requestContent(String url, Consumer<String> call) {
        String content = "";
        ChapterRules chapterRules = selectedSiteBean.getChapterRules();
        try {
            if (chapterRules.isUseNextContentApi()) {
                HttpRequestBase requestBase = HttpUtil.commonRequest(url);
                requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
                try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        HttpEntity entity = httpResponse.getEntity();
                        String result = EntityUtils.toString(entity);
                        Gson gson = new Gson();
                        JsonObject resJson = gson.fromJson(result, JsonObject.class);

                        // 使用jsonpath获取内容
                        Object readJson = JsonPath.read(resJson.toString(), chapterRules.getNextContentApiDataRule());

                        content = (String) readJson;
                        call.accept(resJson.toString());
                    }
                } catch (Exception e) {
                    Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                    throw new RuntimeException(e);
                }
            } else {
                Document document = null;
                try {
                    document = Jsoup.connect(url)
                            .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                            .get();
                } catch (IOException e) {
                    Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                    throw new RuntimeException(e);
                }

                // 页面展示主体
                Element bodyElement = document.body();
                call.accept(bodyElement.html());
                // 获取小说内容
                // 小说内容的HTML标签类型（class, id）
                String chapterContentElementName = chapterRules.getContentElementName();
                Elements chapterContentElements = bodyElement.select(chapterContentElementName);
                if (chapterContentElements.isEmpty()) {
                    return "";
                }

                StringBuilder contentHtml = new StringBuilder();
                for (Element element : chapterContentElements) {
                    Tag tag = element.tag();
                    String html = element.html();
                    if (tag.isEmpty() || StringUtils.trimToNull(html) == null) {
                        continue;
                    }

                    contentHtml.append(String.format("<%s>%s</%s>", tag.normalName(), html, tag.normalName()));
                }

                content = contentHtml.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 将换行符和制表符替换成html对应代码
        content = content.replaceAll("\\n", "<br/>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
        // 章节内容处理规则
        List<String> contentRegexList = chapterRules.getContentRegexList();
        if (ListUtil.isNotEmpty(contentRegexList)) {
            for (String contentRegex : contentRegexList) {
                String[] regulars = contentRegex.split(ConstUtil.SPLIT_REGEX_REPLACE_FLAG);
                String regex = regulars[0];
                String replacement = regulars.length > 1 ? regulars[1] : "";
                content = content.replaceAll(regex, replacement);
            }
        }

        return content;
    }

    /**
     * 处理缓存
     */
    private void handleCache() {
        // 选择的站点信息缓存
        cacheService.setSelectedSiteBean(selectedSiteBean);
        cacheService.setSelectedBookSiteIndex(selectedBookSiteIndex);
        // 选择的小说信息缓存
        cacheService.setSelectedBookInfo(selectBookInfo);
        // 小说目录缓存
        cacheService.setChapterList(chapterList);
        cacheService.setChapterUrlList(chapterUrlList);
        // 选择的小说章节缓存
        cacheService.setSelectedChapterInfo(currentChapterInfo);

        // 自定义站点规则缓存
        customSiteRuleCacheServer.setSelectedCustomSiteRuleKey(selectSiteGroupName);
    }

    /**
     * 取消操作时的通用操作
     */
    private void commCancelOperationHandle() {
        initData();
    }

    /**
     * 获取鼠标点击的文档位置
     *
     * @param e
     * @return
     */
    public static int getClickedPosition(JTextPane contentTextPane, MouseEvent e) {
        Point p = e.getPoint();
        return contentTextPane.viewToModel2D(p);
    }

    /**
     * 获取指定位置的HTML标签
     *
     * @param textPane
     * @param pos
     * @return
     */
    public static String getHTMLTagAtPosition(JTextPane textPane, int pos) {
        javax.swing.text.Document doc = textPane.getDocument();
        if (!(doc instanceof HTMLDocument htmlDoc)) {
            return null;
        }

        javax.swing.text.Element element = htmlDoc.getCharacterElement(pos);

        // 收集所有包含该位置的标签
        List<javax.swing.text.Element> elements = new ArrayList<>();
        javax.swing.text.Element current = element;
        while (current != null && current.getName() != null) {
            elements.add(current);
            current = current.getParentElement();
        }

        // 构建HTML标签字符串（从最内层到最外层）
        StringBuilder sb = new StringBuilder();
        for (javax.swing.text.Element e : elements) {
            AttributeSet attrs = e.getAttributes();
            HTML.Tag tag = (HTML.Tag) attrs.getAttribute(StyleConstants.NameAttribute);
            if (tag != null) {
                sb.append("<").append(tag);
                // 添加属性
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
                sb.append(">");
            }
        }

        // 反转列表以展示正确的嵌套顺序
        Collections.reverse(elements);
        return sb.toString();
    }

    private static @NotNull String getContent(String text) {
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();
        // 设置内容
        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";

        return "<div style=\"" + style + "\">" + text + "</div>";
    }
}
