package com.wei.wreader.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.BookSiteInfo;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.service.CacheService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class OperateActionUtil {
    private ConfigYaml configYaml;
    private CacheService cacheService;
    private static OperateActionUtil instance;
    /**
     * 书本名称列表
     */
    private final java.util.List<String> bookNameList = new ArrayList<>();
    /**
     * 书本信息列表
     */
    private final java.util.List<BookInfo> bookInfoList = new ArrayList<>();
    /**
     * 章节名称列表
     */
    private java.util.List<String> chapterList = new ArrayList<>();
    /**
     * 章节链接列表
     */
    private java.util.List<String> chapterUrlList = new ArrayList<>();
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
     * 当前小说信息
     */
    private BookInfo selectBookInfo = new BookInfo();
    /**
     * 当前章节信息
     */
    private ChapterInfo currentChapterInfo = new ChapterInfo();


    public static OperateActionUtil getInstance() {
        if (instance == null) {
            instance = new OperateActionUtil();
        }
        return instance;
    }

    public OperateActionUtil() {
        configYaml = new ConfigYaml();
        cacheService = CacheService.getInstance();
        initData();
    }

    private void initData() {
        try {
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
            }

            // 站点列表信息
            siteList = configYaml.getSiteList();

            // 加载持久化数据--站点信息
            selectedBookSiteInfo = cacheService.getSelectedBookSiteInfo();
            if (selectedBookSiteInfo == null) {
                // 选中的站点信息
                selectedBookSiteInfo = siteList.get(0);
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
            if (chapterList != null && !chapterList.isEmpty() &&
                    chapterUrlList != null && !chapterUrlList.isEmpty()) {
                if (currentChapterInfo.getChapterUrl() != null) {
                    currentChapterIndex = currentChapterInfo.getSelectedChapterIndex();
                    searchBookContent(currentChapterInfo.getChapterUrl());
                }
            }

            if (chapterContentHtml == null || chapterContentHtml.isEmpty()) {
                chapterContentHtml = "<pre>" + ConstUtil.WREADER_TOOL_WINDOW_CONTENT_INIT_TEXT + "</pre>";
            }
        } catch (Exception e) {
            Messages.showErrorDialog(ConstUtil.WREADER_INIT_ERROR, "Error");
            throw new RuntimeException(e);
        }
    }


    /**
     * 构建搜索弹出窗口
     */
    public void buildSearchBookDialog() {
        SwingUtilities.invokeLater(() -> {
            // 创建一个弹出窗, 包含一个选择下拉框和一个输入框
            ComboBox<String> comboBox = getStringComboBox();
            JTextField searchBookTextField = new JTextField(20);
            Object[] objs = {ConstUtil.WREADER_SEARCH_BOOK_TITLE, comboBox, searchBookTextField};
            int result = JOptionPane.showConfirmDialog(null, objs,
                    ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT, JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                SwingUtilities.invokeLater(() -> {
                    String bookName = searchBookTextField.getText();
                    if (bookName == null || bookName.trim().isEmpty()) {
                        Messages.showMessageDialog(ConstUtil.WREADER_SEARCH_EMPTY, "提示", Messages.getInformationIcon());
                        return;
                    }

                    String searchBookUrl = baseUrl + selectedBookSiteInfo.getSearchUrl() +
                            "?" + selectedBookSiteInfo.getSearchBookNameParam() + "=" + bookName;

                    // 获取搜索结果
                    String searchBookResult = searchBookList(searchBookUrl);
                    if (searchBookResult == null || ConstUtil.STR_ONE.equals(searchBookResult)) {
                        Messages.showMessageDialog(ConstUtil.WREADER_SEARCH_BOOK_ERROR, "提示", Messages.getInformationIcon());
                        return;
                    }
                    handleBookList(searchBookResult);
                });
            }
        });
    }

    private @NotNull ComboBox<String> getStringComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        for (BookSiteInfo bookName : siteList) {
            comboBox.addItem(bookName.getName() + "(" + bookName.getId() + ")");
        }

        comboBox.addActionListener(e -> {
            int selectedIndex = comboBox.getSelectedIndex();
            selectedBookSiteInfo = siteList.get(selectedIndex);
            cacheService.setSelectedBookSiteInfo(selectedBookSiteInfo);
            baseUrl = selectedBookSiteInfo.getBaseUrl();
        });
        return comboBox;
    }


    /**
     * 搜索小说列表
     *
     * @param url
     * @return
     */
    public String searchBookList(String url) {
        String result = null;
        // 获取小说列表的接口返回的是否是html
        if (selectedBookSiteInfo.isHtml()) {
            // 获取html
            Document document = null;
            try {
                document = Jsoup.connect(url)
                        .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                        .get();
            } catch (IOException e) {
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                throw new RuntimeException(e);
            }
            // 小说列表的HTML标签类型（class, id）
            String bookListElementType = selectedBookSiteInfo.getBookListElementType();
            Element element = null;
            if (ConstUtil.ELEMENT_CLASS_STR.equals(bookListElementType)) {
                Elements elements = document.getElementsByClass(selectedBookSiteInfo.getBookListElementName());
                element = elements.first();
            } else if (ConstUtil.ELEMENT_ID_STR.equals(bookListElementType)) {
                element = document.getElementById(selectedBookSiteInfo.getBookListElementName());
            }

            if (element != null) {
                JsonArray jsonArray = new JsonArray();
                String location = document.location();
                element.getElementsByClass(selectedBookSiteInfo.getBookNameField()).forEach(itemElement -> {
                    Element aElement = itemElement.getElementsByTag("a").first();
                    if (aElement != null) {
                        String bookUrl = aElement.attr("href");
                        String bookName = aElement.text();

                        try {
                            bookUrl = JsUtil.buildFullURL(location, bookUrl);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }

                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty(selectedBookSiteInfo.getBookNameField(), bookName);
                        jsonObject.addProperty(selectedBookSiteInfo.getBookUrlField(), bookUrl);
                        jsonObject.addProperty(selectedBookSiteInfo.getBookAuthorField(), "");
                        jsonObject.addProperty(selectedBookSiteInfo.getBookDescField(), "");
                        jsonObject.addProperty(selectedBookSiteInfo.getBookImgUrlField(), "");
                        jsonArray.add(jsonObject);
                    }
                });
                return jsonArray.toString();
            }
        } else {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
            try (CloseableHttpResponse httpResponse = HttpClients.createDefault().execute(httpGet)) {
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = httpResponse.getEntity();
                    result = EntityUtils.toString(entity);
                }

            } catch (IOException e) {
                Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    /**
     * 处理搜索目录结果
     *
     * @param result
     */
    public void handleBookList(String result) {
        if (!result.isEmpty()) {
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
                    String articleName = asJsonObject.get(selectedBookSiteInfo.getBookNameField()).getAsString();
                    String author = asJsonObject.get(selectedBookSiteInfo.getBookAuthorField()).getAsString();
                    String intro = asJsonObject.get(selectedBookSiteInfo.getBookDescField()).getAsString();
                    String urlImg = asJsonObject.get(selectedBookSiteInfo.getBookImgUrlField()).getAsString();
                    String urlList = asJsonObject.get(selectedBookSiteInfo.getBookUrlField()).getAsString();

                    // 设置bookInfo
                    BookInfo bookInfo = new BookInfo();
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
                        cacheService.setSelectedBookInfo(selectBookInfo);
                        String bookUrl = selectBookInfo.getBookUrl();
                        if (!selectBookInfo.getBookUrl().startsWith("http://") && !selectBookInfo.getBookUrl().startsWith("https://")) {
                            bookUrl = baseUrl + selectBookInfo.getBookUrl();
                        }

                        // 搜索小说目录
                        chapterList = new ArrayList<>();
                        chapterUrlList = new ArrayList<>();
                        searchBookDirectory(bookUrl);
                    }
                });

                // 使用滚动面板来添加滚动条
                JBScrollPane jScrollPane = new JBScrollPane(searchBookList);

                JFrame frame = new JFrame();
                frame.setSize(350, 500);
                frame.setVisible(true);
                frame.add(jScrollPane);
                frame.setTitle("搜索结果");
                // 居中显示
                frame.setLocationRelativeTo(null);
            }
        }
    }

    /**
     * 获取小说目录
     *
     * @param url
     * @throws IOException
     */
    public void searchBookDirectory(String url) {
        Document document = null;
        try {
            document = Jsoup.connect(url)
                    .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                    .get();
        } catch (IOException e) {
            Messages.showWarningDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
            throw new RuntimeException(e);
        }

        // 获取目录列表元素
        Element listMainElement = null;
        if (selectedBookSiteInfo.isHtml()) {
            String listMainElementType = selectedBookSiteInfo.getListMainElementType();
            if (ConstUtil.ELEMENT_CLASS_STR.equals(listMainElementType)) {
                listMainElement = document.getElementsByClass(selectedBookSiteInfo.getListMainElementName()).first();
            } else if (ConstUtil.ELEMENT_ID_STR.equals(listMainElementType)) {
                listMainElement = document.getElementById(selectedBookSiteInfo.getListMainElementName());
            }
        }

        if (listMainElement != null) {
            // 获取页面的地址
            String location = document.location();
            // 获取目录列表元素下所有的a标签元素
            listMainElement.getElementsByTag("a").forEach(element -> {
                // 提取链接和章节名称
                String href = element.attr("href");
                String text = element.text();
                chapterList.add(text);
                try {
                    // 转化url路径，将相对路径转化成绝对路径
                    href = JsUtil.buildFullURL(location, href);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                chapterUrlList.add(href);
            });
            cacheService.setChapterList(chapterList);
            cacheService.setChapterUrlList(chapterUrlList);

            // 构建目录列表组件
            JBList<String> chapterListJBList = new JBList<>(chapterList);
            // 设置单选模式
            chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            chapterListJBList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedIndex = chapterListJBList.getSelectedIndex();
                    currentChapterIndex = selectedIndex;
                    String chapterTitle = chapterList.get(currentChapterIndex);
                    String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
                    String chapterUrl = chapterSuffixUrl;
                    if (!chapterSuffixUrl.startsWith("http://") && !chapterSuffixUrl.startsWith("https://")) {
                        chapterUrl = baseUrl + chapterSuffixUrl;
                    }
                    currentChapterInfo.setChapterTitle(chapterTitle);
                    currentChapterInfo.setChapterUrl(chapterUrl);
                    try {
                        searchBookContent(chapterUrl);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    currentChapterInfo.setChapterContent(chapterContentHtml);
                    currentChapterInfo.setChapterContentStr(chapterContentText);
                    currentChapterInfo.setSelectedChapterIndex(currentChapterIndex);
                    // 缓存当前章节信息
                    cacheService.setSelectedChapterInfo(currentChapterInfo);
                }
            });

            JBScrollPane jScrollPane = new JBScrollPane(chapterListJBList);

            JFrame frame = new JFrame();
            frame.setSize(350, 500);
            frame.setVisible(true);
            frame.add(jScrollPane);
            frame.setTitle("目录");
            // 居中显示
            frame.setLocationRelativeTo(null);
        }
    }

    /**
     * 显示当前小说目录
     */
    public void showBookDirectory(BookDirectoryListener listener) {
        SwingUtilities.invokeLater(() -> {
            JBList<String> chapterListJBList = new JBList<>(chapterList);
            chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            chapterListJBList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedIndex = chapterListJBList.getSelectedIndex();
                    currentChapterIndex = selectedIndex;
                    String chapterTitle = chapterList.get(currentChapterIndex);
                    String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
                    String chapterUrl = chapterSuffixUrl;
                    if (!chapterSuffixUrl.startsWith("http://") && !chapterSuffixUrl.startsWith("https://")) {
                        chapterUrl = baseUrl + chapterSuffixUrl;
                    }
                    currentChapterInfo.setChapterTitle(chapterTitle);
                    currentChapterInfo.setChapterUrl(chapterUrl);
                    try {
                        Element chapterElement = searchBookContent(chapterUrl);
                        currentChapterInfo.setSelectedChapterIndex(currentChapterIndex);
                        currentChapterInfo.setChapterContent(chapterContentHtml);
                        currentChapterInfo.setChapterContentStr(chapterContentText);
                        cacheService.setSelectedChapterInfo(currentChapterInfo);
                        if (listener != null) {
                            listener.onClickItem(selectedIndex, chapterList, chapterElement);
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            JBScrollPane jScrollPane = new JBScrollPane(chapterListJBList);
            JFrame frame = new JFrame();
            frame.setSize(350, 500);
            frame.setVisible(true);
            frame.add(jScrollPane);
            frame.setTitle("目录");
            // 居中显示
            frame.setLocationRelativeTo(null);

            chapterListJBList.setSelectedIndex(currentChapterIndex);
            chapterListJBList.ensureIndexIsVisible(currentChapterIndex);
        });
    }

    /**
     * 获取小说内容
     *
     * @param url
     * @return Element 小说内容Element
     * @throws IOException
     */
    public Element searchBookContent(String url) throws IOException {
        Document document = Jsoup.connect(url)
                .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                .get();
        Element bodyElement = document.body();

        // 获取小说内容
        Element chapterContentElement = null;
        if (selectedBookSiteInfo.isHtml()) {
            // 小说内容的HTML标签类型（class, id）
            String chapterContentElementType = selectedBookSiteInfo.getChapterContentElementType();
            if (chapterContentElementType.equals(ConstUtil.ELEMENT_CLASS_STR)) {
                chapterContentElement = bodyElement.getElementsByClass(selectedBookSiteInfo.getChapterContentElementName()).first();
            } else if (chapterContentElementType.equals(ConstUtil.ELEMENT_ID_STR)) {
                chapterContentElement = bodyElement.getElementById(selectedBookSiteInfo.getChapterContentElementName());
            }
        }
        if (chapterContentElement == null) {
            Messages.showMessageDialog(ConstUtil.WREADER_SEARCH_BOOK_CONTENT_ERROR, "提示", Messages.getInformationIcon());
            return null;
        }

        String chapterContent = chapterContentElement.html();
        chapterContentText = chapterContentElement.text();
        chapterContent = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" + fontColorHex + ";\">" +
                currentChapterInfo.getChapterTitle() + "</h3>" +
                chapterContent;

        chapterContentHtml = chapterContent;

        String style = "color:" + fontColorHex + ";" +
                "font-family: '" + fontFamily + "';" +
                "font-size: " + fontSize + "px;";
        String text = "<div style=\"" + style + "\">" + chapterContentHtml + "</div>";

        chapterContentElement.html(text);

        return chapterContentElement;
    }

    /**
     * 上一页
     */
    public Element prevPageChapter() {
        try {
            if (currentChapterIndex <= 0) {
                return null;
            }

            currentChapterIndex = currentChapterIndex - 1;
            String chapterTitle = chapterList.get(currentChapterIndex);
            String prevChapterSuffixUrl = chapterUrlList.get(currentChapterIndex);
            String prevChapterUrl = prevChapterSuffixUrl;
            if (!prevChapterSuffixUrl.startsWith("http://") && !prevChapterSuffixUrl.startsWith("https://")) {
                prevChapterUrl = baseUrl + prevChapterSuffixUrl;
            }
            currentChapterInfo.setChapterTitle(chapterTitle);
            currentChapterInfo.setChapterUrl(prevChapterUrl);
            Element element = searchBookContent(prevChapterUrl);
            currentChapterInfo.setChapterContent(chapterContentHtml);
            currentChapterInfo.setChapterContentStr(chapterContentText);
            currentChapterInfo.setSelectedChapterIndex(currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);
            return element;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下一页
     */
    public Element nextPageChapter() {
        try {
            if (currentChapterIndex >= chapterUrlList.size() - 1) {
                return null;
            }

            currentChapterIndex = currentChapterIndex + 1;
            String chapterTitle = chapterList.get(currentChapterIndex);
            String nextChapterSuffixUrl = chapterUrlList.get(currentChapterIndex);
            String nextChapterUrl = nextChapterSuffixUrl;
            if (!nextChapterSuffixUrl.startsWith("http://") && !nextChapterSuffixUrl.startsWith("https://")) {
                nextChapterUrl = baseUrl + nextChapterSuffixUrl;
            }
            currentChapterInfo.setChapterTitle(chapterTitle);
            currentChapterInfo.setChapterUrl(nextChapterUrl);
            Element element = searchBookContent(nextChapterUrl);
            currentChapterInfo.setChapterContent(chapterContentHtml);
            currentChapterInfo.setChapterContentStr(chapterContentText);
            currentChapterInfo.setSelectedChapterIndex(currentChapterIndex);
            cacheService.setSelectedChapterInfo(currentChapterInfo);

            return element;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * ToolWindow工具类
     */
    public static class ToolWindow {

        /**
         * 获取ToolWindow的根内容面板
         * @param contentManager
         * @return
         */
        public static Component getReaderPanel(ContentManager contentManager) {
            Content rootContent = contentManager.getContent(0);
            if (rootContent != null) {
                return rootContent.getComponent();
            }
            return null;
        }

        /**
         * 获取ToolWindow的根内容面板
         * @param rootContent
         * @return
         */
        public static JPanel getReaderPanel(Content rootContent) {
            return (JPanel) rootContent.getComponent();
        }

        /**
         * 获取ToolWindow的内容面板
         * @param rootContent
         * @return
         */
        public static JPanel getContentPanel(Content rootContent) {
            JPanel readerPanel = getReaderPanel(rootContent);
            return (JPanel) readerPanel.getComponent(0);
        }

        /**
         * 获取ToolWindow的内容滚动面板
         * @param rootContent
         * @return
         */
        public static JScrollPane getContentScrollPane(Content rootContent) {
            JPanel contentPanel = getContentPanel(rootContent);
            if (contentPanel.getComponentCount() > 0) {
                Component contentScrollComponent = contentPanel.getComponent(0);
                if (contentScrollComponent instanceof JScrollPane contentScrollPane) {
                    return contentScrollPane;
                }
            }
            return null;
        }

        /**
         * 获取ToolWindow的内容文本面板
         * @param rootContent
         * @return
         */
        public static JTextPane getContentTextPanel(Content rootContent) {
            JScrollPane contentScrollPane = getContentScrollPane(rootContent);
            if (contentScrollPane != null) {
                return (JTextPane) contentScrollPane.getViewport().getView();
            }
            return null;
        }
    }


}
