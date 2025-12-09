package com.wei.wreader.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
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
import com.wei.wreader.utils.http.HttpRequestConfigParser;
import com.wei.wreader.utils.http.HttpUtil;
import com.wei.wreader.utils.ui.MessageDialogUtil;
import com.wei.wreader.utils.ui.ToolWindowUtil;
import com.wei.wreader.utils.yml.ConfigYaml;
import com.wei.wreader.widget.WReaderStatusBarWidget;
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

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.wei.wreader.utils.OperateActionUtil.*;
import static com.wei.wreader.utils.ui.ToolWindowUtil.updateContentText;

public class SearchBookUtil {
    private static ConfigYaml configYaml;
    private static CacheService cacheService;
    private static CustomSiteRuleCacheServer customSiteRuleCacheServer;
    private CustomSiteUtil customSiteUtil;
    private Settings settings;
    private static Project mProject;

    private Task.Backgroundable nextContentTask;
    private List<SiteBean> siteBeanList;
    /**
     * 搜索小说对话框
     */
    private DialogBuilder searchBookDialogBuilder;

    private int selectedBookSiteIndex;
    /**
     * 书本名称列表
     */
    private List<String> bookNameList = new ArrayList<>();
    /**
     * 书本信息列表
     */
    private List<BookInfo> bookInfoList = new ArrayList<>();
    private String contentOriginalStyle;

    public SearchBookUtil(Project project) {
        mProject = project;
        configYaml = new ConfigYaml();
        cacheService = CacheService.getInstance();
        customSiteUtil = CustomSiteUtil.getInstance(project);
        customSiteRuleCacheServer = CustomSiteRuleCacheServer.getInstance();
        // settings
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }
        if (StringUtils.isBlank(settings.getCharset())) {
            settings.setCharset(configYaml.getSettings().getCharset());
        }

    }

    /**
     * 构建搜索弹出窗口
     */
    public void buildSearchBookDialog() {
        SwingUtilities.invokeLater(() -> {
            String selectedCustomSiteRuleKey = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();
            if (StringUtils.isBlank(selectedCustomSiteRuleKey) || ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(selectedCustomSiteRuleKey)) {
                // 站点列表信息
                siteBeanList = configYaml.getSiteList();
            } else {
                // 站点列表信息
                Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
                siteBeanList = siteMap.get(selectedCustomSiteRuleKey);
            }

            Integer selectedBookSiteIndexTemp = cacheService.getSelectedBookSiteIndex();
            if (selectedBookSiteIndexTemp == null) {
                selectedBookSiteIndex = 0;
                cacheService.setSelectedBookSiteIndex(0);
            } else {
                selectedBookSiteIndex = selectedBookSiteIndexTemp;
            }

            // 创建一个搜索弹出窗
            // 书源列表下拉框
            ComboBox<String> siteListComboBox = buildSiteComboBox();
            // 书源分组下拉框
            ComboBox<String> siteGroupComboBox = buildSiteGroupComboBox(siteListComboBox);
            // 搜索框
            JTextField searchBookTextField = new JTextField(20);
            Object[] objs = {"书源分组", siteGroupComboBox, ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT, siteListComboBox, searchBookTextField};
            searchBookDialogBuilder = MessageDialogUtil.showMessageDialog(mProject, ConstUtil.WREADER_SEARCH_BOOK_TITLE, objs,
                    () -> searchBookDialogOk(siteListComboBox, searchBookTextField),
                    this::commCancelOperationHandle);
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
            String selectSiteGroupName = (String) e.getItem();
            siteBeanList = siteGroupMap.get(selectSiteGroupName);
            // 刷新书源列表下拉框
            siteListComboBox.removeAllItems();
            for (SiteBean site : siteBeanList) {
                siteListComboBox.addItem(site.getName() + "(" + site.getId() + ")");
            }
            siteListComboBox.setSelectedItem(0);
            selectedBookSiteIndex = 0;
            siteListComboBox.setSelectedIndex(selectedBookSiteIndex);

            customSiteRuleCacheServer.setTempSelectedCustomSiteRuleKey(selectSiteGroupName);
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
        SiteBean selectedSiteBean = siteBeanList.get(selectedIndex);
        // 缓存临时搜索站点信息
        cacheService.setTempSelectedBookSiteIndex(selectedIndex);
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
                        BookInfo selectBookInfo = bookInfoList.get(selectedIndex);
                        cacheService.setTempSelectedBookInfo(selectBookInfo);

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
        BookInfo tempSelectedBookInfo = cacheService.getTempSelectedBookInfo();
        SiteBean searchTempSelectedSiteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules searchTempListMainRules = searchTempSelectedSiteBean.getListMainRules();
        String listMainUrl = searchTempListMainRules.getUrl();
        String listMainUrlDataRule = searchTempListMainRules.getUrlDataRule();
        // 获取目录列表元素
        // 当使用api接口获取目录列表，且小说目录列表JSONPath规则不为空时，则使用api接口获取，
        // 反之使用html页面获取
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
                                put("bookId", tempSelectedBookInfo.getBookId());
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
                                    new Object[]{paramMap, itemIndexList, tempSelectedBookInfo.getBookId(), itemIdList});
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
        // 小说目录选择监听事件
        chapterListJBList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SiteBean tempSearchSiteBean = cacheService.getTempSelectedSiteBean();
                cacheService.setTempSelectedSiteBean(tempSearchSiteBean);
                OperateActionUtil operateAction = OperateActionUtil.getInstance(mProject);
                // 停止自动阅读定时器
                operateAction.executorServiceShutdown();
                // 停止语音
                operateAction.stopTTS();
                // 重置编辑器消息垂直滚动条位置
                cacheService.setEditorMessageVerticalScrollValue(0);

                BookInfo selectBookInfo = cacheService.getTempSelectedBookInfo();
                ChapterInfo selectChapterInfo = cacheService.getSelectedChapterInfo();
                int selectedIndex = chapterListJBList.getSelectedIndex();
                String chapterTitle = tempChapterList.get(selectedIndex);
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
                selectChapterInfo.setChapterTitle(chapterTitle);
                selectChapterInfo.setChapterUrl(chapterUrl);
                // 远程获取章节内容
                searchBookContentRemote(chapterUrl, (searchBookContent) -> {
                    selectChapterInfo.setChapterContent(searchBookContent.getChapterContentHtml());
                    selectChapterInfo.setChapterContentStr(searchBookContent.getChapterContentText());
                    selectChapterInfo.setSelectedChapterIndex(selectedIndex);

                    // 缓存当前章节信息：将临时信息缓存转成正式的已选择的信息缓存
                    // 选择的站点信息缓存
                    SiteBean tempSelectedSiteBean = cacheService.getTempSelectedSiteBean();
                    Integer tempSelectedBookSiteIndex = cacheService.getTempSelectedBookSiteIndex();
                    cacheService.setSelectedSiteBean(tempSelectedSiteBean);
                    cacheService.setSelectedBookSiteIndex(tempSelectedBookSiteIndex);
                    // 选择的小说信息缓存
                    BookInfo tempSelectedBookInfo = cacheService.getTempSelectedBookInfo();
                    cacheService.setSelectedBookInfo(tempSelectedBookInfo);
                    // 小说目录缓存
                    cacheService.setChapterList(tempChapterList);
                    cacheService.setChapterUrlList(tempChapterUrlList);
                    // 选择的小说章节缓存
                    cacheService.setSelectedChapterInfo(selectChapterInfo);
                    // 自定义站点规则缓存
                    String tempSelectedCustomSiteRuleKey = customSiteRuleCacheServer.getTempSelectedCustomSiteRuleKey();
                    customSiteRuleCacheServer.setSelectedCustomSiteRuleKey(tempSelectedCustomSiteRuleKey);

                    // 更新内容
                    updateContentText();

                    loadThisChapterNextContent(chapterUrl, searchBookContent.getBodyElement());

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
     * 远程获取小说内容
     *
     * @param url
     * @param call 获取成功后执行的方法
     * @throws IOException
     */
    public void searchBookContentRemote(String url, Consumer<SearchBookContent> call) {
        new Task.Backgroundable(mProject, "【W-Reader】正在获取内容...") {
            String chapterContent = "";

            Element bodyElement;
            String chapterContentText;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // 在后台线程执行耗时操作
                progressIndicator.setText("【W-Reader】正在获取内容...");
                // 设置进度条为不确定模式
                progressIndicator.setIndeterminate(true);

                SiteBean tempSelectedSiteBean = cacheService.getTempSelectedSiteBean();
                ChapterRules tempSelectedChapterRules = tempSelectedSiteBean.getChapterRules();
                String chapterContentUrl = tempSelectedChapterRules.getUrl();
                String chapterContentUrlDataRule = tempSelectedChapterRules.getUrlDataRule();
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
                        Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                        throw new RuntimeException(e);
                    }
                    // 头部
                    Element headElement = document.head();
                    // 获取页面原样式
                    boolean isContentOriginalStyle = tempSelectedChapterRules.isUseContentOriginalStyle();
                    if (isContentOriginalStyle) {
                        // 获取页面<style></style>中的CSS样式
                        StringBuilder allStyle = new StringBuilder();
                        Elements styles = headElement.getElementsByTag("style");
                        for (Element style : styles) {
                            String styleText = style.html();
                            String replacement = tempSelectedChapterRules.getReplaceContentOriginalRegex();
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
                    String chapterContentElementName = tempSelectedChapterRules.getContentElementName();
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
                String fontColorHex = cacheService.getFontColorHex();
                ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();

                // 处理内容
                try {
                    chapterContent = handleContent(chapterContent);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                chapterContent = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" + fontColorHex + ";\">" +
                        selectedChapterInfo.getChapterTitle() + "</h3>" + chapterContent;
                Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
                chapterContentText = pattern.matcher(chapterContent).replaceAll("　");
                chapterContentText = StringUtils.normalizeSpace(chapterContentText);
                chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);

                SearchBookContent searchBookContent = new SearchBookContent();
                searchBookContent.setBodyElement(bodyElement);
                searchBookContent.setChapterContentHtml(chapterContent);
                searchBookContent.setChapterContentText(chapterContentText);
                call.accept(searchBookContent);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                error.printStackTrace();
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            }
        }.queue();
    }

    /**
     * 处理小说内容
     *
     * @param content 内容处理规则配置信息 or 小说内容
     * @return
     */
    public String handleContent(String content) throws Exception {
        String result = "";
        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
        ChapterRules selectedChapterRules = selectedSiteBean.getChapterRules();
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
     * 加载本章节下一页的内容
     *
     * @param bodyElement 文章内容html页面{@code <body></body>}部分的元素
     */
    public void loadThisChapterNextContent(String chapterUrl, Element bodyElement) {
        if (settings.getDisplayType() != Settings.DATA_LOAD_TYPE_NETWORK) {
            return;
        }

        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
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
                        progressIndicator.setText2("正在加载第 " + pageCount + " 页...");

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

    private String requestContent(String url, Consumer<String> call) {
        String content = "";
        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
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
                        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
                        // 设置内容
                        String fontColorHex = cacheService.getFontColorHex();
                        String fontFamily = cacheService.getFontFamily();
                        int fontSize = cacheService.getFontSize();
                        String chapterContent = cacheService.getSelectedChapterInfo().getChapterContent();
                        // 是否使用原网页的css样式
                        boolean isContentOriginalStyle = selectedSiteBean.getChapterRules().isUseContentOriginalStyle();
                        if (isContentOriginalStyle) {
                            // 设置内容
                            chapterContent = String.format("""
                                            <div class="%s" style="color:%s;font-size:%dpx;">%s</div>
                                            """,
                                    ConstUtil.NEW_FONT_CLASS_NAME, fontColorHex, fontSize, chapterContent);

                            // 构建完整html结构
                            BookInfo selectedBookInfo = cacheService.getSelectedBookInfo();
                            chapterContent = StringUtil.buildFullHtml(selectedBookInfo.getBookName(), contentOriginalStyle,
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

    /**
     * 取消操作时的通用操作
     */
    private void commCancelOperationHandle() {
    }

}
