package com.wei.wreader.utils;

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

/**
 * 小说搜索工具类 - 重构版本
 * <p>
 * 重构目标：
 * 1. 改善代码结构，按功能模块分组
 * 2. 提高代码可读性和可维护性
 * 3. 保持原有功能完整性
 * 4. 优化异常处理和用户体验
 * <p>
 * 主要功能：
 * - 小说搜索和结果显示
 * - 目录获取和章节管理
 * - 内容加载和显示处理
 * - 多种数据源支持（HTML/API/脚本）
 *
 * @author weizhanjie
 * @version 1.0
 */
public class SearchBookRefactored {

    // ==================== 常量定义区域 ====================

    //region 全局参数 -- 常量定义区域
    // 任务标题常量，用于进度指示器显示
    private static final String SEARCH_TASK_TITLE = "【W-Reader】正在搜索...";
    private static final String DIRECTORY_TASK_TITLE = "【W-Reader】正在获取小说目录...";
    private static final String CONTENT_TASK_TITLE = "【W-Reader】正在获取内容...";
    private static final String NEXT_PAGE_DIRECTORY_TITLE = "【W-Reader】加载下一页目录...";
    private static final String NEXT_PAGE_CONTENT_TITLE = "【W-Reader】加载本章节下一页内容";
    //endregion

    // ==================== 依赖服务注入 ====================

    //region 全局参数 -- 依赖服务注入
    // 核心服务依赖，通过构造函数注入
    private final ConfigYaml configYaml;                           // 配置文件服务
    private final CacheService cacheService;                       // 缓存服务
    private final CustomSiteRuleCacheServer customSiteRuleCacheServer; // 自定义站点规则缓存
    private final CustomSiteUtil customSiteUtil;                   // 自定义站点工具
    private final Settings settings;                               // 应用设置
    private final Project project;                                 // 当前项目实例
    //endregion

    // ==================== 状态管理变量 ====================

    //region 全局参数 -- 状态管理变量
    // 任务状态管理
    private Task.Backgroundable nextListMainTask;                  // 下一页目录加载任务
    private Task.Backgroundable nextContentTask;                   // 下一页内容加载任务

    // 数据状态变量
    private List<SiteBean> siteBeanList;                          // 当前站点列表
    private DialogBuilder searchBookDialogBuilder;                // 搜索对话框构建器
    private JBList<LabelValueItem> chapterListJBList;             // 章节列表组件
    private DefaultListModel<LabelValueItem> chapterListModel;    // 章节列表数据模型
    private int selectedBookSiteIndex;                            // 选中的书源索引
    private List<String> bookNameList = new ArrayList<>();        // 书名列表
    private List<BookInfo> bookInfoList = new ArrayList<>();      // 书籍信息列表
    private String contentOriginalStyle;                          // 原始内容样式
    //endregion

    // ==================== 构造函数 ====================

    /**
     * 构造函数 - 依赖注入模式
     *
     * @param project 当前IntelliJ项目实例
     */
    public SearchBookRefactored(Project project) {
        // 注入外部依赖
        this.project = project;
        this.configYaml = new ConfigYaml();
        this.cacheService = CacheService.getInstance();
        this.customSiteRuleCacheServer = CustomSiteRuleCacheServer.getInstance();
        this.customSiteUtil = CustomSiteUtil.getInstance(project);

        // 初始化应用设置
        this.settings = initializeSettings();
        this.selectedBookSiteIndex = initializeSelectedSiteIndex();
        this.siteBeanList = initializeSiteBeanList();
    }

    // ==================== 公共API方法 ====================

    //region 公共API方法

    /**
     * 构建搜索弹出窗口
     * 主入口方法，启动整个搜索流程
     */
    public void buildSearchBookDialog() {
        SwingUtilities.invokeLater(this::showSearchDialog);
    }

    /**
     * 执行小说搜索
     *
     * @param searchBookTextField 搜索关键词输入框
     */
    public void searchBook(JTextField searchBookTextField) {
        // 验证输入
        String searchKey = searchBookTextField.getText();
        if (StringUtils.isBlank(searchKey)) {
            Messages.showMessageDialog(ConstUtil.WREADER_SEARCH_EMPTY, "提示", Messages.getInformationIcon());
            return;
        }

        // 获取搜索配置
        SiteBean selectedSiteBean = cacheService.getTempSelectedSiteBean();
        String searchUrl = selectedSiteBean.getSearchRules().getUrl();
        if (StringUtils.isBlank(searchUrl)) {
            Messages.showMessageDialog(ConstUtil.WREADER_ERROR, "提示", Messages.getInformationIcon());
            return;
        }

        // 构建搜索URL并执行搜索任务
        String searchBookUrl = buildSearchUrl(searchUrl, searchKey);
        searchBookBackTask(searchBookUrl);
    }

    /**
     * 搜索小说列表 - 核心搜索方法
     *
     * @param url 搜索请求URL
     * @return 搜索结果JSON字符串
     */
    public String searchBookList(String url) {
        SiteBean tempSiteBean = cacheService.getTempSelectedSiteBean();
        SearchRules searchRules = tempSiteBean.getSearchRules();
        BookInfoRules bookInfoRules = tempSiteBean.getBookInfoRules();

        // 根据站点配置决定使用HTML解析还是API调用
        return tempSiteBean.isHasHtml() ?
                searchBookListHtml(url, searchRules, bookInfoRules) :
                searchBookListApi(url, searchRules);
    }

    /**
     * 获取小说目录信息
     *
     * @param url 目录页面URL
     * @return 包含目录信息的Map
     */
    public Map<String, Object> searchBookDirectory(String url) {
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules listMainRules = siteBean.getListMainRules();
        String listMainUrl = listMainRules.getUrl();
        String listMainUrlDataRule = listMainRules.getUrlDataRule();

        // 根据配置决定使用API还是HTML方式获取目录
        boolean useApi = StringUtils.isNotBlank(listMainUrl) && StringUtils.isNotBlank(listMainUrlDataRule);
        return useApi ?
                searchBookDirectoryHtml(url, siteBean, listMainRules) :
                searchBookDirectoryApi(url, listMainRules);
    }

    /**
     * 远程获取小说内容
     *
     * @param url      内容页面URL
     * @param callback 获取成功后的回调处理
     */
    public void searchBookContentRemote(String url, Consumer<SearchBookCallParam> callback) {
        new ContentLoadTask(url, callback).queue();
    }

    /**
     * 处理小说内容 - 应用各种规则和正则表达式
     *
     * @param content 原始内容
     * @return 处理后的内容
     * @throws Exception 处理过程中可能抛出的异常
     */
    public String handleContent(String content) throws Exception {
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        ChapterRules chapterRules = siteBean.getChapterRules();
        String handleRule = chapterRules.getContentHandleRule();

        // 根据配置规则处理内容
        String processedContent = processContentByRule(content, handleRule);
        return applyContentRegexRules(processedContent, chapterRules.getContentRegexList());
    }

    /**
     * 加载下一页目录内容
     *
     * @param bodyElementStr 页面HTML内容
     * @param bodyElement    页面DOM元素
     * @param callback       加载完成后的回调
     */
    public void loadNextListMain(String bodyElementStr, Element bodyElement, Runnable callback) {
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules listMainRules = siteBean.getListMainRules();

        // 验证必要条件
        if (listMainRules == null || StringUtils.isEmpty(listMainRules.getNextListMainUrl())) {
            if (callback != null) callback.run();
            return;
        }

        // 检查是否为脚本配置
        if (!ScriptCodeUtil.isJavaCodeConfig(listMainRules.getNextListMainUrl())) {
            if (callback != null) callback.run();
            return;
        }

        // 取消之前的任务并启动新任务
        cancelPreviousTask(nextListMainTask);
        nextListMainTask = new NextListMainTask(bodyElementStr, bodyElement, callback);
        nextListMainTask.queue();
    }

    /**
     * 加载本章节的下一页内容
     *
     * @param chapterUrl     当前章节URL
     * @param bodyElementStr 页面内容
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

        // 取消之前任务并启动新任务
        cancelPreviousTask(nextContentTask);
        nextContentTask = new NextContentTask(chapterUrl, bodyElementStr);
        nextContentTask.queue();
    }

    /**
     * 更新内容显示 - 根据设置类型更新不同界面
     */
    public void updateContentText() {
        try {
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    updateToolWindowContent();
                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    updateStatusBarContent();
                    break;
                case Settings.DISPLAY_TYPE_TERMINAL:
                    // TODO: 终端显示暂未实现
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //endregion

    // ==================== 初始化方法 ====================

    //region 初始化方法

    /**
     * 初始化应用设置
     *
     * @return 初始化后的Settings对象
     */
    private Settings initializeSettings() {
        Settings settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }
        // 确保字符集设置有效
        if (StringUtils.isBlank(settings.getCharset())) {
            settings.setCharset(configYaml.getSettings().getCharset());
        }
        return settings;
    }

    /**
     * 初始化选中的书源索引
     *
     * @return 选中的索引值
     */
    private int initializeSelectedSiteIndex() {
        Integer cachedIndex = cacheService.getSelectedBookSiteIndex();
        if (cachedIndex == null) {
            cacheService.setSelectedBookSiteIndex(0);
            return 0;
        }
        return cachedIndex;
    }

    /**
     * 初始化站点Bean列表
     *
     * @return 站点Bean列表
     */
    private List<SiteBean> initializeSiteBeanList() {
        String selectedRuleKey = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();

        // 根据选择的规则键决定使用默认站点还是自定义站点
        if (StringUtils.isBlank(selectedRuleKey) ||
                ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(selectedRuleKey)) {
            return configYaml.getSiteList();
        } else {
            Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
            return siteMap.get(selectedRuleKey) == null ? new ArrayList<>() : siteMap.get(selectedRuleKey);
        }
    }
    //endregion

    // ==================== 搜索相关方法 ====================

    //region 搜索相关方法

    /**
     * 显示搜索对话框 - UI构建入口
     */
    private void showSearchDialog() {
        // 构建UI组件
        ComboBox<String> siteListComboBox = buildSiteComboBox();
        ComboBox<String> siteGroupComboBox = buildSiteGroupComboBox(siteListComboBox);
        JTextField searchBookTextField = new JTextField(20);

        // 组装对话框组件
        Object[] dialogComponents = {
                "书源分组", siteGroupComboBox,
                ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT, siteListComboBox,
                searchBookTextField
        };

        // 显示对话框
        searchBookDialogBuilder = MessageDialogUtil.showMessageDialog(
                project,
                ConstUtil.WREADER_SEARCH_BOOK_TITLE,
                dialogComponents,
                () -> searchBookDialogOk(siteListComboBox, searchBookTextField),
                this::commonCancelOperation
        );
    }

    /**
     * 构建书源分组下拉框
     *
     * @param siteListComboBox 书源列表下拉框（用于联动更新）
     * @return 分组下拉框组件
     */
    private ComboBox<String> buildSiteGroupComboBox(ComboBox<String> siteListComboBox) {
        // 获取分组数据
        Map<String, List<SiteBean>> siteGroupMap = customSiteUtil.getSiteMap();
        List<String> siteGroupNameList = customSiteUtil.getCustomSiteKeyGroupList();
        String selectedSiteGroupName = getSelectedSiteGroupName(siteGroupNameList);

        // 更新当前站点列表
        siteBeanList = siteGroupMap.get(selectedSiteGroupName) == null ?
                new ArrayList<>() :
                siteGroupMap.get(selectedSiteGroupName);

        // 创建并填充分组下拉框
        ComboBox<String> comboBox = new ComboBox<>();
        int selectedIndex = populateSiteGroupComboBox(comboBox, siteGroupNameList, selectedSiteGroupName);
        comboBox.setSelectedIndex(selectedIndex);

        // 添加选择事件监听器
        comboBox.addItemListener(e -> handleSiteGroupChange(e, siteGroupMap, siteListComboBox));
        return comboBox;
    }

    /**
     * 获取选中的站点分组名称
     *
     * @param siteGroupNameList 分组名称列表
     * @return 选中的分组名称
     */
    private String getSelectedSiteGroupName(List<String> siteGroupNameList) {
        String selectedName = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();
        if (selectedName == null) {
            // 使用默认分组
            int defaultIndex = siteGroupNameList.indexOf(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY);
            return siteGroupNameList.get(defaultIndex);
        }
        return selectedName;
    }

    /**
     * 填充分组下拉框
     *
     * @param comboBox          下拉框组件
     * @param groupNameList     分组名称列表
     * @param selectedGroupName 选中的分组名称
     * @return 选中项的索引
     */
    private int populateSiteGroupComboBox(ComboBox<String> comboBox,
                                          List<String> groupNameList,
                                          String selectedGroupName) {
        int selectedIndex = 0;
        for (int i = 0; i < groupNameList.size(); i++) {
            String groupName = groupNameList.get(i);
            comboBox.addItem(groupName);
            if (groupName.equals(selectedGroupName)) {
                selectedIndex = i;
            }
        }
        return selectedIndex;
    }

    /**
     * 处理站点分组变更事件
     *
     * @param e                选择事件
     * @param siteGroupMap     站点分组映射
     * @param siteListComboBox 书源列表下拉框
     */
    private void handleSiteGroupChange(java.awt.event.ItemEvent e,
                                       Map<String, List<SiteBean>> siteGroupMap,
                                       ComboBox<String> siteListComboBox) {
        String selectedGroupName = (String) e.getItem();
        siteBeanList = siteGroupMap.get(selectedGroupName) == null ?
                new ArrayList<>() :
                siteGroupMap.get(selectedGroupName);

        // 刷新书源列表
        refreshSiteListComboBox(siteListComboBox);
        selectedBookSiteIndex = 0;
        siteListComboBox.setSelectedIndex(selectedBookSiteIndex);

        // 更新缓存
        customSiteRuleCacheServer.setTempSelectedCustomSiteRuleKey(selectedGroupName);
    }

    /**
     * 刷新书源列表下拉框
     *
     * @param comboBox 要刷新的下拉框
     */
    private void refreshSiteListComboBox(ComboBox<String> comboBox) {
        comboBox.removeAllItems();
        for (SiteBean site : siteBeanList) {
            comboBox.addItem(site.getName() + "(" + site.getId() + ")");
        }
    }

    /**
     * 构建书源选择下拉框
     *
     * @return 书源下拉框组件
     */
    private ComboBox<String> buildSiteComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        for (SiteBean site : siteBeanList) {
            comboBox.addItem(site.getName() + "(" + site.getId() + ")");
        }
        comboBox.setSelectedIndex(selectedBookSiteIndex);
        return comboBox;
    }

    /**
     * 搜索对话框确认按钮处理
     *
     * @param comboBox            书源选择下拉框
     * @param searchBookTextField 搜索输入框
     */
    private void searchBookDialogOk(ComboBox<String> comboBox, JTextField searchBookTextField) {
        // 获取选择的站点信息
        int selectedIndex = comboBox.getSelectedIndex();
        selectedBookSiteIndex = selectedIndex;
        SiteBean selectedSiteBean = siteBeanList.get(selectedIndex);

        // 缓存临时搜索站点信息
        cacheService.setTempSelectedBookSiteIndex(selectedIndex);
        cacheService.setTempSelectedSiteBean(selectedSiteBean);

        // 执行搜索
        searchBook(searchBookTextField);
    }

    /**
     * 构建搜索URL
     *
     * @param searchUrlTemplate URL模板
     * @param searchKey         搜索关键词
     * @return 完整的搜索URL
     */
    private String buildSearchUrl(String searchUrlTemplate, String searchKey) {
        // 判断是否为脚本代码配置，如果是则执行脚本返回URL，否则使用模板引擎渲染URL
        return ScriptCodeUtil.isJavaCodeConfig(searchUrlTemplate) ?
                executeSearchUrlScript(searchUrlTemplate, searchKey) :
                StringTemplateEngine.render(searchUrlTemplate, Map.of("key", searchKey, "page", 1));
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
            e.printStackTrace();
            Messages.showMessageDialog(ConstUtil.WREADER_ERROR, "提示", Messages.getInformationIcon());
            return "";
        }
    }

    /**
     * 启动搜索后台任务
     *
     * @param searchBookUrl 搜索URL
     */
    private void searchBookBackTask(String searchBookUrl) {
        new SearchBookTask(searchBookUrl).queue();
    }
    //endregion

    // ==================== 搜索结果处理方法 ====================

    //region 搜索结果处理方法

    /**
     * 处理搜索结果列表
     *
     * @param result 搜索结果JSON字符串
     */
    public void handleBookList(String result) {
        if (StringUtils.isEmpty(result)) return;

        // 获取相关配置
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        SearchRules searchRules = siteBean.getSearchRules();
        BookInfoRules bookInfoRules = siteBean.getBookInfoRules();

        // 解析JSON数据
        JsonArray jsonArray = parseJsonArray(result);
        if (jsonArray == null || jsonArray.isEmpty()) return;

        // 提取书籍信息
        extractBookInfo(jsonArray, bookInfoRules);

        // 显示书籍列表对话框
        showBookListDialog();
    }

    /**
     * 解析JSON数组
     *
     * @param jsonString JSON字符串
     * @return 解析后的JsonArray对象
     */
    private JsonArray parseJsonArray(String jsonString) {
        try {
            return new Gson().fromJson(jsonString, JsonArray.class);
        } catch (Exception e) {
            e.printStackTrace();
            Messages.showMessageDialog(ConstUtil.WREADER_ERROR, "提示", Messages.getInformationIcon());
            return null;
        }
    }

    /**
     * 从JSON数组中提取书籍信息
     *
     * @param jsonArray     JSON数组
     * @param bookInfoRules 书籍信息规则
     */
    private void extractBookInfo(JsonArray jsonArray, BookInfoRules bookInfoRules) {
        // 清空旧数据
        bookInfoList.clear();
        bookNameList.clear();

        // 遍历解析每本书的信息
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();

            BookInfo bookInfo = new BookInfo();
            bookInfo.setBookId(JsonUtil.getString(jsonObject, bookInfoRules.getBookIdField()));
            bookInfo.setBookName(JsonUtil.getString(jsonObject, bookInfoRules.getBookNameField()));
            bookInfo.setBookAuthor(JsonUtil.getString(jsonObject, bookInfoRules.getBookAuthorField()));
            bookInfo.setBookDesc(JsonUtil.getString(jsonObject, bookInfoRules.getBookDescField()));
            bookInfo.setBookImgUrl(JsonUtil.getString(jsonObject, bookInfoRules.getBookImgUrlField()));
            bookInfo.setBookUrl(JsonUtil.getString(jsonObject, bookInfoRules.getBookUrlField()));

            bookInfoList.add(bookInfo);
            bookNameList.add(bookInfo.getBookName());
        }
    }

    /**
     * 显示书籍列表对话框
     */
    private void showBookListDialog() {
        // 创建列表组件
        JBList<String> searchBookList = new JBList<>(bookNameList);
        searchBookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchBookList.addListSelectionListener(this::handleBookSelection);

        // 添加滚动条
        JBScrollPane scrollPane = new JBScrollPane(searchBookList);
        scrollPane.setPreferredSize(new Dimension(350, 500));

        // 显示对话框
        MessageDialogUtil.showMessageDialog(
                project,
                "搜索结果",
                scrollPane,
                null,
                this::commonCancelOperation
        );
    }

    /**
     * 处理书籍选择事件
     *
     * @param e 选择事件
     */
    private void handleBookSelection(ListSelectionEvent e) {
        // 避免重复触发
        if (e.getValueIsAdjusting()) return;

        JBList<String> sourceList = (JBList<String>) e.getSource();
        int selectedIndex = sourceList.getSelectedIndex();

        // 验证索引有效性
        if (selectedIndex >= 0 && selectedIndex < bookInfoList.size()) {
            processSelectedBook(selectedIndex);
        }
    }

    /**
     * 处理选中的书籍
     *
     * @param selectedIndex 选中的索引
     */
    private void processSelectedBook(int selectedIndex) {
        // 获取选中的书籍信息
        BookInfo selectedBookInfo = bookInfoList.get(selectedIndex);
        cacheService.setTempSelectedBookInfo(selectedBookInfo);

        // 获取目录规则
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules listMainRules = siteBean.getListMainRules();

        if (listMainRules == null) {
            Messages.showErrorDialog(ConstUtil.WREADER_CACHE_ERROR, "提示");
            return;
        }

        // 构建目录URL并获取目录
        String listMainUrl = getListMainUrl(
                listMainRules.getUrl(),
                siteBean.getSearchRules(),
                selectedBookInfo,
                siteBean
        );

        getListMainBackTask(listMainUrl);
        cacheService.setChapterContentList(null);
    }
    //endregion

    // ==================== 目录相关方法 ====================

    //region 目录相关方法

    /**
     * 获取目录主URL
     *
     * @param url         URL模板或脚本
     * @param searchRules 搜索规则
     * @param bookInfo    书籍信息
     * @param siteBean    站点信息
     * @return 完整的目录URL
     */
    private String getListMainUrl(String url, SearchRules searchRules,
                                  BookInfo bookInfo, SiteBean siteBean) {

        // 判断是否为脚本代码配置，是则执行脚本返回目录url，否则使用模板生成目录URL
        return ScriptCodeUtil.isJavaCodeConfig(url) ?
                executeListMainUrlScript(url, bookInfo) :
                buildListMainUrlFromTemplate(url, searchRules, bookInfo, siteBean);
    }

    /**
     * 执行目录URL生成脚本
     *
     * @param script   脚本配置
     * @param bookInfo 书籍信息
     * @return 生成的URL
     */
    private String executeListMainUrlScript(String script, BookInfo bookInfo) {
        try {
            // 根据脚本内容决定参数类型
            Class<?>[] paramTypes = script.contains("com.wei.wreader.pojo.BookInfo") ?
                    new Class[]{BookInfo.class} : new Class[]{String.class};
            Object[] params = script.contains("com.wei.wreader.pojo.BookInfo") ?
                    new Object[]{bookInfo} : new Object[]{bookInfo.getBookId()};

            return (String) ScriptCodeUtil.getScriptCodeExeResult(
                    script, paramTypes, params, Map.of("bookInfo", bookInfo)
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 从模板构建目录URL
     *
     * @param url         URL模板
     * @param searchRules 搜索规则
     * @param bookInfo    书籍信息
     * @param siteBean    站点信息
     * @return 构建的URL
     */
    private String buildListMainUrlFromTemplate(String url, SearchRules searchRules,
                                                BookInfo bookInfo, SiteBean siteBean) {
        // 判断获取目录的方式
        if (StringUtils.isNotBlank(url) && !siteBean.isHasHtml()) {
            return StringTemplateEngine.render(url, Map.of("bookId", bookInfo.getBookId()));
        } else {
            String listUrl = bookInfo.getBookUrl();
            // 判断是否需要拼接基础URL
            if (!isValidUrl(listUrl) && !JsonUtil.isValid(listUrl)) {
                return searchRules.getUrl() + listUrl;
            }
            return listUrl;
        }
    }

    /**
     * 验证URL是否有效
     *
     * @param url 待验证的URL
     * @return 是否为有效URL
     */
    private boolean isValidUrl(String url) {
        return url.startsWith(ConstUtil.HTTP_SCHEME) || url.startsWith(ConstUtil.HTTPS_SCHEME);
    }

    /**
     * 启动获取目录后台任务
     *
     * @param url 目录URL
     */
    private void getListMainBackTask(String url) {
        new GetListMainTask(url).queue();
    }
    //endregion

    // ==================== 内容处理方法 ====================

    //region 内容处理方法

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
     *
     * @param content    原始内容
     * @param handleRule 处理规则脚本
     * @return 处理后的内容
     * @throws Exception 执行异常
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

    /**
     * 应用内容正则表达式规则
     *
     * @param content   原始内容
     * @param regexList 正则表达式规则列表
     * @return 应用规则后的内容
     */
    private String applyContentRegexRules(String content, List<String> regexList) {
        // 基础格式化：替换换行符和制表符
        String result = content.replaceAll("\\n", "<br/>")
                .replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");

        // 应用自定义正则规则
        if (ListUtil.isNotEmpty(regexList)) {
            for (String contentRegex : regexList) {
                String[] parts = contentRegex.split(ConstUtil.SPLIT_REGEX_REPLACE_FLAG);
                String regex = parts[0];
                String replacement = parts.length > 1 ? parts[1] : "";
                result = result.replaceAll(regex, replacement);
            }
        }

        return result;
    }
    //endregion

    // ==================== UI更新方法 ====================

    //region UI更新方法

    /**
     * 构建书籍目录对话框
     *
     * @param chapterList    章节名称列表
     * @param chapterUrlList 章节URL列表
     * @param bodyElement    HTML页面body元素
     * @param bodyContentStr 页面内容字符串
     * @param isUpdate       是否为更新操作
     */
    private void buildBookDirectoryDialog(List<String> chapterList,
                                          List<String> chapterUrlList,
                                          Element bodyElement,
                                          String bodyContentStr,
                                          boolean isUpdate) {
        // 重置编辑器滚动位置
        cacheService.setEditorMessageVerticalScrollValue(0);

        if (isUpdate) {
            // 更新模式：只更新列表数据
            updateChapterList(chapterList, chapterUrlList);
        } else {
            // 创建模式：创建新的列表组件
            createChapterList(chapterList, chapterUrlList);
            // 加载下一页目录并在完成后显示对话框
            loadNextListMain(bodyContentStr, bodyElement, () -> showChapterDialog());
        }
    }

    /**
     * 更新章节列表数据
     *
     * @param newChapters 新章节名称列表
     * @param newUrls     新章节URL列表
     */
    private void updateChapterList(List<String> newChapters, List<String> newUrls) {
        // 添加新章节到数据模型
        for (int i = 0; i < newChapters.size(); i++) {
            LabelValueItem item = new LabelValueItem(newChapters.get(i), newUrls.get(i));
            chapterListModel.addElement(item);
        }

        // 重新设置监听器以确保捕获最新数据引用
        resetChapterListListener(newChapters, newUrls);
    }

    /**
     * 重新设置章节列表监听器
     *
     * @param chapters 章节名称列表
     * @param urls     章节URL列表
     */
    private void resetChapterListListener(List<String> chapters, List<String> urls) {
        // 移除现有的所有监听器，避免闭包捕获过期数据
        ListSelectionListener[] listeners = chapterListJBList.getListSelectionListeners();
        for (ListSelectionListener listener : listeners) {
            chapterListJBList.removeListSelectionListener(listener);
        }

        // 添加新的监听器，捕获当前的列表引用
        chapterListJBList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleChapterSelection(chapters, urls);
            }
        });
    }

    /**
     * 创建章节列表组件
     *
     * @param chapters 章节名称列表
     * @param urls     章节URL列表
     */
    private void createChapterList(List<String> chapters, List<String> urls) {
        // 创建数据模型
        chapterListModel = new DefaultListModel<>();
        for (int i = 0; i < chapters.size(); i++) {
            LabelValueItem item = new LabelValueItem(chapters.get(i), urls.get(i));
            chapterListModel.addElement(item);
        }

        // 创建列表组件
        chapterListJBList = new JBList<>(chapterListModel);
        chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 添加选择监听器
        chapterListJBList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleChapterSelection(chapters, urls);
            }
        });

        // 设置自定义单元格渲染器
        chapterListJBList.setCellRenderer(new ChapterListCellRenderer());
    }

    /**
     * 显示章节对话框
     */
    private void showChapterDialog() {
        JBScrollPane scrollPane = new JBScrollPane(chapterListJBList);
        scrollPane.setPreferredSize(new Dimension(400, 500));
        MessageDialogUtil.showMessageDialog(
                project,
                "目录",
                scrollPane,
                null,
                this::commonCancelOperation
        );
    }

    /**
     * 处理章节选择事件
     *
     * @param chapterList    章节名称列表
     * @param chapterUrlList 章节URL列表
     */
    private void handleChapterSelection(List<String> chapterList, List<String> chapterUrlList) {
        // 缓存站点信息
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        cacheService.setTempSelectedSiteBean(siteBean);

        // 停止相关服务
        OperateActionRefactored operateAction = OperateActionRefactored.getInstance(project);
        operateAction.executorServiceShutdown();
        operateAction.stopTTS();
        cacheService.setEditorMessageVerticalScrollValue(0);

        // 获取相关数据
        BookInfo bookInfo = cacheService.getTempSelectedBookInfo();
        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
        int selectedIndex = chapterListJBList.getSelectedIndex();

        // 构建章节URL
        String chapterTitle = chapterList.get(selectedIndex);
        String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
        String chapterUrl = buildFullChapterUrl(chapterSuffixUrl, siteBean.getBaseUrl());

        // 设置章节信息
        chapterInfo.setChapterTitle(chapterTitle);
        chapterInfo.setChapterUrl(chapterUrl);

        // 获取章节内容
        searchBookContentRemote(chapterUrl, param -> {
            processChapterContent(param, chapterInfo, selectedIndex,
                    chapterList, chapterUrlList, siteBean);
        });
    }

    /**
     * 构建完整的章节URL
     *
     * @param suffixUrl URL后缀
     * @param baseUrl   基础URL
     * @return 完整的URL
     */
    private String buildFullChapterUrl(String suffixUrl, String baseUrl) {
        // 判断是否已经是完整URL
        if (suffixUrl.startsWith(ConstUtil.HTTP_SCHEME) ||
                suffixUrl.startsWith(ConstUtil.HTTPS_SCHEME) ||
                JsonUtil.isValid(suffixUrl)) {
            return suffixUrl;
        }
        return baseUrl + suffixUrl;
    }

    /**
     * 处理章节内容
     *
     * @param param          内容参数
     * @param chapterInfo    章节信息
     * @param selectedIndex  选中索引
     * @param chapterList    章节列表
     * @param chapterUrlList 章节URL列表
     * @param siteBean       站点信息
     */
    private void processChapterContent(SearchBookCallParam param, ChapterInfo chapterInfo,
                                       int selectedIndex, List<String> chapterList,
                                       List<String> chapterUrlList, SiteBean siteBean) {
        // 设置章节内容
        chapterInfo.setChapterContent(param.getChapterContentHtml());
        chapterInfo.setChapterContentStr(param.getChapterContentText());
        chapterInfo.setSelectedChapterIndex(selectedIndex);

        // 缓存选择信息
        cacheSelectionInfo(siteBean);
        cacheService.setChapterList(chapterList);
        cacheService.setChapterUrlList(chapterUrlList);
        cacheService.setSelectedChapterInfo(chapterInfo);

        // 更新自定义站点规则缓存
        String tempRuleKey = customSiteRuleCacheServer.getTempSelectedCustomSiteRuleKey();
        customSiteRuleCacheServer.setSelectedCustomSiteRuleKey(tempRuleKey);

        // 更新显示内容
        updateContentText();
        loadThisChapterNextContent(chapterInfo.getChapterUrl(), param.getBodyContentStr());

        // 设置数据加载模式
        settings.setDataLoadType(Settings.DATA_LOAD_TYPE_NETWORK);
        cacheService.setSettings(settings);
    }

    /**
     * 缓存选择信息
     *
     * @param siteBean 站点信息
     */
    private void cacheSelectionInfo(SiteBean siteBean) {
        cacheService.setSelectedSiteBean(siteBean);
        cacheService.setSelectedBookSiteIndex(cacheService.getTempSelectedBookSiteIndex());
        cacheService.setSelectedBookInfo(cacheService.getTempSelectedBookInfo());
    }
    //endregion

    // ==================== 显示更新方法 ====================

    //region 显示更新方法

    /**
     * 更新侧边栏ToolWindow内容显示
     */
    private void updateToolWindowContent() {
        // 初始化章节行号信息
        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
        chapterInfo.initLineNum(1, 1, 1);
        cacheService.setSelectedChapterInfo(chapterInfo);

        // 更新工具窗口内容
        ToolWindowUtil.updateContentText(project, textPane -> {
            SiteBean siteBean = cacheService.getSelectedSiteBean();
            String content = buildStyledContent(siteBean, chapterInfo.getChapterContent());
            textPane.setText(content);
            textPane.setCaretPosition(0);
        });
    }

    /**
     * 更新状态栏内容显示
     */
    private void updateStatusBarContent() {
        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
        chapterInfo.initLineNum(1, 1, 1);
        WReaderStatusBarWidget.update(project, "");
    }

    /**
     * 构建带样式的章节内容
     *
     * @param siteBean       站点信息
     * @param chapterContent 章节内容
     * @return 带样式的HTML内容
     */
    private String buildStyledContent(SiteBean siteBean, String chapterContent) {
        String fontColor = cacheService.getFontColorHex();
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();

        // 判断是否使用原始样式
        if (siteBean.getChapterRules().isUseContentOriginalStyle()) {
            chapterContent = String.format("""
                    <div class="%s" style="color:%s;font-size:%dpx;">%s</div>
                    """, ConstUtil.NEW_FONT_CLASS_NAME, fontColor, fontSize, chapterContent);

            BookInfo bookInfo = cacheService.getSelectedBookInfo();
            return StringUtil.buildFullHtml(bookInfo.getBookName(), contentOriginalStyle, chapterContent);
        } else {
            return String.format("""
                    <div style="color:%s;font-family:'%s';font-size:%dpx;">%s</div>
                    """, fontColor, fontFamily, fontSize, chapterContent);
        }
    }
    //endregion

    // ==================== 通用辅助方法 ====================

    //region 通用辅助方法

    /**
     * 通用取消操作处理
     */
    private void commonCancelOperation() {
        // 通用取消操作处理逻辑
    }

    /**
     * 取消之前的任务
     *
     * @param task 要取消的任务
     */
    private void cancelPreviousTask(Task.Backgroundable task) {
        if (task != null) {
            task.onCancel();
        }
    }

    /**
     * 获取带样式的内容
     *
     * @param text 原始文本
     * @return 带样式的HTML内容
     */
    private static String getContent(String text) {
        CacheService cacheService = CacheService.getInstance();
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColor = cacheService.getFontColorHex();
        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();

        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColor + ";";

        return "<h3 style=\"text-align: center;margin-bottom: 20px;color:" + fontColor + ";\">" +
                chapterInfo.getChapterTitle() + "</h3>" +
                "<div style=\"" + style + "\">" + text + "</div>";
    }
    //endregion

    // ==================== 内部类定义 ====================

    //region 内部类定义

    /**
     * 章节列表单元格渲染器
     * 用于自定义列表项的显示方式
     */
    private static class ChapterListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            // 处理LabelValueItem类型的值
            if (value instanceof LabelValueItem) {
                LabelValueItem item = (LabelValueItem) value;
                super.getListCellRendererComponent(list, item.getLabel(), index, isSelected, cellHasFocus);
            } else {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
            return this;
        }
    }

    /**
     * 搜索小说后台任务
     * 负责在后台执行搜索操作并更新UI
     */
    private class SearchBookTask extends Task.Backgroundable {
        private final String searchUrl;
        private String searchResult = "";
        private Exception error = null;

        public SearchBookTask(String searchUrl) {
            super(project, SEARCH_TASK_TITLE);
            this.searchUrl = searchUrl;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText(SEARCH_TASK_TITLE);
            indicator.setIndeterminate(true);

            try {
                searchResult = searchBookList(searchUrl);
            } catch (Exception e) {
                error = e;
            }
        }

        @Override
        public void onSuccess() {
            if (error != null) {
                Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
            } else if (searchResult != null) {
                // 验证搜索结果
                if (StringUtils.isBlank(searchResult) ||
                        ConstUtil.STR_ONE.equals(searchResult) ||
                        "[]".equals(searchResult)) {
                    Messages.showInfoMessage(ConstUtil.WREADER_SEARCH_BOOK_ERROR, "提示");
                }

                // 重新初始化数据
                bookInfoList.clear();
                bookNameList.clear();

                // 处理并显示搜索结果
                handleBookList(searchResult);
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
    }

    /**
     * 获取目录后台任务
     * 负责在后台获取小说目录信息
     */
    private class GetListMainTask extends Task.Backgroundable {
        private final String url;
        private boolean isSuccess = false;
        private List<String> tempChapterList = new ArrayList<>();
        private List<String> tempChapterUrlList = new ArrayList<>();
        private String bodyStr = "";
        private Element bodyElement = null;

        public GetListMainTask(String url) {
            super(project, DIRECTORY_TASK_TITLE);
            this.url = url;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText(DIRECTORY_TASK_TITLE);
            indicator.setIndeterminate(true);

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
    }

    /**
     * 内容加载后台任务
     * 负责在后台加载和处理章节内容
     */
    private class ContentLoadTask extends Task.Backgroundable {
        private final String url;
        private final Consumer<SearchBookCallParam> callback;
        private String chapterContent = "";
        private Element bodyElement;
        private String bodyContentStr;
        private String chapterContentText;

        public ContentLoadTask(String url, Consumer<SearchBookCallParam> callback) {
            super(project, CONTENT_TASK_TITLE);
            this.url = url;
            this.callback = callback;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText(CONTENT_TASK_TITLE);
            indicator.setIndeterminate(true);

            SiteBean siteBean = cacheService.getTempSelectedSiteBean();
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
            HttpRequestBase request = HttpUtil.commonRequest(url);
            request.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

            try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    String result = EntityUtils.toString(entity);
                    JsonObject json = new Gson().fromJson(result, JsonObject.class);

                    // 使用JSONPath提取内容
                    Object contentObj = JsonPath.read(json.toString(), rules.getUrlDataRule());
                    chapterContent = contentObj.toString();
                    bodyContentStr = result;
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
                boolean useOriginalStyle = rules.isUseContentOriginalStyle();

                // 处理原始样式
                if (useOriginalStyle) {
                    extractOriginalStyles(headElement, rules);
                }

                // 提取内容
                bodyElement = document.body();
                Elements contentElements = bodyElement.select(rules.getContentElementName());

                if (contentElements.isEmpty()) {
                    Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_BOOK_CONTENT_ERROR, "提示");
                    return;
                }

                chapterContent = buildContentHtml(contentElements);
                bodyContentStr = bodyElement.html();
                chapterContentText = contentElements.text();

            } catch (IOException e) {
                Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                throw new RuntimeException(e);
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
                // 去除样式中的HTML标签
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
                // 处理内容
                chapterContent = handleContent(chapterContent);
                chapterContent = buildChapterTitleContent(chapterContent);
                chapterContentText = processChapterTextContent(chapterContent);

                // 构造回调参数
                SearchBookCallParam param = new SearchBookCallParam();
                param.setBodyElement(bodyElement);
                param.setBodyContentStr(bodyContentStr);
                param.setChapterContentHtml(chapterContent);
                param.setChapterContentText(chapterContentText);
                callback.accept(param);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 构建带章节标题的内容
         */
        private String buildChapterTitleContent(String content) {
            ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
            String fontColor = cacheService.getFontColorHex();
            return "<h3 style=\"text-align: center;margin-bottom: 20px;color:" +
                    fontColor + ";\">" + chapterInfo.getChapterTitle() + "</h3>" + content;
        }

        /**
         * 处理章节文本内容
         */
        private String processChapterTextContent(String htmlContent) {
            Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
            String text = pattern.matcher(htmlContent).replaceAll("　");
            text = StringUtils.normalizeSpace(text);
            return StringEscapeUtils.unescapeHtml4(text);
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            error.printStackTrace();
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
        }
    }

    /**
     * 下一页目录加载任务
     * 负责异步加载后续目录页面
     */
    private class NextListMainTask extends Task.Backgroundable {
        private final AtomicReference<String> bodyElementStrRef;
        private final Element initialBodyElement;
        private final Runnable callback;
        private volatile boolean isRunning = true;
        private String returnResult;
        private final List<String> tempChapterList = new ArrayList<>();
        private final List<String> tempChapterUrlList = new ArrayList<>();

        public NextListMainTask(String bodyElementStr, Element bodyElement, Runnable callback) {
            super(project, NEXT_PAGE_DIRECTORY_TITLE);
            this.bodyElementStrRef = new AtomicReference<>(bodyElementStr);
            this.initialBodyElement = bodyElement;
            this.callback = callback;
            this.returnResult = getNextListMainUrl();
        }

        private String getNextListMainUrl() {
            SiteBean siteBean = cacheService.getTempSelectedSiteBean();
            return siteBean.getListMainRules().getNextListMainUrl();
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setIndeterminate(true);

            try {
                String baseUrl = cacheService.getTempSelectedSiteBean().getBaseUrl();
                String previousUrl = "";
                int pageIndex = 1;
                AtomicReference<Element> currentBodyElement = new AtomicReference<>(initialBodyElement);

                // 循环加载下一页直到没有更多内容
                while (isRunning) {
                    indicator.checkCanceled();
                    pageIndex++;
                    indicator.setText2("正在加载第" + pageIndex + "页...");

                    // 执行动态脚本获取下一页URL
                    returnResult = executeNextListMainScript(baseUrl, pageIndex,
                            previousUrl, bodyElementStrRef.get(), currentBodyElement.get());

                    previousUrl = returnResult;

                    // 如果返回空结果，则停止加载
                    if (StringUtils.isEmpty(returnResult)) {
                        isRunning = false;
                        break;
                    }

                    // 请求下一页内容
                    Map<String, List<String>> listMap = requestNextListMain(returnResult, param -> {
                        bodyElementStrRef.set(param.getBodyContentStr());
                        currentBodyElement.set(param.getBodyElement());
                        // Lambda表达式中不能修改外部非final变量
                        // currentBodyElement的更新需要在其他地方处理
                    });

                    tempChapterList.addAll(listMap.get("chapterList"));
                    tempChapterUrlList.addAll(listMap.get("chapterUrlList"));

                    // 添加延迟避免请求过于频繁
                    Thread.sleep(1000);
                }
            } catch (ProcessCanceledException e) {
                // 用户取消操作
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog("下一页目录加载失败: " + e.getMessage(), "提示")
                );
            }
        }

        /**
         * 执行下一页目录脚本
         */
        private String executeNextListMainScript(String baseUrl, int pageIndex,
                                                 String previousUrl, String bodyStr, Element bodyElement) {
            try {

                // 执行动态代码，获取下一页目录的链接（五个参数：基础网址、当前页码、上一页Url、主体信息字符串、页面主体元素对象）
                return (String) ScriptCodeUtil.getScriptCodeExeResult(
                        returnResult,
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
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onSuccess() {
            // 更新目录列表
            buildBookDirectoryDialog(tempChapterList, tempChapterUrlList, true);
            if (callback != null) {
                callback.run();
            }
        }

        @Override
        public void onCancel() {
            isRunning = false;
            super.onCancel();
        }
    }

    /**
     * 下一页内容加载任务
     * 负责异步加载章节的后续内容页面
     */
    private class NextContentTask extends Task.Backgroundable {
        private final String chapterUrl;
        private final String initialBodyContent;
        private volatile boolean isRunning = true;
        private String returnResult;
        private final String nextContentUrl;
        private final StringBuilder nextContent = new StringBuilder();

        public NextContentTask(String chapterUrl, String bodyContent) {
            super(project, NEXT_PAGE_CONTENT_TITLE);
            this.chapterUrl = chapterUrl;
            this.initialBodyContent = bodyContent;
            this.nextContentUrl = getNextContentUrl();
            this.returnResult = nextContentUrl;
        }

        private String getNextContentUrl() {
            SiteBean siteBean = cacheService.getSelectedSiteBean();
            return siteBean.getChapterRules().getNextContentUrl();
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setIndeterminate(true);

            try {
                String baseUrl = cacheService.getSelectedSiteBean().getBaseUrl();
                String previousContentUrl = "";
                AtomicReference<String> previousPageContent = new AtomicReference<>(initialBodyContent);
                int pageCount = 0;

                // 循环加载下一页内容
                while (isRunning) {
                    indicator.checkCanceled();
                    pageCount++;
                    indicator.setText2("正在加载第 " + pageCount + " 页...");

                    // 执行脚本获取下一页URL
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

                    // 添加延迟避免请求过于频繁
                    Thread.sleep(1000);
                }
            } catch (ProcessCanceledException e) {
                // 用户取消操作
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog("本章节下一页内容加载失败: " + e.getMessage(), "提示")
                );
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
        public void onSuccess() {
            ToolWindowUtil.updateContentText(project, contentTextPanel -> {
                String text = nextContent.toString();
                int caretPosition = contentTextPanel.getCaretPosition();
                text = getContent(text);
                contentTextPanel.setText(text);
                contentTextPanel.setCaretPosition(caretPosition);

                updateChapterInfo(text);
            });
        }

        /**
         * 更新章节信息
         */
        private void updateChapterInfo(String content) {
            SiteBean siteBean = cacheService.getSelectedSiteBean();
            ChapterRules chapterRules = siteBean.getChapterRules();

            Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
            String chapterContentText = pattern.matcher(content).replaceAll("　");
            chapterContentText = StringUtils.normalizeSpace(chapterContentText);
            chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);
            chapterContentText = applyContentRegexRules(chapterContentText, chapterRules.getContentRegexList());

            ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
            chapterInfo.setChapterContent(content);
            chapterInfo.setChapterContentStr(chapterContentText);
            cacheService.setSelectedChapterInfo(chapterInfo);
        }

        @Override
        public void onCancel() {
            isRunning = false;
            super.onCancel();
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            if (!(error instanceof ProcessCanceledException)) {
                super.onThrowable(error);
            }
        }
    }
    //endregion

    // ==================== HTML搜索方法实现 ====================

    //region HTML搜索方法实现

    /**
     * 通过HTML页面搜索小说列表
     *
     * @param url           请求URL
     * @param searchRules   搜索规则
     * @param bookInfoRules 书籍信息规则
     * @return 搜索结果JSON字符串
     */
    private String searchBookListHtml(String url, SearchRules searchRules, BookInfoRules bookInfoRules) {
        // 解析HTTP请求配置
        HttpRequestConfigParser parser = new HttpRequestConfigParser(url);
        String requestUrl = buildRequestUrl(parser);
        String requestMethod = parser.getMethod();
        Map<String, String> queryParams = parser.getQueryParams();
        Map<String, String> bodyParams = parser.getBodyParams();
        Map<String, String> headers = parser.getHeader();

        // 执行HTTP请求
        Document document = executeHtmlRequest(requestUrl, requestMethod, queryParams, bodyParams, headers);

        // 从HTML中提取书籍列表
        return extractBookListFromHtml(document, searchRules, bookInfoRules);
    }

    /**
     * 构建请求URL
     *
     * @param parser HTTP请求配置解析器
     * @return 完整的请求URL
     */
    private String buildRequestUrl(HttpRequestConfigParser parser) {
        String baseUrl = parser.getUrl();
        Map<String, String> queryParams = parser.getQueryParams();

        // 添加查询参数
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder(baseUrl).append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            return urlBuilder.toString();
        }
        return baseUrl;
    }

    /**
     * 执行HTML请求
     *
     * @param url         请求URL
     * @param method      请求方法
     * @param queryParams 查询参数
     * @param bodyParams  请求体参数
     * @param headers     请求头
     * @return 解析后的文档对象
     */
    private Document executeHtmlRequest(String url, String method, Map<String, String> queryParams,
                                        Map<String, String> bodyParams, Map<String, String> headers) {
        try {
            Connection connection = Jsoup.connect(url);
            // 设置请求头
            if (headers != null && !headers.isEmpty()) {
                connection.headers(headers);
            }

            // 设置请求方法和参数
            if (HttpUtil.POST.equals(method)) {
                connection.method(Connection.Method.POST);
                if (bodyParams != null) {
                    for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                        connection.data(entry.getKey(), entry.getValue());
                    }
                }
                connection.header("Content-Type", "application/x-www-form-urlencoded");
            } else {
                connection.method(Connection.Method.GET);
            }

            return connection.execute().parse();
        } catch (IOException e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }
    }

    /**
     * 从HTML中提取书籍列表
     *
     * @param document      HTML文档
     * @param searchRules   搜索规则
     * @param bookInfoRules 书籍信息规则
     * @return 书籍列表JSON字符串
     */
    private String extractBookListFromHtml(Document document, SearchRules searchRules, BookInfoRules bookInfoRules) {
        // 解析URL元素规则
        String bookListUrlElement = StringUtils.defaultIfBlank(searchRules.getBookListUrlElement(), "a");
        String[] urlElementRules = bookListUrlElement.split("@");
        String cssSelector = urlElementRules[0];
        String urlRuleBack = extractRulePart(urlElementRules, ConstUtil.CSS_QUERY_BACK_FLAG);
        String urlRuleFront = extractRulePart(urlElementRules, ConstUtil.CSS_QUERY_FONT_FLAG);

        JsonArray jsonArray = new JsonArray();
        Elements elements = document.select(searchRules.getBookListElementName());
        String location = document.location();
        String titleElement = searchRules.getBookListTitleElement();

        // 遍历提取每本书的信息
        for (Element itemElement : elements) {
            if (itemElement != null) {
                JsonObject bookJson = createBookJsonObject(itemElement, cssSelector, titleElement,
                        urlRuleFront, urlRuleBack, location, bookInfoRules);
                jsonArray.add(bookJson);
            }
        }

        return jsonArray.toString();
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

    /**
     * 创建书籍JSON对象
     *
     * @param itemElement   列表项元素
     * @param urlSelector   URL选择器
     * @param titleSelector 标题选择器
     * @param urlFront      URL前缀
     * @param urlBack       URL后缀
     * @param location      页面位置
     * @param rules         书籍信息规则
     * @return 书籍JSON对象
     */
    private JsonObject createBookJsonObject(Element itemElement, String urlSelector, String titleSelector,
                                            String urlFront, String urlBack, String location, BookInfoRules rules) {
        String bookUrl = extractElementAttribute(itemElement, urlSelector, "href");
        String bookName = extractElementText(itemElement, titleSelector);

        try {
            bookUrl = UrlUtil.buildFullURL(location, urlFront + bookUrl + urlBack);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(rules.getBookNameField(), bookName);
        jsonObject.addProperty(rules.getBookUrlField(), bookUrl);
        jsonObject.addProperty(rules.getBookAuthorField(), "");
        jsonObject.addProperty(rules.getBookDescField(), "");
        jsonObject.addProperty(rules.getBookImgUrlField(), "");
        return jsonObject;
    }

    /**
     * 提取元素属性
     *
     * @param parent    父元素
     * @param selector  选择器
     * @param attribute 属性名
     * @return 属性值
     */
    private String extractElementAttribute(Element parent, String selector, String attribute) {
        Element element = parent.selectFirst(selector);
        return element != null ? element.attr(attribute) : "";
    }

    /**
     * 提取元素文本
     *
     * @param parent   父元素
     * @param selector 选择器
     * @return 文本内容
     */
    private String extractElementText(Element parent, String selector) {
        Element element = parent.selectFirst(selector);
        return element != null ? element.text() : "";
    }
    //endregion

    // ==================== API搜索方法实现 ====================

    //region API搜索方法实现

    /**
     * 通过API接口搜索小说列表
     *
     * @param url         请求URL
     * @param searchRules 搜索规则
     * @return 搜索结果JSON字符串
     */
    private String searchBookListApi(String url, SearchRules searchRules) {
        HttpRequestBase request = HttpUtil.commonRequest(url);
        request.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);

                // 使用JSONPath提取书籍列表
                Object bookListObj = JsonPath.read(result, searchRules.getDataBookListRule());
                return Objects.isNull(bookListObj) ? "" : bookListObj.toString();
            }
        } catch (IOException e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }

        return "";
    }
    //endregion

    // ==================== 目录HTML方法实现 ====================

    //region 目录HTML方法实现

    /**
     * 通过HTML页面获取目录信息
     *
     * @param url           请求URL
     * @param siteBean      站点信息
     * @param listMainRules 目录规则
     * @return 目录信息Map
     */
    private Map<String, Object> searchBookDirectoryHtml(String url, SiteBean siteBean, ListMainRules listMainRules) {
        String bodyStr = "";
        List<String> chapterList = new ArrayList<>();
        List<String> chapterUrlList = new ArrayList<>();

        BookInfo bookInfo = cacheService.getTempSelectedBookInfo();
        String dataRule = listMainRules.getUrlDataRule();

        HttpRequestBase request = HttpUtil.commonRequest(url);
        request.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);

                // 使用JSONPath提取目录数据
                Object jsonResult = JsonPath.read(result, dataRule);
                Gson gson = new Gson();
                String itemListStr = gson.toJson(jsonResult);
                JsonArray jsonArray = gson.fromJson(itemListStr, JsonArray.class);

                Map<String, String> paramMap = Map.of(
                        "dataJsonStr", result,
                        "menuListJsonStr", itemListStr
                );

                ChapterRules chapterRules = siteBean.getChapterRules();
                String chapterUrl = chapterRules.getUrl();
                boolean useJavaCode = ScriptCodeUtil.isJavaCodeConfig(chapterUrl);

                List<String> itemIdList = new ArrayList<>();
                List<Integer> itemIndexList = new ArrayList<>();

                // 处理目录项
                processDirectoryItems(jsonArray, listMainRules, chapterRules, bookInfo,
                        itemIdList, itemIndexList, chapterList, chapterUrlList, paramMap, useJavaCode);

                // 如果使用Java代码配置，则执行脚本生成URL
                if (useJavaCode) {
                    chapterUrlList = executeChapterUrlScript(chapterUrl, paramMap, itemIndexList,
                            bookInfo.getBookId(), itemIdList);
                }

                bodyStr = result;
            }
        } catch (IOException e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }

        return Map.of(
                "bodyStr", bodyStr,
                "bodyElement", "",
                "chapterList", chapterList,
                "chapterUrlList", chapterUrlList
        );
    }

    /**
     * 处理目录项
     *
     * @param jsonArray      JSON数组
     * @param listMainRules  目录规则
     * @param chapterRules   章节规则
     * @param bookInfo       书籍信息
     * @param itemIdList     项目ID列表
     * @param itemIndexList  项目索引列表
     * @param chapterList    章节列表
     * @param chapterUrlList 章节URL列表
     * @param paramMap       参数映射
     * @param useJavaCode    是否使用Java代码
     */
    private void processDirectoryItems(JsonArray jsonArray, ListMainRules listMainRules,
                                       ChapterRules chapterRules, BookInfo bookInfo,
                                       List<String> itemIdList, List<Integer> itemIndexList,
                                       List<String> chapterList, List<String> chapterUrlList,
                                       Map<String, String> paramMap, boolean useJavaCode) {
        String itemIdField = listMainRules.getItemIdField();
        String itemTitleField = listMainRules.getItemTitleField();
        String chapterUrlTemplate = chapterRules.getUrl();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject itemJson = jsonArray.get(i).getAsJsonObject();
            String itemId = itemJson.get(itemIdField).getAsString();
            String title = itemJson.get(itemTitleField).getAsString();

            if (useJavaCode) {
                // Java代码配置模式
                itemIdList.add(itemId);
                itemIndexList.add(i);
                chapterList.add(title);
            } else {
                // 模板配置模式
                String itemUrl = buildItemUrl(chapterUrlTemplate, bookInfo.getBookId(), itemId, itemJson, listMainRules);
                chapterList.add(title);
                chapterUrlList.add(itemUrl);
            }
        }
    }

    /**
     * 构建项目URL
     *
     * @param chapterUrlTemplate URL模板
     * @param bookId             书籍ID
     * @param itemId             项目ID
     * @param itemJson           项目JSON
     * @param listMainRules      目录规则
     * @return 完整的项目URL
     */
    private String buildItemUrl(String chapterUrlTemplate, String bookId, String itemId,
                                JsonObject itemJson, ListMainRules listMainRules) {
        if (StringUtils.isNotBlank(chapterUrlTemplate)) {
            return StringTemplateEngine.render(chapterUrlTemplate, Map.of("bookId", bookId, "itemId", itemId));
        } else {
            String itemUrlField = listMainRules.getItemUrlField();
            return itemJson.get(itemUrlField).getAsString();
        }
    }

    /**
     * 执行章节URL生成脚本
     *
     * @param script        脚本配置
     * @param paramMap      参数映射
     * @param itemIndexList 项目索引列表
     * @param bookId        书籍ID
     * @param itemIdList    项目ID列表
     * @return 章节URL列表
     */
    private List<String> executeChapterUrlScript(String script, Map<String, String> paramMap,
                                                 List<Integer> itemIndexList, String bookId,
                                                 List<String> itemIdList) {
        try {
            return ScriptCodeUtil.getScriptCodeExeListResult(
                    script,
                    new Class[]{Map.class, List.class, String.class, List.class},
                    new Object[]{paramMap, itemIndexList, bookId, itemIdList},
                    Map.of(
                            "result", paramMap.get("dataJsonStr"),
                            "bookInfo", cacheService.getTempSelectedBookInfo(),
                            "itemIndexList", itemIndexList,
                            "itemIdList", itemIdList
                    )
            );
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_ERROR, "提示");
            throw new RuntimeException(e);
        }
    }
    //endregion

    // ==================== 目录API方法实现 ====================

    //region 目录API方法实现

    /**
     * 通过API接口获取目录信息
     *
     * @param url           请求URL
     * @param listMainRules 目录规则
     * @return 目录信息Map
     */
    private Map<String, Object> searchBookDirectoryApi(String url, ListMainRules listMainRules) {
        String bodyStr = "";
        Element bodyElement = null;
        List<String> chapterList = new ArrayList<>();
        List<String> chapterUrlList = new ArrayList<>();

        try {
            Document document = Jsoup.connect(url)
                    .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                    .get();

            String listMainElementName = listMainRules.getListMainElementName();
            Elements listMainElements = document.select(listMainElementName);
            bodyElement = document.body();
            bodyStr = bodyElement.html();
            String location = document.location();

            String urlElement = StringUtils.defaultIfBlank(listMainRules.getUrlElement(), "a");
            String titleElement = listMainRules.getTitleElement();

            for (Element element : listMainElements) {
                String chapterUrl = extractElementAttribute(element, urlElement, "href");
                String chapterTitle = extractElementText(element, titleElement);

                chapterList.add(chapterTitle);
                chapterUrl = UrlUtil.buildFullURL(location, chapterUrl);
                chapterUrlList.add(chapterUrl);
            }

        } catch (IOException e) {
            Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }

        return Map.of(
                "bodyStr", bodyStr,
                "bodyElement", bodyElement,
                "chapterList", chapterList,
                "chapterUrlList", chapterUrlList
        );
    }
    //endregion

    // ==================== 下一页目录请求方法 ====================

    //region 下一页目录请求方法

    /**
     * 请求下一页目录
     *
     * @param url      请求URL
     * @param callback 回调函数
     * @return 目录信息Map
     */
    public Map<String, List<String>> requestNextListMain(String url, Consumer<SearchBookCallParam> callback) {
        SiteBean siteBean = cacheService.getTempSelectedSiteBean();
        ListMainRules listMainRules = siteBean.getListMainRules();

        try {
            return listMainRules.isUseNextListMainApi() ?
                    requestNextListMainApi(url, listMainRules, callback) :
                    requestNextListMainHtml(url, listMainRules, callback);
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            callback.accept(new SearchBookCallParam());
            e.printStackTrace();
        }

        return Map.of("chapterList", new ArrayList<>(), "chapterUrlList", new ArrayList<>());
    }

    /**
     * 通过API方式请求下一页目录
     *
     * @param url      请求URL
     * @param rules    目录规则
     * @param callback 回调函数
     * @return 目录信息Map
     */
    private Map<String, List<String>> requestNextListMainApi(String url, ListMainRules rules,
                                                             Consumer<SearchBookCallParam> callback) {
        List<String> chapterTitles = new ArrayList<>();
        List<String> chapterUrls = new ArrayList<>();
        String bodyContent = "";
        Element bodyElement = null;
        SearchBookCallParam callParam = new SearchBookCallParam();

        HttpRequestBase request = HttpUtil.commonRequest(url);
        request.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                bodyContent = result;

                JsonObject resultJson = new Gson().fromJson(result, JsonObject.class);
                List<Map<String, Object>> items = JsonPath.read(resultJson.toString(),
                        rules.getNextListMainApiDataRule());

                processApiDirectoryItems(items, rules, result, chapterTitles, chapterUrls);
            }
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            callback.accept(callParam);
            throw new RuntimeException(e);
        }

        callParam.setBodyElement(bodyElement);
        callParam.setBodyContentStr(bodyContent);
        callback.accept(callParam);

        return Map.of("chapterList", chapterTitles, "chapterUrlList", chapterUrls);
    }

    /**
     * 处理API目录项
     *
     * @param items  目录项列表
     * @param rules  目录规则
     * @param result 响应结果
     * @param titles 标题列表
     * @param urls   URL列表
     */
    private void processApiDirectoryItems(List<Map<String, Object>> items, ListMainRules rules,
                                          String result, List<String> titles, List<String> urls) {
        for (Map<String, Object> itemMap : items) {
            String itemId = (String) itemMap.get(rules.getItemIdField());
            String title = (String) itemMap.get(rules.getItemTitleField());
            String itemUrl = (String) itemMap.get(rules.getItemUrlField());

            if (StringUtils.isNotBlank(itemUrl)) {
                urls.add(itemUrl);
            } else {
                itemUrl = executeUrlDataHandleScript(rules.getUrlDataHandleRule(), result, itemMap, itemId);
                urls.add(itemUrl);
            }
            titles.add(title);
        }
    }

    /**
     * 执行URL数据处理脚本
     *
     * @param script  脚本配置
     * @param result  响应结果
     * @param itemMap 项目映射
     * @param itemId  项目ID
     * @return 处理后的URL
     */
    private String executeUrlDataHandleScript(String script, String result,
                                              Map<String, Object> itemMap, String itemId) {
        try {
            return (String) ScriptCodeUtil.getScriptCodeExeResult(
                    script,
                    new Class[]{String.class, Map.class, String.class},
                    new Object[]{result, itemMap, itemId},
                    Map.of("result", result, "itemMap", itemMap, "itemId", itemId)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过HTML方式请求下一页目录
     *
     * @param url      请求URL
     * @param rules    目录规则
     * @param callback 回调函数
     * @return 目录信息Map
     */
    private Map<String, List<String>> requestNextListMainHtml(String url, ListMainRules rules,
                                                              Consumer<SearchBookCallParam> callback) {
        List<String> chapterTitles = new ArrayList<>();
        List<String> chapterUrls = new ArrayList<>();
        String bodyContent = "";
        Element bodyElement = null;
        SearchBookCallParam callParam = new SearchBookCallParam();

        try {
            Document document = Jsoup.connect(url)
                    .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                    .get();

            bodyElement = document.body();
            bodyContent = bodyElement.toString();
            String location = document.location();

            Elements elements = bodyElement.select(rules.getListMainElementName());
            if (elements.isEmpty()) {
                return new HashMap<>();
            }

            String urlElement = StringUtils.defaultIfBlank(rules.getUrlElement(), "a");
            String titleElement = rules.getTitleElement();

            for (Element element : elements) {
                String chapterUrl = extractElementAttribute(element, urlElement, "href");
                String chapterTitle = extractElementText(element, titleElement);

                chapterTitles.add(chapterTitle);
                chapterUrl = UrlUtil.buildFullURL(location, chapterUrl);
                chapterUrls.add(chapterUrl);
            }

        } catch (IOException e) {
            Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            callback.accept(callParam);
            throw new RuntimeException(e);
        }

        callParam.setBodyElement(bodyElement);
        callParam.setBodyContentStr(bodyContent);
        callback.accept(callParam);

        return Map.of("chapterList", chapterTitles, "chapterUrlList", chapterUrls);
    }
    //endregion

    // ==================== 内容请求方法 ====================

    //region 内容请求方法

    /**
     * 请求内容
     *
     * @param url      请求URL
     * @param callback 回调函数
     * @return 内容字符串
     */
    private String requestContent(String url, Consumer<String> callback) {
        String content = "";
        SiteBean siteBean = cacheService.getSelectedSiteBean();
        ChapterRules chapterRules = siteBean.getChapterRules();

        try {
            if (chapterRules.isUseNextContentApi()) {
                content = requestContentViaApi(url, callback, chapterRules);
            } else {
                content = requestContentViaHtml(url, callback, chapterRules);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return applyContentRegexRules(
                content.replaceAll("\\n", "<br/>").replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;"),
                chapterRules.getContentRegexList()
        );
    }

    /**
     * 通过API方式请求内容
     *
     * @param url      请求URL
     * @param callback 回调函数
     * @param rules    章节规则
     * @return 内容字符串
     */
    private String requestContentViaApi(String url, Consumer<String> callback, ChapterRules rules) {
        HttpRequestBase request = HttpUtil.commonRequest(url);
        request.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);

        try (CloseableHttpResponse response = HttpClients.createDefault().execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                JsonObject resultJson = new Gson().fromJson(result, JsonObject.class);

                Object contentObj = JsonPath.read(resultJson.toString(), rules.getNextContentApiDataRule());
                callback.accept(result);
                return (String) contentObj;
            }
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            callback.accept("");
            throw new RuntimeException(e);
        }

        return "";
    }

    /**
     * 通过HTML方式请求内容
     *
     * @param url      请求URL
     * @param callback 回调函数
     * @param rules    章节规则
     * @return 内容字符串
     */
    private String requestContentViaHtml(String url, Consumer<String> callback, ChapterRules rules) {
        try {
            Document document = Jsoup.connect(url)
                    .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                    .get();

            Element bodyElement = document.body();
            callback.accept(bodyElement.html());

            Elements contentElements = bodyElement.select(rules.getContentElementName());
            if (contentElements.isEmpty()) {
                return "";
            }

            StringBuilder contentHtml = new StringBuilder();
            for (Element element : contentElements) {
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
            callback.accept("");
            throw new RuntimeException(e);
        }
    }
    //endregion

    // ==================== 构建目录对话框方法 ====================

    //region 构建目录对话框方法

    /**
     * 构建书籍目录对话框
     *
     * @param param 搜索回调参数
     */
    private void buildBookDirectoryDialog(SearchBookCallParam param) {
        buildBookDirectoryDialog(
                param.getTempChapterList(),
                param.getTempChapterUrlList(),
                param.getBodyElement(),
                param.getBodyContentStr(),
                false
        );
    }

    /**
     * 构建书籍目录对话框
     *
     * @param chapterList    章节列表
     * @param chapterUrlList 章节URL列表
     */
    private void buildBookDirectoryDialog(List<String> chapterList, List<String> chapterUrlList) {
        buildBookDirectoryDialog(chapterList, chapterUrlList, false);
    }

    /**
     * 构建书籍目录对话框
     *
     * @param chapterList    章节列表
     * @param chapterUrlList 章节URL列表
     * @param isUpdate       是否为更新操作
     */
    private void buildBookDirectoryDialog(List<String> chapterList, List<String> chapterUrlList, boolean isUpdate) {
        buildBookDirectoryDialog(chapterList, chapterUrlList, null, null, isUpdate);
    }
    //endregion

}
