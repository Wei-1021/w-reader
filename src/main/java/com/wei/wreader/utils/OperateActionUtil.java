package com.wei.wreader.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import com.intellij.openapi.ui.ComboBox;
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
import com.wei.wreader.utils.http.HttpRequestConfigParser;
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
import org.jsoup.Connection;
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
import java.net.MalformedURLException;
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
     * 构建搜索弹出窗口
     */
    public void buildSearchBookDialog(Project project) {
        SwingUtilities.invokeLater(() -> {
            // 创建一个搜索弹出窗
            // 书源列表下拉框
//            ComboBox<String> siteListComboBox = buildSiteComboBox();
            // 书源分组下拉框
//            ComboBox<String> siteGroupComboBox = buildSiteGroupComboBox(siteListComboBox);
            // 搜索框
//            JTextField searchBookTextField = new JTextField(20);
//            Object[] objs = {"书源分组", siteGroupComboBox, ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT, siteListComboBox, searchBookTextField};
//            searchBookDialogBuilder = MessageDialogUtil.showMessageDialog(project, ConstUtil.WREADER_SEARCH_BOOK_TITLE, objs,
//                    () -> searchBookDialogOk(siteListComboBox, searchBookTextField),
//                    this::commCancelOperationHandle);
        });
    }

    /**
     * 构建书源列表分组下拉框
     */
    private ComboBox<String> buildSiteGroupComboBox(ComboBox<String> siteListComboBox) {
        // 获取书源分组Map
        Map<String, List<SiteBean>> siteGroupMap = customSiteUtil.getSiteMap();
        // 获取书源分组名称列表
        List<String> siteGroupNameList = customSiteUtil.getCustomSiteKeyGroupList();
        // 获取已选择的分组
        String selectedSiteGroupName = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();
        if (selectedSiteGroupName == null) {
            // 获取分组名称列表中指定分组的索引
            int selectedSiteGroupIndex = siteGroupNameList.indexOf(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY);
            // 默认
            selectedSiteGroupName = siteGroupNameList.get(selectedSiteGroupIndex);
        }
        siteBeanList = siteGroupMap.get(selectedSiteGroupName);

        // 创建书源分组下拉框
        ComboBox<String> comboBox = new ComboBox<>();
        int selectedSiteGroupIndex = 0;
        for (int i = 0; i < siteGroupNameList.size(); i++) {
            String siteGroupName = siteGroupNameList.get(i);
            comboBox.addItem(siteGroupName);
            if (siteGroupName.equals(selectedSiteGroupName)) {
                selectedSiteGroupIndex = i;
            }
        }
        comboBox.setSelectedIndex(selectedSiteGroupIndex);
        comboBox.addItemListener(e -> {
            selectSiteGroupName = (String) e.getItem();
            siteBeanList = siteGroupMap.get(selectSiteGroupName);
            // 刷新书源列表下拉框
            siteListComboBox.removeAllItems();
            for (SiteBean site : siteBeanList) {
                siteListComboBox.addItem(site.getName() + "(" + site.getId() + ")");
            }
            siteListComboBox.setSelectedItem(0);
            selectedBookSiteIndex = 0;
            siteListComboBox.setSelectedIndex(selectedBookSiteIndex);
        });
        return comboBox;
    }

    /**
     * 构建书源选择下拉框
     *
     * @return
     */
    private @NotNull ComboBox<String> buildSiteComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        for (SiteBean site : siteBeanList) {
            comboBox.addItem(site.getName() + "(" + site.getId() + ")");
        }
        comboBox.setSelectedIndex(selectedBookSiteIndex);
        return comboBox;
    }

    /**
     * 搜索弹出窗口确定按钮点击事件
     *
     * @param comboBox
     * @param searchBookTextField
     */
    private void searchBookDialogOk(ComboBox<String> comboBox, JTextField searchBookTextField) {
        int selectedIndex = comboBox.getSelectedIndex();
        selectedBookSiteIndex = selectedIndex;
        tempSelectedSiteBean = selectedSiteBean;
        selectedSiteBean = siteBeanList.get(selectedIndex);
        selectedSearchRules = selectedSiteBean.getSearchRules();
        selectedListMainRules = selectedSiteBean.getListMainRules();
        selectedChapterRules = selectedSiteBean.getChapterRules();
        selectedBookInfoRules = selectedSiteBean.getBookInfoRules();
        baseUrl = selectedSiteBean.getBaseUrl();
        // 缓存临时搜索站点信息
        cacheService.setTempSelectedSiteBean(selectedSiteBean);
        searchBook(searchBookTextField);
    }

    /**
     * 搜索小说
     *
     * @param searchBookTextField
     */
    public void searchBook(JTextField searchBookTextField) {
        String bookName = searchBookTextField.getText();
        if (bookName == null || bookName.trim().isEmpty()) {
            Messages.showMessageDialog(ConstUtil.WREADER_SEARCH_EMPTY, "提示", Messages.getInformationIcon());
            return;
        }

        String searchBookUrl = "";
        SiteBean thisTempSelectedSiteBean = cacheService.getTempSelectedSiteBean();
        if (thisTempSelectedSiteBean == null) {
            thisTempSelectedSiteBean = selectedSiteBean;
        }
        String searchUrl = thisTempSelectedSiteBean.getSearchRules().getUrl();
        if (StringUtils.isBlank(searchUrl)) {
            Messages.showMessageDialog(ConstUtil.WREADER_ERROR, "提示", Messages.getInformationIcon());
            return;
        }

        if (searchUrl.startsWith(ConstUtil.CODE_CONFIG_START_LABEL) && searchUrl.endsWith(ConstUtil.CODE_CONFIG_END_LABEL)) {
            try {
                // 执行动态代码
                searchBookUrl = (String) DynamicCodeExecutor.executeMethod(
                        searchUrl,
                        "execute",
                        new Class<?>[]{String.class, String.class},
                        new Object[]{bookName, "1"}
                );
            } catch (Exception e) {
                e.printStackTrace();
                Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
            }
        } else {
            // 使用模板引擎匹配参数
            searchUrl = StringTemplateEngine.render(searchUrl, new HashMap<>() {{
                put("key", bookName);
                put("page", 1);
            }});

            searchBookUrl = searchUrl;
        }
        String finalSearchBookUrl = searchBookUrl;
        new Task.Backgroundable(mProject, "【W-Reader】正在搜索...") {
            private String searchBookResult = "";
            private Exception error = null;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // 在后台线程执行耗时操作
                progressIndicator.setText("【W-Reader】正在搜索...");
                // 设置进度条为不确定模式
                progressIndicator.setIndeterminate(true);

                try {
                    // 获取搜索结果
                    searchBookResult = searchBookList(finalSearchBookUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 捕获异常，以便在 onSuccess 或 onThrowable 中处理
                    error = e;
                }
            }

            @Override
            public void onSuccess() {
                // 任务成功完成，在 EDT 中更新 UI
                if (error != null) {
                    // 如果有错误，在 UI 中显示错误信息
                    Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
                } else if (searchBookResult != null) {
                    // 如果成功获取结果，显示结果
                    if (StringUtils.isBlank(searchBookResult) ||
                            ConstUtil.STR_ONE.equals(searchBookResult) ||
                            "[]".equals(searchBookResult)) {
                        Messages.showInfoMessage(ConstUtil.WREADER_SEARCH_BOOK_ERROR, "提示");
                    }

                    // 初始化数据
                    bookInfoList = new ArrayList<>();
                    bookNameList = new ArrayList<>();

                    // 处理并展示搜索结果
                    handleBookList(searchBookResult);
                    if (searchBookDialogBuilder != null) {
                        searchBookDialogBuilder.dispose();
                    }
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                error.printStackTrace();
                Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
            }
        }.queue();
    }

    /**
     * 搜索小说列表
     *
     * @param url
     * @return
     */
    public String searchBookList(String url) {
        String result = null;

        SiteBean tempSearchSelectedSiteBean = cacheService.getTempSelectedSiteBean();
        if (tempSearchSelectedSiteBean == null) {
            tempSearchSelectedSiteBean = selectedSiteBean;
        }
        SearchRules tempSearchSearchRules = tempSearchSelectedSiteBean.getSearchRules();
        BookInfoRules tempSearchBookInfoRules = tempSearchSelectedSiteBean.getBookInfoRules();

        String header = tempSearchSelectedSiteBean.getHeader();
        Map<String, String> headerJson = new HashMap<>();
        if (StringUtils.isNotBlank(header)) {
            Gson gson = new Gson();
            headerJson = gson.fromJson(header, HashMap.class);
        }

        // 获取小说列表的接口返回的是否是html
        boolean isHtml = tempSearchSelectedSiteBean.isHasHtml();
        if (isHtml) {
            HttpRequestConfigParser parser = new HttpRequestConfigParser(url);
            String requestUrl = parser.getUrl();
            String requestMethod = parser.getMethod();
            Map<String, String> queryParam = parser.getQueryParams();
            Map<String, String> bodyParam = parser.getBodyParams();
            Map<String, String> headerMap = parser.getHeader();

            // 获取html
            Document document = null;
            try {
                if (queryParam != null && !queryParam.isEmpty()) {
                    requestUrl += "?";
                    for (Map.Entry<String, String> entry : queryParam.entrySet()) {
                        requestUrl += entry.getKey() + "=" + entry.getValue() + "&";
                    }
                }

                Connection connection = Jsoup.connect(requestUrl);
                if (headerMap != null && !headerMap.isEmpty()) {
                    connection.headers(headerMap);
                }
                if (HttpUtil.POST.equals(requestMethod)) {
                    connection.method(Connection.Method.POST);
                    for (Map.Entry<String, String> entry : bodyParam.entrySet()) {
                        connection.data(entry.getKey(), entry.getValue());
                    }
                    connection.header("Content-Type", "application/x-www-form-urlencoded");
                } else {
                    connection.method(Connection.Method.GET);
                }
                document = connection.execute().parse();
            } catch (IOException e) {
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                throw new RuntimeException(e);
            }
            // 小说列表的HTML标签类型（class, id）
            Elements elements = document.select(tempSearchSearchRules.getBookListElementName());
            JsonArray jsonArray = new JsonArray();
            String location = document.location();
            // 获取小说列表链接元素cssQuery
            String bookListUrlElement = tempSearchSearchRules.getBookListUrlElement();
            bookListUrlElement = StringUtils.isBlank(bookListUrlElement) ? "a" : bookListUrlElement;
            // 获取小说列表链接元素cssQuery规则
            String[] bookListUrlElementRules = bookListUrlElement.split("@");
            String bookListUrlCssQueryRule = bookListUrlElementRules[0];
            String bookListUrlRuleBack = "";
            String bookListUrlRuleFront = "";
            if (bookListUrlElementRules.length > 1) {
                for (String bookListUrlElementItemRule : bookListUrlElementRules) {
                    if (bookListUrlElementItemRule.startsWith(ConstUtil.CSS_QUERY_BACK_FLAG)) {
                        // 提取@back:后面的字符串
                        bookListUrlRuleBack = bookListUrlElementItemRule.replace(ConstUtil.CSS_QUERY_BACK_FLAG, "");
                    } else if (bookListUrlElementItemRule.startsWith(ConstUtil.CSS_QUERY_FONT_FLAG)) {
                        // 提取@font:后面的字符串
                        bookListUrlRuleFront = bookListUrlElementItemRule.replace(ConstUtil.CSS_QUERY_FONT_FLAG, "");
                    }
                }
            }
            // 获取小说列表标题元素cssQuery
            String bookListTitleElement = tempSearchSearchRules.getBookListTitleElement();
            for (Element itemElement : elements) {
                if (itemElement != null) {
                    // url
                    Element bookUrlElement = itemElement.selectFirst(bookListUrlCssQueryRule);
                    String bookUrl = "";
                    if (bookUrlElement != null) {
                        bookUrl = bookListUrlRuleFront + bookUrlElement.attr("href") + bookListUrlRuleBack;
                    }
                    // title
                    Element bookTitleElement = itemElement.selectFirst(bookListTitleElement);
                    String bookName = "";
                    if (bookTitleElement != null) {
                        bookName = bookTitleElement.text();
                    }

                    try {
                        bookUrl = UrlUtil.buildFullURL(location, bookUrl);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty(tempSearchBookInfoRules.getBookNameField(), bookName);
                    jsonObject.addProperty(tempSearchBookInfoRules.getBookUrlField(), bookUrl);
                    jsonObject.addProperty(tempSearchBookInfoRules.getBookAuthorField(), "");
                    jsonObject.addProperty(tempSearchBookInfoRules.getBookDescField(), "");
                    jsonObject.addProperty(tempSearchBookInfoRules.getBookImgUrlField(), "");
                    jsonArray.add(jsonObject);
                }
            }
            return jsonArray.toString();
        } else {
            HttpRequestBase requestBase = HttpUtil.commonRequest(url);
            requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
            try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = httpResponse.getEntity();
                    result = EntityUtils.toString(entity);

                    // 使用jsonpath提取小说列表
                    Object readJsonObject = JsonPath.read(result, tempSearchSearchRules.getDataBookListRule());
                    result = Objects.isNull(readJsonObject) ? "" : readJsonObject.toString();
                }
            } catch (IOException e) {
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    /**
     * 处理搜索小说结果
     *
     * @param result
     */
    public void handleBookList(String result) {
        if (!result.isEmpty()) {
            SiteBean tempSearchSelectedSiteBean = cacheService.getTempSelectedSiteBean();
            if (tempSearchSelectedSiteBean == null) {
                tempSearchSelectedSiteBean = selectedSiteBean;
            }
            SearchRules tempSearchSearchRules = tempSearchSelectedSiteBean.getSearchRules();
            BookInfoRules tempSearchBookInfoRules = tempSearchSelectedSiteBean.getBookInfoRules();

            JsonArray jsonArray = null;
            try {
                Gson gson = new Gson();
                jsonArray = gson.fromJson(result, JsonArray.class);
            } catch (Exception e) {
                e.printStackTrace();
                Messages.showMessageDialog(ConstUtil.WREADER_ERROR, "提示", Messages.getInformationIcon());
            }

            SiteBean searchSelectedSiteBean = tempSearchSelectedSiteBean;
            if (jsonArray != null && !jsonArray.isEmpty()) {
                // 获取书本信息列表
                for (int i = 0, len = jsonArray.size(); i < len; i++) {
                    JsonObject asJsonObject = jsonArray.get(i).getAsJsonObject();

                    // 获取信息
                    String bookId      = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookIdField());
                    String articleName = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookNameField());
                    String author      = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookAuthorField());
                    String intro       = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookDescField());
                    String urlImg      = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookImgUrlField());
                    String urlList     = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookUrlField());

                    // 设置bookInfo
                    BookInfo bookInfo = new BookInfo();
                    bookInfo.setBookId(bookId);
                    bookInfo.setBookName(articleName);
                    bookInfo.setBookAuthor(author);
                    bookInfo.setBookDesc(intro);
                    bookInfo.setBookImgUrl(urlImg);
                    bookInfo.setBookUrl(urlList);

                    bookInfoList.add(bookInfo);
                    bookNameList.add(articleName);
                }

                // 创建JBList（小说列表）
                JBList<String> searchBookList = new JBList<>(bookNameList);
                searchBookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                // 选择监听
                searchBookList.addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {

                        // 获取选择的小说信息
                        int selectedIndex = searchBookList.getSelectedIndex();
                        selectBookInfo = bookInfoList.get(selectedIndex);
                        // 小说目录链接
                        ListMainRules tempListMainRules = searchSelectedSiteBean.getListMainRules();
                        if (tempListMainRules == null) {
                            Messages.showErrorDialog(ConstUtil.WREADER_CACHE_ERROR, "提示");
                            return;
                        }
                        String listMainUrl = tempListMainRules.getUrl();
                        // 请求链接
                        String bookUrl = "";

                        // 判断是否是动态代码配置
                        if (listMainUrl.startsWith(ConstUtil.CODE_CONFIG_START_LABEL) &&
                                listMainUrl.endsWith(ConstUtil.CODE_CONFIG_END_LABEL)) {
                            try {
                                Class<?>[] paramTypes = new Class[]{};
                                Object[] params = new Object[]{};
                                if (listMainUrl.contains("com.wei.wreader.pojo.BookInfo")) {
                                    paramTypes = new Class[]{BookInfo.class};
                                    params = new Object[]{selectBookInfo};
                                } else {
                                    paramTypes = new Class[]{String.class};
                                    params = new Object[]{selectBookInfo.getBookId()};
                                }

                                bookUrl = (String) DynamicCodeExecutor.executeMethod(listMainUrl,
                                        "execute",
                                        paramTypes,
                                        params);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                                Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
                            }
                        } else {
                            // 判断获取小说目录的方式：调用api接口或者html页面获取
                            boolean isHtml = searchSelectedSiteBean.isHasHtml();
                            if (StringUtils.isNotBlank(listMainUrl) && !isHtml) {
                                bookUrl = StringTemplateEngine.render(listMainUrl, new HashMap<>() {{
                                    put("bookId", selectBookInfo.getBookId());
                                }});
                            } else {
                                bookUrl = selectBookInfo.getBookUrl();
                                if (!selectBookInfo.getBookUrl().startsWith(ConstUtil.HTTP_SCHEME) &&
                                        !selectBookInfo.getBookUrl().startsWith(ConstUtil.HTTPS_SCHEME) &&
                                        !JsonUtil.isValid(selectBookInfo.getBookUrl())) {
                                    bookUrl = tempSearchSearchRules.getUrl() + selectBookInfo.getBookUrl();
                                }
                            }
                        }

                        // 搜索小说目录
                        String finalBookUrl = bookUrl;
                        new Task.Backgroundable(mProject, "【W-Reader】正在获取小说目录...") {
                            boolean isSuccess = false;
                            List<String> tempChapterList = new ArrayList<>();
                            List<String> tempChapterUrlList = new ArrayList<>();

                            @Override
                            public void run(@NotNull ProgressIndicator progressIndicator) {
                                // 在后台线程执行耗时操作
                                progressIndicator.setText("【W-Reader】正在获取小说目录...");
                                // 设置进度条为不确定模式
                                progressIndicator.setIndeterminate(true);

                                Map<String, List<String>> chapterMap = searchBookDirectory(finalBookUrl);
                                if (chapterMap != null && !chapterMap.isEmpty()) {
                                    isSuccess = true;
                                    tempChapterList = chapterMap.get("chapterList");
                                    tempChapterUrlList = chapterMap.get("chapterUrlList");
                                }
                            }

                            @Override
                            public void onSuccess() {
                                if (isSuccess) {
                                    buildBookDirectoryDialog(tempChapterList, tempChapterUrlList);
                                } else {
                                    Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
                                }
                            }

                            @Override
                            public void onThrowable(@NotNull Throwable error) {
                                error.printStackTrace();
                                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                            }
                        }.queue();
                        cacheService.setChapterContentList(null);
                    }
                });

                // 使用滚动面板来添加滚动条
                JBScrollPane jScrollPane = new JBScrollPane(searchBookList);
                jScrollPane.setPreferredSize(new Dimension(350, 500));
                MessageDialogUtil.showMessageDialog(mProject, "搜索结果", jScrollPane,
                        null, this::commCancelOperationHandle);
            }
        }
    }

    /**
     * 获取小说目录
     *
     * @param url
     * @throws IOException
     */
    public Map<String, List<String>> searchBookDirectory(String url) {
        boolean isSuccess = false;
        List<String> tempChapterList = new ArrayList<>();
        List<String> tempChapterUrlList = new ArrayList<>();
        // 获取目录列表元素
        // 当使用api接口获取目录列表，且小说目录列表JSONPath规则不为空时，则使用api接口获取，
        // 反之使用html页面获取
        SiteBean searchTempSelectedSiteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules searchTempListMainRules = searchTempSelectedSiteBean.getListMainRules();
        String listMainUrl = searchTempListMainRules.getUrl();
        String listMainUrlDataRule = searchTempListMainRules.getUrlDataRule();
        if (StringUtils.isNotBlank(listMainUrl) && StringUtils.isNotBlank(listMainUrlDataRule)) {
            HttpRequestBase requestBase = HttpUtil.commonRequest(url);
            requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
            try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = httpResponse.getEntity();
                    String result = EntityUtils.toString(entity);

                    // 使用jsonpath获取目录列表
                    Object readJson = JsonPath.read(result, listMainUrlDataRule);
                    Gson gson = new Gson();
                    String itemListStr = gson.toJson(readJson);
                    JsonArray listMainJsonArray = gson.fromJson(itemListStr, JsonArray.class);

                    Map<String, String> paramMap = new HashMap<>();
                    paramMap.put("dataJsonStr", result);
                    paramMap.put("menuListJsonStr", itemListStr);

                    String listMainItemIdField = searchTempListMainRules.getItemIdField();
                    String listMainItemTitleField = searchTempListMainRules.getItemTitleField();
                    ChapterRules tempSelectedChapterRules = searchTempSelectedSiteBean.getChapterRules();
                    String chapterUrl = tempSelectedChapterRules.getUrl();
                    boolean isCodeConfig = chapterUrl.startsWith(ConstUtil.CODE_CONFIG_START_LABEL) &&
                            chapterUrl.endsWith(ConstUtil.CODE_CONFIG_END_LABEL);

                    List<String> itemIdList = new ArrayList<>();
                    List<Integer> itemIndexList = new ArrayList<>();
                    for (int i = 0, len = listMainJsonArray.size(); i < len; i++) {
                        JsonObject jsonObject = listMainJsonArray.get(i).getAsJsonObject();
                        String itemId = jsonObject.get(listMainItemIdField).getAsString();
                        String title = jsonObject.get(listMainItemTitleField).getAsString();
                        String itemChapterUrl = "";
                        // 当chapterUrl符合规则时，则将itemId和index加入集合中，反正视为普通的API请求地址
                        if (isCodeConfig) {
                            itemIdList.add(itemId);
                            itemIndexList.add(i);
                            tempChapterList.add(title);
                        } else {
                            itemChapterUrl = StringTemplateEngine.render(chapterUrl, new HashMap<>() {{
                                put("bookId", selectBookInfo.getBookId());
                                put("itemId", itemId);
                            }});

                            tempChapterList.add(title);
                            tempChapterUrlList.add(itemChapterUrl);
                        }
                    }

                    // 当chapterUrl符合规则时，视为Java代码，并执行动态代码
                    if (isCodeConfig) {
                        try {
                            // 执行动态代码
                            tempChapterUrlList = (List<String>) DynamicCodeExecutor.executeMethod(chapterUrl,
                                    "execute",
                                    new Class[]{Map.class, List.class, String.class, List.class},
                                    new Object[]{paramMap, itemIndexList, selectBookInfo.getBookId(), itemIdList});
                        } catch (Exception e1) {
                            Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
                            throw new RuntimeException(e1);
                        }
                    }

                    isSuccess = true;
                }
            } catch (IOException e) {
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

            String listMainElementName = searchTempListMainRules.getListMainElementName();
            Elements listMainElement = document.select(listMainElementName);
            // 获取页面的地址
            String location = document.location();
            // 目录链接元素cssQuery
            String chapterListUrlElement = searchTempListMainRules.getUrlElement();
            chapterListUrlElement = StringUtils.isBlank(chapterListUrlElement) ? "a" : chapterListUrlElement;
            // 目录标题元素cssQuery
            String chapterListTitleElement = searchTempListMainRules.getTitleElement();
            for (Element element : listMainElement) {
                // url
                Element chapterUrlElement = element.selectFirst(chapterListUrlElement);
                String chapterUrl = "";
                if (chapterUrlElement != null) {
                    chapterUrl = chapterUrlElement.attr("href");
                }
                // title
                Element chapterTitleElement = element.selectFirst(chapterListTitleElement);
                String chapterTitle = "";
                if (chapterTitleElement != null) {
                    chapterTitle = chapterTitleElement.text();
                }
                tempChapterList.add(chapterTitle);
                try {
                    // 转化url路径，将相对路径转化成绝对路径
                    chapterUrl = UrlUtil.buildFullURL(location, chapterUrl);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                tempChapterUrlList.add(chapterUrl);
            }

            isSuccess = true;
        }

        List<String> finalTempChapterUrlList = tempChapterUrlList;
        return new HashMap<>() {{
            put("chapterList", tempChapterList);
            put("chapterUrlList", finalTempChapterUrlList);
        }};
    }

    /**
     * 构建搜索书籍目录列表窗口
     */
    private void buildBookDirectoryDialog(List<String> tempChapterList, List<String> tempChapterUrlList) {
        // 重置编辑器消息垂直滚动条位置
        cacheService.setEditorMessageVerticalScrollValue(0);
        // 构建目录列表组件
        JBList<String> chapterListJBList = new JBList<>(tempChapterList);
        // 设置单选模式
        chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 选择监听
        chapterListJBList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SiteBean tempSearchSiteBean = cacheService.getTempSelectedSiteBean();
                if (!selectedSiteBean.getId().equals(tempSearchSiteBean.getId())) {
                    selectedSiteBean = tempSearchSiteBean;
                    selectedSearchRules = selectedSiteBean.getSearchRules();
                    selectedListMainRules = selectedSiteBean.getListMainRules();
                    selectedChapterRules = selectedSiteBean.getChapterRules();
                    selectedBookInfoRules = selectedSiteBean.getBookInfoRules();
                    cacheService.setTempSelectedSiteBean(tempSearchSiteBean);
                }

                // 停止定时器
                executorServiceShutdown();
                // 停止语音
                stopTTS();
                // 重置编辑器消息垂直滚动条位置
                cacheService.setEditorMessageVerticalScrollValue(0);

                int selectedIndex = chapterListJBList.getSelectedIndex();
                currentChapterIndex = selectedIndex;
                String chapterTitle = tempChapterList.get(currentChapterIndex);
                String chapterSuffixUrl = tempChapterUrlList.get(selectedIndex);
                String chapterUrl;
                if (!chapterSuffixUrl.startsWith(ConstUtil.HTTP_SCHEME) &&
                        !chapterSuffixUrl.startsWith(ConstUtil.HTTPS_SCHEME) &&
                        !JsonUtil.isValid(selectBookInfo.getBookUrl())) {
                    String tempBaseUrl = cacheService.getTempSelectedSiteBean().getBaseUrl();
                    chapterUrl = tempBaseUrl + chapterSuffixUrl;
                } else {
                    chapterUrl = chapterSuffixUrl;
                }
                currentChapterInfo.setChapterTitle(chapterTitle);
                currentChapterInfo.setChapterUrl(chapterUrl);
                // 远程获取章节内容
                searchBookContentRemote(chapterUrl, (bodyElement) -> {
                    currentChapterInfo.setChapterContent(chapterContentHtml);
                    currentChapterInfo.setChapterContentStr(chapterContentText);
                    currentChapterInfo.setSelectedChapterIndex(currentChapterIndex);

                    chapterList = tempChapterList;
                    chapterUrlList = tempChapterUrlList;

                    // 缓存当前章节信息
                    handleCache();
                    // 更新内容
                    updateContentText();

                    loadThisChapterNextContent(chapterUrl, bodyElement);

                    // 设置数据加载模式
                    settings.setDataLoadType(Settings.DATA_LOAD_TYPE_NETWORK);
                    cacheService.setSettings(settings);
                });
            }
        });

        JBScrollPane jScrollPane = new JBScrollPane(chapterListJBList);
        jScrollPane.setPreferredSize(new Dimension(400, 500));
        MessageDialogUtil.showMessageDialog(mProject, "目录", jScrollPane,
                null, this::commCancelOperationHandle);
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

        boolean isCodeConfig = nextContentUrl.startsWith(ConstUtil.CODE_CONFIG_START_LABEL) &&
                nextContentUrl.endsWith(ConstUtil.CODE_CONFIG_END_LABEL);
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
                    String preContentUrlTemp = "";
                    AtomicReference<String> prePageContent = new AtomicReference<>(bodyElement.html());
                    int pageCount = 0;
                    while (isRunning && StringUtils.isNotEmpty(returnResult)) {
                        // 检查用户是否取消了操作
                        progressIndicator.checkCanceled();

                        // 更新进度信息
                        pageCount += 1;
                        progressIndicator.setText("正在加载第 " + pageCount + " 页...");

                        // 执行动态代码
                        returnResult = (String) DynamicCodeExecutor.executeMethod(
                                nextContentUrl,
                                "execute",
                                new Class[]{String.class, int.class, String.class, String.class},
                                new Object[]{chapterUrl, pageCount, preContentUrlTemp, prePageContent.get(),}
                        );

                        preContentUrlTemp = returnResult;

                        if (StringUtils.isNotEmpty(returnResult)) {
                            // 在 UI 线程中插入内容
                            nextContent.append(requestContent(returnResult, prePageContent::set));
                        }

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
                    String text = nextContent.toString();
                    int caretPosition = contentTextPanel.getCaretPosition();
                    text = getContent(text);
                    contentTextPanel.setText(text);
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

                    ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
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
        searchBookContentRemote(chapterUrl, (bodyElement) -> {
            // 初始化并缓存当前章节信息
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);
            if (listener != null) {
                listener.onClickItem(selectedIndex, chapterList, currentChapterInfo, bodyElement);
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
    public void searchBookContentRemote(String url, Consumer<Element> call) {
        new Task.Backgroundable(mProject, "【W-Reader】正在获取内容...") {
            String chapterContent = "";

            Element bodyElement;

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

                call.accept(bodyElement);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                error.printStackTrace();
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            }
        }.queue();
    }

    /**
     * 上一页
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
                searchBookContentRemote(prevChapterUrl, (bodyElement) -> {
                    // 初始化并缓存章节信息
                    currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
                    cacheService.setSelectedChapterInfo(currentChapterInfo);

                    runnable.accept(currentChapterInfo, bodyElement);
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
     * 下一页
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
                searchBookContentRemote(nextChapterUrl, (bodyElement) -> {
                    // 初始化并缓存章节信息
                    currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex);
                    cacheService.setSelectedChapterInfo(currentChapterInfo);

                    // 标记为已成功切换下一个章节
                    IS_SWITCH_NEXT_CHAPTER_SUCCESS = true;

                    runnable.accept(currentChapterInfo, bodyElement);
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
        if (chapterContentHandleRule.startsWith(ConstUtil.CODE_CONFIG_START_LABEL) &&
                chapterContentHandleRule.endsWith(ConstUtil.CODE_CONFIG_END_LABEL)) {

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
        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        // 设置内容
        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";


        return "<h3 style=\"text-align: center;margin-bottom: 20px;color:" + fontColorHex + ";\">" +
                selectedChapterInfo.getChapterTitle() + "</h3>" + "<div style=\"" + style + "\">" + text + "</div>";
    }
}
