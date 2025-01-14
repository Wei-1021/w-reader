package com.wei.wreader.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.pojo.*;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.utils.UrlUtil;
import groovy.util.logging.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;


/**
 * 工具窗口视图
 *
 * @author weizhanjie
 */
@Log4j2
public class WReaderToolWindow  {

    //region 参数属性
    //region 组件
    /**
     * 阅读器面板
     */
    private JPanel readerPanel;
    /**
     * 菜单工具面板
     */
    private JPanel menuToolPanel;
    /**
     * 内容面板
     */
    private JPanel contentPanel;
    /**
     * 目录列表按钮
     */
    private JButton menuListButton;
    /**
     * 上一页按钮
     */
    private JButton prevPageButton;
    /**
     * 下一页按钮
     */
    private JButton nextPageButton;
    /**
     * 搜索按钮
     */
    private JButton searchBookButton;
    /**
     * 内容编辑器JEditorPane
     */
    private JEditorPane contentEditorPane1;
    /**
     * 内容编辑器JTextPane
     */
    private JTextPane contentTextPane;
    /**
     * 内容滚动面板
     */
    private JScrollPane contentScrollPane;
    /**
     * 字体大小减按钮
     */
    private JButton fontSubButton;
    /**
     * 字体大小加按钮
     */
    private JButton fontAddButton1;
    /**
     * 颜色显示面板
     */
    private JPanel colorShowPanel;
    private JPanel menuToolBarPanel;

    private JToolBar menuToolBar;
    //endregion
    //region 自定义参数
    /**
     * 书本名称列表
     */
    private final List<String> bookNameList = new ArrayList<>();
    /**
     * 书本信息列表
     */
    private final List<BookInfo> bookInfoList = new ArrayList<>();
    /**
     * 章节名称列表
     */
    private List<String> chapterList = new ArrayList<>();
    /**
     * 章节链接列表
     */
    private List<String> chapterUrlList = new ArrayList<>();
    /**
     * 章节链接列表
     */
    private List<String> chapterContentList = new ArrayList<>();
    /**
     * 当前章节索引
     */
    private int currentChapterIndex = 0;
    /**
     * font size
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
     * 基础网址
     */
    private String baseUrl;
    /**
     * 站点信息列表
     */
    private List<BookSiteInfo> siteList;
    /**
     * 选中的站点信息(默认第一个)
     */
    private BookSiteInfo selectedBookSiteInfo;
    /**
     * 选中的站点索引(默认第一个)
     */
    private int selectedBookSiteIndex;
    /**
     * 当前小说信息
     */
    private BookInfo selectBookInfo = new BookInfo();
    /**
     * 当前章节信息
     */
    private ChapterInfo currentChapterInfo = new ChapterInfo();
    private ConfigYaml configYaml;

    private CacheService cacheService;
    private Settings settings;
    private OperateActionUtil operateAction;
    //endregion
    //endregion

    public WReaderToolWindow(ToolWindow toolWindow) {
        configYaml = new ConfigYaml();
        cacheService = CacheService.getInstance();

        menuToolPanel.setVisible(false);
        menuToolBarPanel.setVisible(false);
        SwingUtilities.invokeLater(() -> {
            // 初始化组件
            initMenuToolTabs(toolWindow);
            // 初始化编辑器
            initContentTextArea(toolWindow);
            // 初始化数据
            initData(toolWindow);
            // 监听编辑器颜色修改
            appEditorColorsListener();
        });
    }

    public void initMenuToolTabs(ToolWindow toolWindow) {
        menuToolBarPanel.setVisible(true);
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup toolWindowBarGroup = (ActionGroup) actionManager.getAction(ConstUtil.WREADER_GROUP_TOOL_WINDOW_BAR_ID);
        ActionToolbar actionToolbar = actionManager.createActionToolbar(ConstUtil.WREADER_TOOL_WINDOW_TOOL_BAR_ID, toolWindowBarGroup, true);

        actionToolbar.setTargetComponent(menuToolBarPanel);
        JComponent actionToolbarComponent = actionToolbar.getComponent();
        actionToolbarComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionToolbarComponent.setAlignmentY(Component.TOP_ALIGNMENT);

        BorderLayout layout = new BorderLayout();
        menuToolBarPanel.setLayout(layout);
        menuToolBarPanel.add(actionToolbarComponent, BorderLayout.NORTH);
        menuToolBarPanel.setAlignmentY(Component.TOP_ALIGNMENT);
    }

    /**
     * 初始化数据
     */
    public void initData(ToolWindow toolWindow) {
        try {
            operateAction = OperateActionUtil.getInstance(toolWindow.getProject());
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
            if (fontFamily == null || fontFamily.isEmpty()) {
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
                colorShowPanel.setBackground(defaultForeground);
            }

            // 站点列表信息
            siteList = configYaml.getEnableSiteList();

            // 加载持久化数据--站点信息
            Integer selectedBookSiteIndexTemp = cacheService.getSelectedBookSiteIndex();
            if (selectedBookSiteIndexTemp == null) {
                selectedBookSiteIndex = 0;
                cacheService.setSelectedBookSiteIndex(0);
            } else {
                selectedBookSiteIndex = selectedBookSiteIndexTemp;
            }
            selectedBookSiteInfo = siteList.get(selectedBookSiteIndex);
            if (selectedBookSiteInfo == null) {
                // 选中的站点信息
                selectedBookSiteInfo = siteList.get(selectedBookSiteIndex);
                cacheService.setSelectedBookSiteInfo(selectedBookSiteInfo);
            }
            // 加载持久化数据--小说信息
            selectBookInfo = cacheService.getSelectedBookInfo();
            // 加载持久化数据--章节信息
            currentChapterInfo = cacheService.getSelectedChapterInfo();
            if (currentChapterInfo == null) {
                currentChapterInfo = new ChapterInfo();
            }
            // 加载持久化数据--目录名称列表
            chapterList = cacheService.getChapterList();
            // 加载持久化数据--目录章节链接列表
            chapterUrlList = cacheService.getChapterUrlList();

            // 选择的站点基础网址
            baseUrl = selectedBookSiteInfo.getBaseUrl();

            // 获取当前章节信息
            currentChapterIndex = currentChapterInfo.getSelectedChapterIndex();
            chapterContentHtml = currentChapterInfo.getChapterContent();
            chapterContentText = currentChapterInfo.getChapterContentStr();
            if (chapterContentHtml == null || chapterContentHtml.isEmpty()) {
                chapterContentHtml = "<pre>" + ConstUtil.WREADER_TOOL_WINDOW_CONTENT_INIT_TEXT + "</pre>";
            } else {
                operateAction.updateContentText();
            }

        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_INIT_ERROR, "Error");
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化内容编辑器JTextArea
     */
    public void initContentTextArea(ToolWindow toolWindow) {
        JComponent toolWindowComponent = toolWindow.getComponent();

        contentPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        contentTextPane = new JTextPane();
        contentTextPane.setContentType("text/html");
        contentTextPane.setEditable(false);
        contentTextPane.setAlignmentY(Component.TOP_ALIGNMENT);
        contentTextPane.setPreferredSize(new Dimension(toolWindowComponent.getWidth(), toolWindowComponent.getHeight()));
        contentTextPane.addMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             *
             * @param e
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    // 双击事件，显示/隐藏工具菜单
//                    menuToolPanel.setVisible(!menuToolPanel.isVisible());
                }
            }
        });
        contentScrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
        contentScrollPane.setViewportView(contentTextPane);
        contentScrollPane.setBorder(JBUI.Borders.empty(2, 5));
    }

    private void setContentText(String content) {
        contentTextPane.setText(content);
        // 滚动到顶部
        contentTextPane.setCaretPosition(0);
    }

    /**
     * 主题切换监听器
     */
    public void appEditorColorsListener() {
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        bus.connect().subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) editorColorsScheme -> {
            if (editorColorsScheme == null) {
                return;
            }

            // 获取前景色
            Color defaultForeground = editorColorsScheme.getDefaultForeground();
            fontColorHex = String.format("#%02x%02x%02x", defaultForeground.getRed(), defaultForeground.getGreen(),
                    defaultForeground.getBlue());
            cacheService.setFontColorHex(fontColorHex);
            try {
                colorShowPanel.setBackground(defaultForeground);
                String style = "color:" + fontColorHex + ";" +
                        "font-family: '" + fontFamily + "';" +
                        "font-size: " + fontSize + "px;";
                chapterContentHtml = String.format("<div style='%s'>%s</div>", style, currentChapterInfo.getChapterContent());
                setContentText(chapterContentHtml);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    public JPanel getContent() {
        return readerPanel;
    }

}
