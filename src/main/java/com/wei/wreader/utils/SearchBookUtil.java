package com.wei.wreader.utils;

import java.util.ArrayList;

import com.google.gson.*;
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
import com.jayway.jsonpath.JsonPath;
import com.wei.wreader.pojo.*;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.CustomSiteRuleCacheServer;
import com.wei.wreader.utils.comm.*;
import com.wei.wreader.utils.comm.script.RhinoJsEngine;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.wei.wreader.utils.ui.ToolWindowUtil.updateContentText;

public class SearchBookUtil {
    private static ConfigYaml configYaml;
    private static CacheService cacheService;
    private static CustomSiteRuleCacheServer customSiteRuleCacheServer;
    private CustomSiteUtil customSiteUtil;
    private Settings settings;
    private static Project mProject;

    private Task.Backgroundable nextListMainTask;
    private Task.Backgroundable nextContentTask;
    private List<SiteBean> siteBeanList;
    /**
     * 搜索小说对话框
     */
    private DialogBuilder searchBookDialogBuilder;
    /**
     * 目录列表控件
     */
    private JBList<LabelValueItem> chapterListJBList;
    private DefaultListModel<LabelValueItem> chapterListModel;

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
        String searchKey = searchBookTextField.getText();
        if (searchKey == null || searchKey.trim().isEmpty()) {
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

        // **** 判断配置类型 ****
        boolean javaCodeConfig = ScriptCodeUtil.isJavaCodeConfig(searchUrl);
        if (javaCodeConfig) {
            try {
                searchBookUrl = (String) ScriptCodeUtil.getScriptCodeExeResult(searchUrl,
                        new Class<?>[]{String.class, String.class},
                        new Object[]{searchKey, "1"},
                        new HashMap<>() {{
                            put("key", searchKey);
                            put("page", 1);
                        }}
                );
            } catch (Exception e) {
                e.printStackTrace();
                Messages.showMessageDialog(ConstUtil.WREADER_ERROR, "提示", Messages.getInformationIcon());
            }
        }
        // 模板引擎配置
        else {
            // 使用模板引擎匹配参数
            searchUrl = StringTemplateEngine.render(searchUrl, new HashMap<>() {{
                put("key", searchKey);
                put("page", 1);
            }});

            searchBookUrl = searchUrl;
        }

        // **** 创建后台任务，并执行 ****
        this.searchBookBackTask(searchBookUrl);
    }

    /**
     * 创建搜索小说后台任务
     *
     * @param searchBookUrl 搜索小说的URL
     */
    private void searchBookBackTask(String searchBookUrl) {

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
                    searchBookResult = searchBookList(searchBookUrl);
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
        SiteBean tempSearchSelectedSiteBean = cacheService.getTempSelectedSiteBean();
        SearchRules tempSearchSearchRules = tempSearchSelectedSiteBean.getSearchRules();
        BookInfoRules tempSearchBookInfoRules = tempSearchSelectedSiteBean.getBookInfoRules();

        // 获取小说列表的接口返回的是否是html
        boolean isHtml = tempSearchSelectedSiteBean.isHasHtml();
        if (isHtml) {
            return this.searchBookListHtml(url, tempSearchSearchRules, tempSearchBookInfoRules);
        } else {
            return this.searchBookListApi(url, tempSearchSearchRules);
        }
    }

    /**
     * 搜索小说列表--html页面
     *
     * @param url                 请求链接
     * @param searchSearchRules   搜索规则
     * @param searchBookInfoRules 书本信息规则
     * @return 小说列表
     */
    private String searchBookListHtml(String url,
                                      SearchRules searchSearchRules,
                                      BookInfoRules searchBookInfoRules) {
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

        // 获取小说列表链接元素CssSelector
        String bookListUrlElement = searchSearchRules.getBookListUrlElement();
        bookListUrlElement = StringUtils.isBlank(bookListUrlElement) ? "a" : bookListUrlElement;
        // 获取小说列表链接元素CssSelector规则
        String[] bookListUrlElementRules = bookListUrlElement.split("@");
        String bookListUrlCssSelectorRule = bookListUrlElementRules[0];
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

        // 获取小说列表标题元素CssSelector
        JsonArray jsonArray = new JsonArray();
        Elements elements = document.select(searchSearchRules.getBookListElementName());
        String location = document.location();
        String bookListTitleElement = searchSearchRules.getBookListTitleElement();
        for (Element itemElement : elements) {
            if (itemElement != null) {
                // url
                Element bookUrlElement = itemElement.selectFirst(bookListUrlCssSelectorRule);
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
                jsonObject.addProperty(searchBookInfoRules.getBookNameField(), bookName);
                jsonObject.addProperty(searchBookInfoRules.getBookUrlField(), bookUrl);
                jsonObject.addProperty(searchBookInfoRules.getBookAuthorField(), "");
                jsonObject.addProperty(searchBookInfoRules.getBookDescField(), "");
                jsonObject.addProperty(searchBookInfoRules.getBookImgUrlField(), "");
                jsonArray.add(jsonObject);
            }
        }
        return jsonArray.toString();

    }

    /**
     * 搜索小说列表--api接口
     *
     * @param url               请求链接
     * @param searchSearchRules 搜索规则
     * @return
     */
    private String searchBookListApi(String url, SearchRules searchSearchRules) {
        String result = "";
        HttpRequestBase requestBase = HttpUtil.commonRequest(url);
        requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
        try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = httpResponse.getEntity();
                result = EntityUtils.toString(entity);

                // 使用jsonpath提取小说列表
                Object readJsonObject = JsonPath.read(result, searchSearchRules.getDataBookListRule());
                result = Objects.isNull(readJsonObject) ? "" : readJsonObject.toString();
            }
        } catch (IOException e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
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

            if (jsonArray != null && !jsonArray.isEmpty()) {
                // 获取书本信息列表
                for (int i = 0, len = jsonArray.size(); i < len; i++) {
                    JsonObject asJsonObject = jsonArray.get(i).getAsJsonObject();

                    // 获取信息
                    String bookId = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookIdField());
                    String articleName = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookNameField());
                    String author = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookAuthorField());
                    String intro = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookDescField());
                    String urlImg = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookImgUrlField());
                    String urlList = JsonUtil.getString(asJsonObject, tempSearchBookInfoRules.getBookUrlField());

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
                    this.bookListSelection(e, searchBookList, tempSearchSelectedSiteBean, tempSearchSearchRules);
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
     * handleBookList--小说列表项选择事件
     *
     * @param e
     * @param searchBookList
     * @param searchSelectedSiteBean
     * @param searchSearchRules
     */
    private void bookListSelection(ListSelectionEvent e,
                                   JBList<String> searchBookList,
                                   SiteBean searchSelectedSiteBean,
                                   SearchRules searchSearchRules) {

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

            // **** 判断代码配置类型，并获取请求链接 ****
            String searchListMainUrl = this.getListMainUrl(listMainUrl, searchSearchRules,
                    selectBookInfo, searchSelectedSiteBean);

            // 搜索小说目录
            this.getListMainBackTask(searchListMainUrl);
            cacheService.setChapterContentList(null);
        }
    }

    /**
     * handleBookList--判断代码配置类型，并获取小说目录的请求链接
     *
     * @param url                    小说目录的请求链接字符串模板或代码配置
     * @param searchSearchRules      搜索规则
     * @param selectBookInfo         选择的图书信息
     * @param searchSelectedSiteBean 搜索的书源规则信息
     * @return
     */
    private String getListMainUrl(String url,
                                  SearchRules searchSearchRules,
                                  BookInfo selectBookInfo,
                                  SiteBean searchSelectedSiteBean) {
        String listMainUrl = "";
        boolean javaCodeConfig = ScriptCodeUtil.isJavaCodeConfig(url);
        if (javaCodeConfig) {
            Class<?>[] paramTypes = new Class[]{};
            Object[] params = new Object[]{};
            if (url.contains("com.wei.wreader.pojo.BookInfo")) {
                paramTypes = new Class[]{BookInfo.class};
                params = new Object[]{selectBookInfo};
            } else {
                paramTypes = new Class[]{String.class};
                params = new Object[]{selectBookInfo.getBookId()};
            }
            try {
                listMainUrl = (String) ScriptCodeUtil.getScriptCodeExeResult(url, paramTypes, params, new HashMap<>() {{
                    put("bookInfo", selectBookInfo);
                }});
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        // 模板引擎配置
        else {
            // 判断获取小说目录的方式：调用api接口或者html页面获取
            boolean isHtml = searchSelectedSiteBean.isHasHtml();
            if (StringUtils.isNotBlank(url) && !isHtml) {
                listMainUrl = StringTemplateEngine.render(url, new HashMap<>() {{
                    put("bookId", selectBookInfo.getBookId());
                }});
            } else {
                listMainUrl = selectBookInfo.getBookUrl();
                if (!selectBookInfo.getBookUrl().startsWith(ConstUtil.HTTP_SCHEME) &&
                        !selectBookInfo.getBookUrl().startsWith(ConstUtil.HTTPS_SCHEME) &&
                        !JsonUtil.isValid(selectBookInfo.getBookUrl())) {
                    listMainUrl = searchSearchRules.getUrl() + selectBookInfo.getBookUrl();
                }
            }
        }

        return listMainUrl;
    }

    /**
     * handleBookList--创建获取小说目录后台任务
     *
     * @param url 小说目录链接
     */
    private void getListMainBackTask(String url) {
        Task.Backgroundable backgroundable = new Task.Backgroundable(mProject, "【W-Reader】正在获取小说目录...") {
            boolean isSuccess = false;
            List<String> tempChapterList = new ArrayList<>();
            List<String> tempChapterUrlList = new ArrayList<>();
            String bodyStr = "";
            Element bodyElement = null;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // 在后台线程执行耗时操作
                progressIndicator.setText("【W-Reader】正在获取小说目录...");
                // 设置进度条为不确定模式
                progressIndicator.setIndeterminate(true);

                Map<String, Object> chapterMap = searchBookDirectory(url);
                if (chapterMap != null && !chapterMap.isEmpty()) {
                    isSuccess = true;
                    tempChapterList = (List<String>) chapterMap.get("chapterList");
                    tempChapterUrlList = (List<String>) chapterMap.get("chapterUrlList");
                    bodyStr = (String) chapterMap.get("bodyStr");
                    bodyElement = (Element) chapterMap.get("bodyElement");
                }
            }

            @Override
            public void onSuccess() {
                if (isSuccess) {
                    SearchBookCallParam callParam = new SearchBookCallParam();
                    callParam.setBodyElement(bodyElement);
                    callParam.setBodyContentStr(bodyStr);
                    callParam.setTempChapterList(tempChapterList);
                    callParam.setTempChapterUrlList(tempChapterUrlList);
                    buildBookDirectoryDialog(callParam);
                } else {
                    Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                error.printStackTrace();
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            }
        };
        backgroundable.queue();
    }

    /**
     * 获取小说目录
     *
     * @param url
     * @throws IOException
     */
    public Map<String, Object> searchBookDirectory(String url) {
        SiteBean searchTempSelectedSiteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules searchTempListMainRules = searchTempSelectedSiteBean.getListMainRules();
        String listMainUrl = searchTempListMainRules.getUrl();
        String listMainUrlDataRule = searchTempListMainRules.getUrlDataRule();

        // 获取目录列表元素
        // 当使用api接口获取目录列表，且小说目录列表JSONPath规则不为空时，则使用api接口获取，
        // 反之使用html页面获取
        if (StringUtils.isNotBlank(listMainUrl) && StringUtils.isNotBlank(listMainUrlDataRule)) {
            return this.searchBookDirectoryHtml(url, searchTempSelectedSiteBean, searchTempListMainRules);
        } else {
            return this.searchBookDirectoryApi(url, searchTempListMainRules);
        }
    }

    /**
     * 获取目录列表--html
     *
     * @param url                        请求链接
     * @param searchTempSelectedSiteBean 选中的站点信息--临时缓存搜索前的站点信息
     * @param searchTempListMainRules    目录规则
     * @return
     */
    private Map<String, Object> searchBookDirectoryHtml(String url,
                                                        SiteBean searchTempSelectedSiteBean,
                                                        ListMainRules searchTempListMainRules) {
        String listMainBodyStr = "";
        List<String> tempChapterList = new ArrayList<>();
        List<String> tempChapterUrlList = new ArrayList<>();

        BookInfo selectedBookInfo = cacheService.getTempSelectedBookInfo();
        String listMainUrlDataRule = searchTempListMainRules.getUrlDataRule();

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
                boolean javaCodeConfig = ScriptCodeUtil.isJavaCodeConfig(chapterUrl);

                List<String> itemIdList = new ArrayList<>();
                List<Integer> itemIndexList = new ArrayList<>();
                for (int i = 0, len = listMainJsonArray.size(); i < len; i++) {
                    JsonObject jsonObject = listMainJsonArray.get(i).getAsJsonObject();
                    String itemId = jsonObject.get(listMainItemIdField).getAsString();
                    String title = jsonObject.get(listMainItemTitleField).getAsString();
                    String itemChapterUrl = "";
                    // 当chapterUrl符合规则时，则将itemId和index加入集合中，反正视为普通的API请求地址
                    if (javaCodeConfig) {
                        itemIdList.add(itemId);
                        itemIndexList.add(i);
                        tempChapterList.add(title);
                    } else {
                        if (StringUtils.isNotBlank(chapterUrl)) {
                            itemChapterUrl = StringTemplateEngine.render(chapterUrl, new HashMap<>() {{
                                put("bookId", selectedBookInfo.getBookId());
                                put("itemId", itemId);
                            }});
                        } else {
                            String listMainItemUrlField = searchTempListMainRules.getItemUrlField();
                            itemChapterUrl = jsonObject.get(listMainItemUrlField).getAsString();
                        }

                        tempChapterList.add(title);
                        tempChapterUrlList.add(itemChapterUrl);
                    }
                }

                // 当chapterUrl符合规则时，视为Java代码，并执行动态代码
                if (javaCodeConfig) {
                    try {
                        tempChapterUrlList = ScriptCodeUtil.getScriptCodeExeListResult(
                                chapterUrl,
                                new Class[]{Map.class, List.class, String.class, List.class},
                                new Object[]{paramMap, itemIndexList, selectedBookInfo.getBookId(), itemIdList},
                                Map.of(
                                        "result", result,
                                        "bookInfo", selectedBookInfo,
                                        "itemIndexList", itemIndexList,
                                        "itemIdList", itemIdList
                                )
                        );
                    } catch (Exception e1) {
                        Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
                        throw new RuntimeException(e1);
                    }
                }

                listMainBodyStr = result;
            }
        } catch (IOException e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }

        return Map.of(
                "bodyStr", listMainBodyStr,
                "bodyElement", "",
                "chapterList", tempChapterList,
                "chapterUrlList", tempChapterUrlList
        );
    }

    /**
     * 获取目录列表--API
     *
     * @param url                     请求链接
     * @param searchTempListMainRules 目录规则
     * @return
     */
    private Map<String, Object> searchBookDirectoryApi(String url, ListMainRules searchTempListMainRules) {
        String listMainBodyStr = "";
        Element listMainBodyElement = null;
        List<String> tempChapterList = new ArrayList<>();
        List<String> tempChapterUrlList = new ArrayList<>();

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
        listMainBodyElement = document.body();
        listMainBodyStr = document.body().html();
        // 目录链接元素CssSelector
        String chapterListUrlElement = searchTempListMainRules.getUrlElement();
        chapterListUrlElement = StringUtils.isBlank(chapterListUrlElement) ? "a" : chapterListUrlElement;
        // 目录标题元素CssSelector
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

        return Map.of(
                "bodyStr", listMainBodyStr,
                "bodyElement", listMainBodyElement,
                "chapterList", tempChapterList,
                "chapterUrlList", tempChapterUrlList
        );
    }

    /**
     * 加载下一页目录
     *
     * @param bodyElementStr html页面{@code <body></body>}部分的元素 OR API接口返回的内容
     */
    public void loadNextListMain(String bodyElementStr, Element bodyElement, Runnable runnable) {
        SiteBean selectedSiteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules listMainRule = selectedSiteBean.getListMainRules();
        if (listMainRule == null) {
            return;
        }

        String nextListMainUrl = listMainRule.getNextListMainUrl();
        if (StringUtils.isEmpty(nextListMainUrl)) {
            if (runnable != null) {
                runnable.run();
            }
            return;
        }

        boolean isCodeConfig = ScriptCodeUtil.isJavaCodeConfig(nextListMainUrl);
        if (!isCodeConfig) {
            return;
        }

        if (nextListMainTask != null) {
            nextListMainTask.onCancel();
        }

        AtomicReference<String> finalBodyElementStr = new AtomicReference<>(bodyElementStr);

        nextListMainTask = new Task.Backgroundable(mProject, "【W-Reader】加载下一页目录...") {
            private volatile boolean isRunning = true;
            private String returnResult = nextListMainUrl;
            private Element finalBodyElement = bodyElement;
            private final List<String> tempChapterList = new ArrayList<>();
            private final List<String> tempChapterUrlList = new ArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // 设置进度条为不确定模式
                progressIndicator.setIndeterminate(true);

                try {
                    String preListMainUrlTemp = "";
                    int pageIndex = 1;
                    String baseUrl = selectedSiteBean.getBaseUrl();
                    while (isRunning) {
                        // 检查用户是否取消了操作
                        progressIndicator.checkCanceled();
                        // 更新进度信息
                        pageIndex += 1;
                        progressIndicator.setText2("正在加载第" + pageIndex + "页...");

                        // 执行动态代码，获取下一页目录的链接（五个参数：基础网址、当前页码、上一页Url、主体信息字符串、页面主体元素对象）
                        returnResult = (String) ScriptCodeUtil.getScriptCodeExeResult(
                                nextListMainUrl,
                                new Class[]{String.class, Integer.class, String.class, String.class, Element.class},
                                new Object[]{baseUrl, pageIndex, preListMainUrlTemp, finalBodyElementStr.get(), finalBodyElement},
                                Map.of(
                                        "baseUrl", baseUrl,
                                        "pageIndex", pageIndex,
                                        "preUrl", preListMainUrlTemp,
                                        "bodyElementStr", finalBodyElementStr.get(),
                                        "bodyContent", finalBodyElement
                                )
                        );

                        preListMainUrlTemp = returnResult;

                        // 若返回结果为空，则停止加载，结束进程
                        if (StringUtils.isEmpty(returnResult)) {
                            isRunning = false;
                            break;
                        }

                        Map<String, List<String>> listMap = requestNextListMain(returnResult, (searchBookCallParam) -> {
                            finalBodyElementStr.set(searchBookCallParam.getBodyContentStr());
                            finalBodyElement = searchBookCallParam.getBodyElement();
                        });
                        tempChapterList.addAll(listMap.get("chapterList"));
                        tempChapterUrlList.addAll(listMap.get("chapterUrlList"));

                        // 添加延迟，避免过于频繁的请求
                        Thread.sleep(1000);
                    }
                } catch (ProcessCanceledException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                "下一页目录加载失败: " + e.getMessage(),
                                "提示"
                        );
                    });
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                buildBookDirectoryDialog(tempChapterList, tempChapterUrlList, true);
                if (runnable != null) {
                    runnable.run();
                }
            }

            @Override
            public void onCancel() {
                isRunning = false;
                super.onCancel();
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                super.onThrowable(error);
            }
        };
        nextListMainTask.queue();
    }

    /**
     * 请求下一页目录
     *
     * @param url  请求的url
     * @param call 回调函数
     * @return 返回结果
     */
    public Map<String, List<String>> requestNextListMain(String url, Consumer<SearchBookCallParam> call) {
        SiteBean selectedSiteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules listMainRule = selectedSiteBean.getListMainRules();
        try {
            // 使用API接口请求下一页目录
            if (listMainRule.isUseNextListMainApi()) {
                return this.requestNextListMainApi(url, listMainRule, call);
            }
            // 解析HTML页面，请求下一页目录
            else {
                return this.requestNextListMainHtml(url, listMainRule, call);
            }
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            call.accept(new SearchBookCallParam());
            e.printStackTrace();
        }

        return Map.of(
                "chapterList", new ArrayList<>(),
                "chapterUrlList", new ArrayList<>()
        );
    }

    /**
     * 请求下一页目录--API接口方式
     *
     * @param url
     * @param listMainRule
     * @param call
     * @return
     */
    private Map<String, List<String>> requestNextListMainApi(String url,
                                                             ListMainRules listMainRule,
                                                             Consumer<SearchBookCallParam> call) {
        List<String> tempChapterTitleList = new ArrayList<>();
        List<String> tempChapterUrlList = new ArrayList<>();
        String bodyElementStr = "";
        Element bodyElement = null;
        SearchBookCallParam callParam = new SearchBookCallParam();

        HttpRequestBase requestBase = HttpUtil.commonRequest(url);
        requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
        try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(requestBase)) {
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);
                // 返回结果
                bodyElementStr = result;
                // 字符串转换JSON对象
                Gson gson = new Gson();
                JsonObject resJson = gson.fromJson(result, JsonObject.class);

                // 使用jsonpath对获取到的json进行处理
                List<Map<String, Object>> readJson = JsonPath.read(resJson.toString(), listMainRule.getNextListMainApiDataRule());
                for (Map<String, Object> itemMap : readJson) {
                    // 获取当前章节的数据
                    String itemId = (String) itemMap.get(listMainRule.getItemIdField());
                    String title = (String) itemMap.get(listMainRule.getItemTitleField());
                    String itemUrl = (String) itemMap.get(listMainRule.getItemUrlField());
                    // 获取当前章节的链接
                    if (StringUtils.isNotBlank(itemUrl)) {
                        tempChapterUrlList.add(itemUrl);
                    } else {
                        // 若itemUrl为空，则使用urlDataHandleRule执行动态代码，对数据进行处理，从而获取章节链接
                        String urlDataHandleRule = listMainRule.getUrlDataHandleRule();
                        itemUrl = (String) ScriptCodeUtil.getScriptCodeExeResult(
                                urlDataHandleRule,
                                new Class[]{String.class, Map.class, String.class},
                                new Object[]{result, itemMap, itemId},
                                new HashMap<>() {{
                                    put("result", result);
                                    put("itemMap", itemMap);
                                    put("itemId", itemId);
                                }}
                        );
                        tempChapterUrlList.add(itemUrl);
                    }
                    tempChapterTitleList.add(title);
                }
            }
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            call.accept(callParam);
            throw new RuntimeException(e);
        }

        callParam.setBodyElement(bodyElement);
        callParam.setBodyContentStr(bodyElementStr);
        call.accept(callParam);

        return Map.of(
                "chapterList", tempChapterTitleList,
                "chapterUrlList", tempChapterUrlList
        );
    }

    /**
     * 请求下一页目录--解析HTML页面方式
     *
     * @param url
     * @param listMainRule
     * @param call
     * @return
     */
    private Map<String, List<String>> requestNextListMainHtml(String url,
                                                              ListMainRules listMainRule,
                                                              Consumer<SearchBookCallParam> call) {
        List<String> tempChapterTitleList = new ArrayList<>();
        List<String> tempChapterUrlList = new ArrayList<>();
        String bodyElementStr = "";
        Element bodyElement = null;
        SearchBookCallParam callParam = new SearchBookCallParam();
        Document document = null;
        try {
            document = Jsoup.connect(url)
                    .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                    .get();
        } catch (IOException e) {
            Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            call.accept(callParam);
            throw new RuntimeException(e);
        }

        // 获取页面的地址
        String location = document.location();
        // 页面展示主体
        bodyElement = document.body();
        bodyElementStr = bodyElement.toString();
        // 获取小说目录
        String listMainElementName = listMainRule.getListMainElementName();
        Elements listMainElements = bodyElement.select(listMainElementName);
        if (listMainElements.isEmpty()) {
            return new HashMap<>();
        }

        // 获取目录的链接和标题
        String urlElementName = listMainRule.getUrlElement();
        urlElementName = StringUtils.isBlank(urlElementName) ? "a" : urlElementName;
        String titleElementName = listMainRule.getTitleElement();
        for (Element element : listMainElements) {
            // url
            Element chapterUrlElement = element.selectFirst(urlElementName);
            String chapterUrl = "";
            if (chapterUrlElement != null) {
                chapterUrl = chapterUrlElement.attr("href");
            }
            // title
            Element chapterTitleElement = element.selectFirst(titleElementName);
            String chapterTitle = "";
            if (chapterTitleElement != null) {
                chapterTitle = chapterTitleElement.text();
            }
            tempChapterTitleList.add(chapterTitle);
            try {
                // 转化url路径，将相对路径转化成绝对路径
                chapterUrl = UrlUtil.buildFullURL(location, chapterUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            tempChapterUrlList.add(chapterUrl);
        }


        callParam.setBodyElement(bodyElement);
        callParam.setBodyContentStr(bodyElementStr);
        call.accept(callParam);

        return Map.of(
                "chapterList", tempChapterTitleList,
                "chapterUrlList", tempChapterUrlList
        );
    }

    /**
     * 构建搜索书籍目录列表窗口
     */
    private void buildBookDirectoryDialog(SearchBookCallParam searchBookCallParam) {
        buildBookDirectoryDialog(
                searchBookCallParam.getTempChapterList(),
                searchBookCallParam.getTempChapterUrlList(),
                searchBookCallParam.getBodyElement(),
                searchBookCallParam.getBodyContentStr(),
                false);
    }

    /**
     * 构建搜索书籍目录列表窗口
     */
    private void buildBookDirectoryDialog(List<String> tempChapterList, List<String> tempChapterUrlList) {
        buildBookDirectoryDialog(tempChapterList, tempChapterUrlList, false);
    }

    /**
     * 构建搜索书籍目录列表窗口
     */
    private void buildBookDirectoryDialog(List<String> tempChapterList, List<String> tempChapterUrlList, boolean isUpdate) {
        buildBookDirectoryDialog(tempChapterList, tempChapterUrlList, null, null, isUpdate);
    }

    /**
     * 构建搜索书籍目录列表窗口
     *
     * @param tempChapterList    章节名称列表
     * @param tempChapterUrlList 章节链接列表（与相应章节名称的索引要对应上）
     * @param bodyElement        HTML页面的body元素对象
     * @param bodyContentStr     返回结果字符串；HTML页面：body元素内容的字符串；API请求：API返回结果字符串
     * @param isUpdate           是否为更新列表数据，当为true时，则只更新列表数据，不重新创建控件
     */
    private void buildBookDirectoryDialog(List<String> tempChapterList,
                                          List<String> tempChapterUrlList,
                                          Element bodyElement,
                                          String bodyContentStr,
                                          boolean isUpdate) {

        // 重置编辑器消息垂直滚动条位置
        cacheService.setEditorMessageVerticalScrollValue(0);
        if (isUpdate) {
            // 添加新的目录
            for (int i = 0, len = tempChapterList.size(); i < len; i++) {
                LabelValueItem labelValueItem = new LabelValueItem(tempChapterList.get(i), tempChapterUrlList.get(i));
                chapterListModel.addElement(labelValueItem);
            }

            // 移除现有的所有监听器，以避免使用旧的闭包变量
            ListSelectionListener[] listeners = chapterListJBList.getListSelectionListeners();
            for (ListSelectionListener listener : listeners) {
                chapterListJBList.removeListSelectionListener(listener);
            }

            List<String> tempChapterList2 = new ArrayList<>();
            List<String> tempChapterUrlList2 = new ArrayList<>();
            // 获取列表中已添加的所有项目
            for (int i = 0; i < chapterListModel.getSize(); i++) {
                LabelValueItem labelValueItem = (LabelValueItem) chapterListModel.getElementAt(i);
                tempChapterList2.add(labelValueItem.getLabel());
                tempChapterUrlList2.add((String) labelValueItem.getValue());
            }
            // 添加新的监听器，捕获当前的列表引用
            chapterListJBList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    handleChapterSelection(tempChapterList2, tempChapterUrlList2);
                }
            });
        } else {
            // 构建目录列表组件
            chapterListModel = new DefaultListModel<>();
            for (int i = 0, len = tempChapterList.size(); i < len; i++) {
                LabelValueItem labelValueItem = new LabelValueItem(tempChapterList.get(i), tempChapterUrlList.get(i));
                chapterListModel.addElement(labelValueItem);
            }
            chapterListJBList = new JBList<>(chapterListModel);
            // 设置单选模式
            chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // 小说目录选择监听器
            chapterListJBList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    handleChapterSelection(tempChapterList, tempChapterUrlList);
                }
            });
            // 设置自定义 Cell Renderer（只显示 label）
            chapterListJBList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected,
                                                              boolean cellHasFocus) {
                    // value 是 LabelValueItem 类型
                    if (value instanceof LabelValueItem) {
                        LabelValueItem item = (LabelValueItem) value;
                        super.getListCellRendererComponent(list, item.getLabel(), index, isSelected, cellHasFocus);
                    } else {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    }
                    return this;
                }
            });

            // 加载下一页目录
            loadNextListMain(bodyContentStr, bodyElement, () -> {
                JBScrollPane jScrollPane = new JBScrollPane(chapterListJBList);
                jScrollPane.setPreferredSize(new Dimension(400, 500));
                MessageDialogUtil.showMessageDialog(mProject, "目录", jScrollPane,
                        null, this::commCancelOperationHandle);
            });
        }
    }

    /**
     * 处理章节选择的逻辑
     *
     * @param tempChapterList    章节标题列表
     * @param tempChapterUrlList 章节URL列表
     */
    private void handleChapterSelection(List<String> tempChapterList, List<String> tempChapterUrlList) {
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
        searchBookContentRemote(chapterUrl, (searchBookCallParam) -> {
            selectChapterInfo.setChapterContent(searchBookCallParam.getChapterContentHtml());
            selectChapterInfo.setChapterContentStr(searchBookCallParam.getChapterContentText());
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

            loadThisChapterNextContent(chapterUrl, searchBookCallParam.getBodyContentStr());

            // 设置数据加载模式
            settings.setDataLoadType(Settings.DATA_LOAD_TYPE_NETWORK);
            cacheService.setSettings(settings);
        });
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
            String bodyContentStr;
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
                            bodyContentStr = result;
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

                    bodyContentStr = bodyElement.html();
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

                SearchBookCallParam searchBookCallParam = new SearchBookCallParam();
                searchBookCallParam.setBodyElement(bodyElement);
                searchBookCallParam.setBodyContentStr(bodyContentStr);
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
     * 处理小说内容
     *
     * @param content 内容处理规则配置信息 or 小说内容
     * @return
     */
    public String handleContent(String content) throws Exception {
        String result = "";
        SiteBean selectedSiteBean = cacheService.getTempSelectedSiteBean();
        ChapterRules selectedChapterRules = selectedSiteBean.getChapterRules();
        String chapterContentHandleRule = selectedChapterRules.getContentHandleRule();

        // 判断是否为动态代码
        boolean javaCodeConfig = ScriptCodeUtil.isJavaCodeConfig(chapterContentHandleRule);
        if (javaCodeConfig) {
            // 判断是否为旧版代码配置
            if (ScriptCodeUtil.isOldJavaCodeConfig(chapterContentHandleRule)) {
                // 兼容旧版的动态代码执行器
                chapterContentHandleRule = StringTemplateEngine.render(chapterContentHandleRule, new HashMap<>() {{
                    put("content", content);
                }});
                // 执行配置中的方法
                result = MethodExecutor.executeMethod(chapterContentHandleRule).toString();
            } else {
                result = (String) ScriptCodeUtil.getScriptCodeExeResult(
                        chapterContentHandleRule,
                        new Class[]{String.class},
                        new Object[]{content},
                        Map.of("content", content)
                );
            }

            // 将换行符和制表符替换成html对应代码
            result = result.replaceAll("\\n", "<br/>")
                    .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        // 正常内容
        else {
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
     * @param bodyElementStr 文章内容html页面{@code <body></body>}部分的元素
     */
    public void loadThisChapterNextContent(String chapterUrl, String bodyElementStr) {
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

        boolean isCodeConfig = ScriptCodeUtil.isJavaCodeConfig(nextContentUrl);
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
                    while (isRunning) {
                        // 检查用户是否取消了操作
                        progressIndicator.checkCanceled();
                        // 更新进度信息
                        pageCount += 1;
                        progressIndicator.setText2("正在加载第 " + pageCount + " 页...");

                        // 执行动态代码
                        returnResult = (String) ScriptCodeUtil.getScriptCodeExeResult(
                                nextContentUrl,
                                new Class[]{String.class, int.class, String.class, String.class},
                                new Object[]{chapterUrl, pageCount, preContentUrlTemp, prePageContent.get()},
                                Map.of(
                                        "chapterUrl", chapterUrl,
                                        "loadingPage", pageCount,
                                        "preContentUrl", preContentUrlTemp,
                                        "prePageContent", prePageContent.get()
                                )
                        );

                        // 若返回结果为空，则结束任务进程
                        if (StringUtils.isBlank(returnResult)) {
                            isRunning = false;
                            break;
                        }

                        returnResult = UrlUtil.buildFullURL(baseUrl, returnResult);
                        preContentUrlTemp = returnResult;
                        // 在 UI 线程中插入内容
                        nextContent.append(requestContent(returnResult, prePageContent::set));

                        // 添加延迟，避免过于频繁的请求
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
                        call.accept(result);
                    }
                } catch (Exception e) {
                    Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                    call.accept("");
                    throw new RuntimeException(e);
                }
            } else {
                Document document = null;
                try {
                    Connection connect = Jsoup.connect(url);
                    connect.header("User-Agent", ConstUtil.HEADER_USER_AGENT);
                    document = connect.get();
                } catch (IOException e) {
                    Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                    call.accept("");
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
