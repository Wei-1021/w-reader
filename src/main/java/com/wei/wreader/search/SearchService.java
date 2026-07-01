package com.wei.wreader.search;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jayway.jsonpath.JsonPath;
import com.wei.wreader.content.ContentFormatter;
import com.wei.wreader.content.ContentParser;
import com.wei.wreader.model.*;
import com.wei.wreader.service.AppConfigService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.comm.ScriptCodeUtil;
import com.wei.wreader.util.comm.StringTemplateEngine;
import com.wei.wreader.util.comm.UrlUtil;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.JsonUtil;
import com.wei.wreader.util.http.HttpRequestConfigParser;
import com.wei.wreader.util.http.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 搜索服务 - 负责书籍搜索和目录加载
 */
public class SearchService {
    private static final Logger LOG = Logger.getInstance(SearchService.class);

    private final Project project;
    private final CacheService cacheService;
    private final AppConfigService appConfig;
    private final SiteRuleService siteRuleService;
    private final CustomSiteUtil customSiteUtil;

    private Task.Backgroundable searchTask;
    private Task.Backgroundable loadListMainTask;
    private Task.Backgroundable nextListMainTask;
    private Task.Backgroundable chapterContentTask;

    public SearchService(Project project) {
        this.project = project;
        this.cacheService = CacheService.getInstance();
        this.appConfig = AppConfigService.getInstance();
        this.siteRuleService = SiteRuleService.getInstance();
        this.customSiteUtil = CustomSiteUtil.getInstance(project);
    }

    /**
     * 搜索书籍列表
     */
    public void searchBookList(String keyword, Consumer<List<BookInfo>> callback) {
        SiteBean siteBean = cacheService.getSelectedSiteBean();
        if (siteBean == null || siteBean.getSearchRules() == null) {
            Messages.showErrorDialog(ConstUtil.WREADER_SITE_BEAN_ERROR, "提示");
            return;
        }

        SearchRules searchRules = siteBean.getSearchRules();
        String searchUrl = searchRules.getUrl();
        if (StringUtils.isBlank(searchUrl)) {
            Messages.showErrorDialog("搜索URL为空", "提示");
            return;
        }

        // Build search URL
        String fullUrl = buildSearchUrl(searchUrl, keyword, siteBean.getBaseUrl());

        if (searchTask != null) {
            searchTask.onCancel();
        }
        searchTask = new Task.Backgroundable(project, "【W-Reader】正在搜索...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    String searchResult;
                    if (StringUtils.isNotBlank(searchRules.getDataBookListRule())) {
                        searchResult = searchBookListApi(fullUrl, searchRules);
                    } else {
                        searchResult = searchBookListHtml(fullUrl, searchRules, siteBean.getBookInfoRules());
                    }
                    List<BookInfo> results = parseSearchResults(searchResult, siteBean);
                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(results));
                } catch (Exception e) {
                    LOG.error("Search failed", e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                    });
                }
            }

            @Override
            public void onCancel() {
                super.onCancel();
            }
        };
        searchTask.queue();
    }

    /**
     * 加载书籍目录
     *
     * @param bookInfo              书籍信息
     * @param chapterNamesCallback  初始章节名列表回调
     * @param chapterUrlsCallback   初始章节URL列表回调
     * @param appendChaptersCallback 追加章节回调（下一页目录），参数为 Map("chapterNames", ..., "chapterUrls", ...)
     */
    public void loadBookDirectory(BookInfo bookInfo,
                                  Consumer<List<String>> chapterNamesCallback,
                                  Consumer<List<String>> chapterUrlsCallback,
                                  Consumer<Map<String, List<String>>> appendChaptersCallback) {
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        if (siteBean == null || siteBean.getListMainRules() == null) {
            return;
        }

        ListMainRules listMainRules = siteBean.getListMainRules();
        SearchRules searchRules = siteBean.getSearchRules();

        // 构建目录URL（与原项目getListMainUrl逻辑一致）
        String fullUrl = buildListMainUrl(listMainRules.getUrl(), searchRules, bookInfo, siteBean);

        if (loadListMainTask != null) {
            loadListMainTask.onCancel();
        }
        loadListMainTask = new Task.Backgroundable(project, "【W-Reader】正在加载目录...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    List<String> chapterNames = new ArrayList<>();
                    List<String> chapterUrls = new ArrayList<>();
                    String[] bodyHolder = {""};
                    Element[] bodyElementHolder = {null};

                    // 判断使用API还是HTML方式获取目录
                    String listMainUrl = listMainRules.getUrl();
                    String listMainUrlDataRule = listMainRules.getUrlDataRule();
                    boolean useApi = StringUtils.isNotBlank(listMainUrl) && StringUtils.isNotBlank(listMainUrlDataRule);

                    if (useApi) {
                        loadDirectoryViaApi(fullUrl, siteBean, listMainRules, bookInfo,
                                chapterNames, chapterUrls, bodyHolder);
                    } else {
                        loadDirectoryViaHtml(fullUrl, listMainRules, siteBean.getBaseUrl(),
                                chapterNames, chapterUrls, bodyHolder, bodyElementHolder);
                    }

                    ApplicationManager.getApplication().invokeLater(() -> {
                        chapterNamesCallback.accept(chapterNames);
                        chapterUrlsCallback.accept(chapterUrls);

                        // 触发下一页目录加载
                        if (appendChaptersCallback != null) {
                            loadNextListMain(
                                    bodyHolder[0],
                                    bodyElementHolder[0],
                                    fullUrl,
                                    appendChaptersCallback
                            );
                        }
                    });
                } catch (Exception e) {
                    LOG.error("Directory load failed", e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                    });
                }
            }

            @Override
            public void onCancel() {
                super.onCancel();
            }
        };
        loadListMainTask.queue();
    }

    /**
     * 构建目录URL（对应原项目getListMainUrl + buildListMainUrlFromTemplate）
     */
    private String buildListMainUrl(String url, SearchRules searchRules, BookInfo bookInfo, SiteBean siteBean) {
        // 如果是脚本配置，执行脚本返回URL
        if (ScriptCodeUtil.isJavaCodeConfig(url)) {
            try {
                Class<?>[] paramTypes = url.contains("com.wei.wreader.model.BookInfo") ?
                        new Class[]{BookInfo.class} : new Class[]{String.class};
                Object[] params = url.contains("com.wei.wreader.model.BookInfo") ?
                        new Object[]{bookInfo} : new Object[]{bookInfo.getBookId()};
                return (String) ScriptCodeUtil.getScriptCodeExeResult(
                        url, paramTypes, params, Map.of("bookInfo", bookInfo));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 如果URL模板不为空且不是HTML模式，使用模板引擎
        if (StringUtils.isNotBlank(url) && !siteBean.isHasHtml()) {
            return StringTemplateEngine.render(url, Map.of("bookId", bookInfo.getBookId()));
        }

        // 否则使用书籍URL
        String bookUrl = bookInfo.getBookUrl();
        if (bookUrl != null && !bookUrl.startsWith("http") && !JsonUtil.isValid(bookUrl)) {
            return searchRules.getUrl() + bookUrl;
        }
        return bookUrl;
    }

    private void loadDirectoryViaApi(String url, SiteBean siteBean, ListMainRules rules, BookInfo bookInfo,
                                     List<String> chapterNames, List<String> chapterUrls, String[] bodyHolder) throws Exception {
        HttpRequestBase requestBase = HttpUtil.commonRequest(url);
        requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(requestBase)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);

                if (StringUtils.isNotBlank(result)) {
                    bodyHolder[0] = result;
                }

                String dataRule = rules.getUrlDataRule();
                ArrayList<Map<String, Object>> resultMapList = JsonPath.read(result, dataRule);

                String itemListStr = resultMapList.toString();
                Map<String, Object> paramMap = Map.of(
                        "dataJsonStr", result,
                        "menuListJsonStr", itemListStr
                );

                ChapterRules chapterRules = siteBean.getChapterRules();
                String chapterUrlTemplate = chapterRules != null ? chapterRules.getUrl() : "";
                boolean useJavaCode = ScriptCodeUtil.isJavaCodeConfig(chapterUrlTemplate);

                List<String> itemIdList = new ArrayList<>();
                List<Integer> itemIndexList = new ArrayList<>();

                // 处理目录项（与原项目processDirectoryItems逻辑一致）
                processDirectoryItems(resultMapList, rules, chapterRules, bookInfo,
                        itemIdList, itemIndexList, chapterNames, chapterUrls,
                        paramMap, useJavaCode);

                // 如果使用Java代码配置，则批量执行脚本生成URL（与原项目executeChapterUrlScript逻辑一致）
                if (useJavaCode && StringUtils.isNotBlank(chapterUrlTemplate)) {
                    List<String> scriptUrls = executeChapterUrlScript(chapterUrlTemplate, paramMap,
                            itemIndexList, bookInfo.getBookId(), itemIdList);
                    if (scriptUrls != null && scriptUrls.size() == chapterNames.size()) {
                        chapterUrls.clear();
                        chapterUrls.addAll(scriptUrls);
                    }
                }
            }
        }
    }

    /**
     * 处理目录项（对应原项目processDirectoryItems）
     */
    private void processDirectoryItems(ArrayList<Map<String, Object>> jsonArray,
                                       ListMainRules listMainRules, ChapterRules chapterRules,
                                       BookInfo bookInfo, List<String> itemIdList, List<Integer> itemIndexList,
                                       List<String> chapterList, List<String> chapterUrlList,
                                       Map<String, Object> paramMap, boolean useJavaCode) throws MalformedURLException {
        String itemIdField = listMainRules.getItemIdField();
        String itemTitleField = listMainRules.getItemTitleField();

        for (int i = 0; i < jsonArray.size(); i++) {
            Map<String, Object> itemJson = jsonArray.get(i);
            String itemId = getStringFromMap(itemJson, itemIdField);
            String title = getStringFromMap(itemJson, itemTitleField);

            if (useJavaCode) {
                // Java代码配置模式：收集ID和索引，后续批量执行脚本
                itemIdList.add(itemId);
                itemIndexList.add(i);
                chapterList.add(title);
            } else {
                // 模板配置模式：直接构建URL
                String itemUrl = buildItemUrl(bookInfo.getBookId(), itemId, paramMap, itemJson,
                        listMainRules, chapterRules);
                chapterList.add(title);
                chapterUrlList.add(itemUrl);
            }
        }
    }

    /**
     * 构建项目URL（对应原项目buildItemUrl）
     */
    private String buildItemUrl(String bookId, String itemId, Map<String, Object> paramMap,
                                Map<String, Object> itemJson, ListMainRules listMainRules,
                                ChapterRules chapterRules) throws MalformedURLException {
        String itemUrlField = listMainRules.getItemUrlField();
        String itemUrl = "";
        if (StringUtils.isNotBlank(itemUrlField)) {
            itemUrl = getStringFromMap(itemJson, itemUrlField);

            if (StringUtils.isNotBlank(itemUrl)) {
                SiteBean tempSiteBean = cacheService.getTempSelectedSiteBean();
                if (tempSiteBean != null) {
                    itemUrl = UrlUtil.buildFullURL(tempSiteBean.getBaseUrl(), itemUrl.trim());
                }
            } else {
                // 如果没有URL字段，尝试使用urlDataHandleRule脚本
                String urlDataHandleRule = listMainRules.getUrlDataHandleRule();
                if (StringUtils.isNotBlank(urlDataHandleRule)) {
                    try {
                        itemUrl = (String) ScriptCodeUtil.getScriptCodeExeResult(
                                urlDataHandleRule,
                                new Class[]{Map.class, Map.class, String.class},
                                new Object[]{paramMap, itemJson, itemId},
                                paramMap
                        );
                    } catch (Exception e) {
                        LOG.warn("URL data handle script execution failed", e);
                    }
                }
            }
        } else {
            String chapterRulesUrl = chapterRules.getUrl();
            if (StringUtils.isNotBlank(chapterRulesUrl)) {
                itemUrl = StringTemplateEngine.render(chapterRulesUrl, new HashMap<>() {{
                    put("bookId", bookId);
                    put("itemId", itemId);
                }});
            }
        }

        return itemUrl != null ? itemUrl : "";
    }

    /**
     * 批量执行章节URL生成脚本（对应原项目executeChapterUrlScript）
     */
    private List<String> executeChapterUrlScript(String script, Map<String, Object> paramMap,
                                                  List<Integer> itemIndexList, String bookId,
                                                  List<String> itemIdList) {
        try {
            return ScriptCodeUtil.getScriptCodeExeListResult(
                    script,
                    new Class[]{Map.class, List.class, String.class, List.class},
                    new Object[]{paramMap, itemIndexList, bookId, itemIdList},
                    Map.of(
                            "result", paramMap.get("dataJsonStr"),
                            "bookInfo", cacheService.getSelectedBookInfo(),
                            "itemIndexList", itemIndexList,
                            "itemIdList", itemIdList
                    )
            );
        } catch (Exception e) {
            LOG.error("Chapter URL script execution failed", e);
            Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String getStringFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return "";
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private void loadDirectoryViaHtml(String url, ListMainRules rules, String baseUrl,
                                       List<String> chapterNames, List<String> chapterUrls,
                                       String[] bodyHolder, Element[] bodyElementHolder) throws Exception {
        Document document = Jsoup.connect(url)
                .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                .get();

        Element body = document.body();
        if (bodyHolder != null) {
            bodyHolder[0] = body.toString();
        }
        if (bodyElementHolder != null) {
            bodyElementHolder[0] = body;
        }

        String listMainElementName = rules.getListMainElementName();
        Elements listMainElements = body.select(listMainElementName);
        String location = document.location();

        String urlElement = StringUtils.defaultIfBlank(rules.getUrlElement(), "a");
        String titleElement = rules.getTitleElement();

        for (Element element : listMainElements) {
            // 提取URL
            String chapterUrl = "";
            Elements urlElements = element.select(urlElement);
            if (!urlElements.isEmpty()) {
                chapterUrl = urlElements.first().attr("href");
            }

            // 提取标题
            String chapterTitle = "";
            if (StringUtils.isNotBlank(titleElement)) {
                Elements titleElements = element.select(titleElement);
                if (!titleElements.isEmpty()) {
                    chapterTitle = titleElements.first().text();
                }
            } else {
                chapterTitle = element.text();
            }

            if (StringUtils.isNotBlank(chapterTitle)) {
                chapterNames.add(chapterTitle);
                // 构建完整URL
                chapterUrl = UrlUtil.buildFullURL(location, chapterUrl);
                chapterUrls.add(chapterUrl);
            }
        }
    }

    private String buildSearchUrl(String searchUrlTemplate, String keyword, String baseUrl) {
        // 判断是否为脚本代码配置，如果是则执行脚本返回URL，否则使用模板引擎渲染URL
        if (ScriptCodeUtil.isJavaCodeConfig(searchUrlTemplate)) {
            return executeSearchUrlScript(searchUrlTemplate, keyword);
        }
        // 使用模板引擎渲染URL
        if (searchUrlTemplate.contains("{keyword}") || searchUrlTemplate.contains("{key}")) {
            if (searchUrlTemplate.startsWith("http")) {
                return searchUrlTemplate.replace("{keyword}", keyword);
            }
            return baseUrl + searchUrlTemplate.replace("{keyword}", keyword);
        }
        return StringTemplateEngine.render(searchUrlTemplate, Map.of("key", keyword, "page", 1));
    }

    /**
     * 执行搜索URL生成脚本
     *
     * @param script    脚本配置
     * @param searchKey 搜索关键词
     * @return 生成的URL
     */
    private String executeSearchUrlScript(String script, String searchKey) {
        try {
            return (String) ScriptCodeUtil.getScriptCodeExeResult(
                    script,
                    new Class<?>[]{String.class, String.class},
                    new Object[]{searchKey, "1"},
                    Map.of("key", searchKey, "page", 1)
            );
        } catch (Exception e) {
            LOG.error("Search URL script execution failed", e);
            Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
            return "";
        }
    }

    /**
     * 同步搜索书籍列表（在后台线程中调用）
     */
    public String searchBookListSync(String url, SiteBean siteBean) throws Exception {
        SearchRules searchRules = siteBean.getSearchRules();
        if (siteBean.isHasHtml()) {
            return searchBookListHtml(url, searchRules, siteBean.getBookInfoRules());
        } else {
            return searchBookListApi(url, searchRules);
        }
    }

    /**
     * 通过API搜索书籍列表
     */
    private String searchBookListApi(String url, SearchRules searchRules) throws Exception {
        HttpRequestBase requestBase = HttpUtil.commonRequest(url);
        requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(requestBase)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                if (StringUtils.isNotBlank(searchRules.getDataBookListRule())) {
                    Object readJson = JsonPath.read(result, searchRules.getDataBookListRule());
                    return readJson.toString();
                }
                return result;
            }
        }
        return "";
    }

    /**
     * 通过HTML搜索书籍列表
     */
    private String searchBookListHtml(String url, SearchRules searchRules, BookInfoRules bookInfoRules) throws Exception {
        // 解析HTTP请求配置
        HttpRequestConfigParser parser = new HttpRequestConfigParser(url);
        String requestUrl = parser.getUrl();
        String requestMethod = parser.getMethod();
        Map<String, String> queryParams = parser.getQueryParams();
        Map<String, String> bodyParams = parser.getBodyParams();
        Map<String, String> headers = parser.getHeader();

        Document document = HttpUtil.requestHtml(requestUrl, requestMethod, queryParams, bodyParams, headers);
        if (document == null) {
            return "";
        }

        Elements bookElements = document.select(searchRules.getBookListElementName());
        JsonArray jsonArray = new JsonArray();

        for (Element element : bookElements) {
           JsonObject bookJson = new JsonObject();

            // 解析URL元素规则
            String bookListUrlElement = StringUtils.defaultIfBlank(searchRules.getBookListUrlElement(), "a");
            String[] urlElementRules = bookListUrlElement.split("@");
            String cssSelector = urlElementRules[0];
            String urlRuleFront = extractRulePart(urlElementRules, ConstUtil.CSS_QUERY_FRONT_FLAG);
            String urlRuleBack = extractRulePart(urlElementRules, ConstUtil.CSS_QUERY_BACK_FLAG);

            Elements urlElements = element.select(cssSelector);
            if (!urlElements.isEmpty()) {
                String bookUrl = urlElements.first().attr("href");

                try {
                    bookUrl = UrlUtil.buildFullURL(document.location(), urlRuleFront + bookUrl + urlRuleBack);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                bookJson.addProperty(bookInfoRules.getBookUrlField(), bookUrl);
            }

            Elements titleElements = element.select(searchRules.getBookListTitleElement());
            if (!titleElements.isEmpty()) {
                bookJson.addProperty(bookInfoRules.getBookNameField(), titleElements.first().text());
            }

            if (bookJson.has(bookInfoRules.getBookNameField())) {
                jsonArray.add(bookJson);
            }
        }

        return jsonArray.toString();
    }

    /**
     * 远程获取章节内容
     *
     * @param url      章节内容页面URL
     * @param callback 获取成功后的回调处理
     */
    public void searchBookContentRemote(String url, ChapterInfo chapterInfo, Consumer<SearchBookCallParam> callback) {
        if (chapterContentTask != null) {
            chapterContentTask.onCancel();
        }
        chapterContentTask = new Task.Backgroundable(project, "【W-Reader】正在获取内容...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("【W-Reader】正在获取内容...");
                indicator.setIndeterminate(true);

                SiteBean siteBean = cacheService.getTempSelectedSiteBean();
                if (siteBean == null || StringUtils.isBlank(siteBean.getId())) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(ConstUtil.WREADER_SITE_BEAN_ERROR, "提示");
                    });
                    return;
                }

                ChapterRules chapterRules = siteBean.getChapterRules();
                try {
                    String chapterContent;
                    String contentText = "";
                    Element bodyElement = null;
                    String bodyElementStr = "";
                    String contentOriginalStyle = "";

                    if (ContentParser.shouldUseApiMethod(chapterRules)) {
                        chapterContent = ContentParser.loadContentViaApi(url, chapterRules);
                    } else {
                        ContentParser.HtmlParseResult result = ContentParser.loadContentViaHtml(url, chapterRules);
                        chapterContent = result.getContentHtml();
                        contentText = result.getContentText();
                        bodyElement = result.getBodyElement();
                        contentOriginalStyle = result.getContentOriginalStyle();
                    }

                    chapterContent = ContentParser.handleContent(chapterContent, siteBean);

                    // Add chapter title as header
                    String fontColorHex = cacheService.getFontColorHex();
                    if (fontColorHex == null) fontColorHex = "#cccccc";

                    chapterContent = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" +
                            fontColorHex + ";\">" + chapterInfo.getChapterTitle() +
                            "</h3>" + chapterContent;

                    String chapterContentText = ContentFormatter.processChapterContentText(chapterContent);

                    SearchBookCallParam param = new SearchBookCallParam();
                    param.setBodyElement(bodyElement);
                    param.setBodyContentStr(bodyElementStr);
                    param.setChapterContentHtml(chapterContent);
                    param.setChapterContentText(chapterContentText);

                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(param));

                } catch (Exception e) {
                    LOG.error("Content load failed for URL: " + url, e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                    });
                }
            }

            @Override
            public void onCancel() {
                super.onCancel();
            }
        };
        chapterContentTask.queue();
    }

    /**
     * 解析搜索结果为BookInfo列表
     */
    public List<BookInfo> parseSearchResults(String searchResult, SiteBean siteBean) {
        List<BookInfo> results = new ArrayList<>();
        if (StringUtils.isBlank(searchResult) || "[]".equals(searchResult)) {
            return results;
        }

        BookInfoRules bookInfoRules = siteBean.getBookInfoRules();
        try {
            JsonArray jsonArray = new Gson().fromJson(searchResult, JsonArray.class);
            if (jsonArray == null) return results;

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                BookInfo bookInfo = new BookInfo();
                bookInfo.setBookId(JsonUtil.getString(jsonObject, bookInfoRules.getBookIdField()));
                bookInfo.setBookName(JsonUtil.getString(jsonObject, bookInfoRules.getBookNameField()));
                bookInfo.setBookAuthor(JsonUtil.getString(jsonObject, bookInfoRules.getBookAuthorField()));
                bookInfo.setBookDesc(JsonUtil.getString(jsonObject, bookInfoRules.getBookDescField()));
                bookInfo.setBookImgUrl(JsonUtil.getString(jsonObject, bookInfoRules.getBookImgUrlField()));
                bookInfo.setBookUrl(JsonUtil.getString(jsonObject, bookInfoRules.getBookUrlField()));
                results.add(bookInfo);
            }
        } catch (Exception e) {
            LOG.error("Failed to parse search results", e);
        }
        return results;
    }


    /**
     * 提取规则部分
     *
     * @param rules 规则数组
     * @param flag  标志字符串
     * @return 提取的规则部分
     */
    private String extractRulePart(String[] rules, String flag) {
        for (String rule : rules) {
            if (rule.startsWith(flag)) {
                return rule.replace(flag, "");
            }
        }
        return "";
    }

    // ==================== 下一页目录加载 ====================

    /**
     * 加载下一页目录
     * 当书源的目录分多页时，通过脚本循环获取后续页面并追加章节
     *
     * @param bodyContentStr      当前页面HTML字符串
     * @param bodyElement         当前页面DOM元素
     * @param listMainUrl         当前目录页URL
     * @param nextChapterCallback 追加章节的回调（章节名列表, 章节URL列表）
     */
    public void loadNextListMain(String bodyContentStr, Element bodyElement,
                                 String listMainUrl,
                                 Consumer<Map<String, List<String>>> nextChapterCallback) {
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        if (siteBean == null || siteBean.getListMainRules() == null) {
            return;
        }
        ListMainRules listMainRules = siteBean.getListMainRules();
        String nextUrl = listMainRules.getNextListMainUrl();

        if (StringUtils.isEmpty(nextUrl) || !ScriptCodeUtil.isJavaCodeConfig(nextUrl)) {
            return;
        }

        // 取消之前的任务
        if (nextListMainTask != null) {
            nextListMainTask.onCancel();
        }
        nextListMainTask = new Task.Backgroundable(project, "【W-Reader】加载下一页目录...") {
            private volatile boolean running = true;
            private final List<String> extraChapterNames = new ArrayList<>();
            private final List<String> extraChapterUrls = new ArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    String baseUrl = siteBean.getBaseUrl();
                    String previousUrl = listMainUrl;
                    AtomicReference<String> currentBodyStr = new AtomicReference<>(bodyContentStr);
                    AtomicReference<Element> currentBody = new AtomicReference<>(bodyElement);
                    int pageIndex = 1;

                    while (running) {
                        indicator.checkCanceled();
                        pageIndex++;
                        indicator.setText2("正在加载第" + pageIndex + "页...");

                        // 执行脚本获取下一页URL
                        String nextResultUrl = executeNextListMainScript(
                                nextUrl, baseUrl, pageIndex, previousUrl,
                                currentBodyStr.get(), currentBody.get()
                        );

                        if (StringUtils.isEmpty(nextResultUrl)) {
                            break;
                        }
                        previousUrl = nextResultUrl;

                        // 请求下一页目录
                        Map<String, List<String>> pageResult = requestNextListMain(
                                nextResultUrl, listMainRules, siteBean,
                                bodyStr -> currentBodyStr.set(bodyStr),
                                body -> currentBody.set(body));

                        extraChapterNames.addAll(pageResult.get("chapterNames"));
                        extraChapterUrls.addAll(pageResult.get("chapterUrls"));

                        if (pageResult.get("chapterNames").isEmpty()) {
                            break;
                        }

                        Thread.sleep(1000);
                    }
                } catch (ProcessCanceledException ignored) {
                } catch (Exception e) {
                    LOG.error("Next page directory load failed", e);
                }
            }

            @Override
            public void onSuccess() {
                if (!extraChapterNames.isEmpty() && nextChapterCallback != null) {
                    nextChapterCallback.accept(Map.of(
                            "chapterNames", extraChapterNames,
                            "chapterUrls", extraChapterUrls
                    ));
                }
            }

            @Override
            public void onCancel() {
                running = false;
                super.onCancel();
            }
        };
        nextListMainTask.queue();
    }

    /**
     * 执行下一页目录脚本
     * 脚本参数：baseUrl, pageIndex, previousUrl, bodyStr, bodyElement
     */
    private String executeNextListMainScript(String script, String baseUrl, int pageIndex,
                                             String previousUrl, String bodyStr, Element bodyElement) {
        try {
            return (String) ScriptCodeUtil.getScriptCodeExeResult(
                    script,
                    new Class[]{String.class, Integer.class, String.class, String.class, Element.class},
                    new Object[]{baseUrl, pageIndex, previousUrl, bodyStr, bodyElement},
                    Map.of(
                            "baseUrl", baseUrl,
                            "pageIndex", pageIndex,
                            "preUrl", previousUrl,
                            "bodyElementStr", bodyStr,
                            "bodyElement", bodyElement
                    )
            );
        } catch (Exception e) {
            LOG.error("Next list main script execution failed", e);
            return "";
        }
    }

    /**
     * 请求下一页目录内容
     */
    private Map<String, List<String>> requestNextListMain(String url, ListMainRules rules,
                                                           SiteBean siteBean,
                                                           Consumer<String> bodyStrCallback,
                                                           Consumer<Element> bodyCallback) {
        List<String> chapterNames = new ArrayList<>();
        List<String> chapterUrls = new ArrayList<>();

        try {
            if (rules.isUseNextListMainApi()) {
                requestNextListMainApi(url, rules, chapterNames, chapterUrls, bodyStrCallback);
            } else {
                requestNextListMainHtml(url, rules, siteBean.getBaseUrl(),
                        chapterNames, chapterUrls, bodyStrCallback, bodyCallback);
            }
        } catch (Exception e) {
            LOG.error("Request next list main failed", e);
        }

        return Map.of("chapterNames", chapterNames, "chapterUrls", chapterUrls);
    }

    /**
     * 通过API方式请求下一页目录
     */
    private void requestNextListMainApi(String url, ListMainRules rules,
                                         List<String> chapterNames, List<String> chapterUrls,
                                         Consumer<String> bodyStrCallback) throws Exception {
        HttpRequestBase requestBase = HttpUtil.commonRequest(url);
        requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(requestBase)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                bodyStrCallback.accept(result);

                String dataRule = rules.getNextListMainApiDataRule();
                List<Map<String, Object>> items = JsonPath.read(result, dataRule);

                String itemIdField = rules.getItemIdField();
                String itemTitleField = rules.getItemTitleField();

                for (Map<String, Object> item : items) {
                    String itemId = item.get(itemIdField) != null ? item.get(itemIdField).toString() : "";
                    String title = item.get(itemTitleField) != null ? item.get(itemTitleField).toString() : "";

                    // 尝试从 itemUrlField 获取 URL，否则用模板构建
                    String itemUrl = "";
                    String itemUrlField = rules.getItemUrlField();
                    if (StringUtils.isNotBlank(itemUrlField) && item.get(itemUrlField) != null) {
                        itemUrl = item.get(itemUrlField).toString();
                    } else if (StringUtils.isNotBlank(rules.getUrlDataHandleRule())) {
                        try {
                            itemUrl = (String) ScriptCodeUtil.getScriptCodeExeResult(
                                    rules.getUrlDataHandleRule(),
                                    new Class[]{Map.class, Map.class, String.class},
                                    new Object[]{result, item, itemId},
                                    Map.of("result", result, "itemMap", item, "itemId", itemId)
                            );
                        } catch (Exception e) {
                            LOG.warn("URL data handle script failed for next page item", e);
                        }
                    }

                    if (StringUtils.isNotBlank(title)) {
                        chapterNames.add(title);
                        chapterUrls.add(itemUrl);
                    }
                }
            }
        }
    }

    /**
     * 通过HTML方式请求下一页目录
     */
    private void requestNextListMainHtml(String url, ListMainRules rules, String baseUrl,
                                          List<String> chapterNames, List<String> chapterUrls,
                                          Consumer<String> bodyStrCallback,
                                          Consumer<Element> bodyCallback) throws Exception {
        Document document = Jsoup.connect(url)
                .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                .get();

        Element body = document.body();
        bodyStrCallback.accept(body.toString());
        bodyCallback.accept(body);

        Elements elements = body.select(rules.getListMainElementName());
        String urlElement = StringUtils.defaultIfBlank(rules.getUrlElement(), "a");
        String titleElement = rules.getTitleElement();
        String location = document.location();

        for (Element element : elements) {
            String chapterUrl = "";
            Elements urlElements = element.select(urlElement);
            if (!urlElements.isEmpty()) {
                chapterUrl = urlElements.first().attr("href");
            }

            String chapterTitle = "";
            if (StringUtils.isNotBlank(titleElement)) {
                Elements titleElements = element.select(titleElement);
                if (!titleElements.isEmpty()) {
                    chapterTitle = titleElements.first().text();
                }
            } else {
                chapterTitle = element.text();
            }

            if (StringUtils.isNotBlank(chapterTitle)) {
                chapterNames.add(chapterTitle);
                chapterUrl = UrlUtil.buildFullURL(location, chapterUrl);
                chapterUrls.add(chapterUrl);
            }
        }
    }
}
