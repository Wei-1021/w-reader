package com.wei.wreader.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.BookSiteInfo;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.JsUtil;
import groovy.util.logging.Log4j2;
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


/**
 * 工具窗口视图
 *
 * @author weizhanjie
 */
@Log4j2
public class WReaderToolWindow {
    /**
     * 阅读器面板
     */
    private JPanel readerPanel;
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
     * 内容面板
     */
    private JPanel contentPanel;
    /**
     * 内容编辑器
     */
    private final JEditorPane contentEditorPane1;
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
     * 颜色输入框
     */
    private JTextField colorTextField1;
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
     * 当前章节索引
     */
    private int currentChapterIndex = 0;
    /**
     * font size
     */
    private int fontSize = 0;
    /**
     * 当前章节内容
     */
    private String chapterContentText = "";

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
     * 当前章节信息
     */
    private final ChapterInfo currentChapterInfo = new ChapterInfo();

    public WReaderToolWindow(ToolWindow toolWindow) {
        ConfigYaml configYaml = new ConfigYaml();
        siteList = configYaml.getSiteList();
        selectedBookSiteInfo = siteList.get(0);
        baseUrl = selectedBookSiteInfo.getBaseUrl();



        // 添加监听器
        searchBookButton.addActionListener(e -> searchBookListener(e, toolWindow));
        menuListButton.addActionListener(e -> menuLisListener(e, toolWindow));
        prevPageButton.addActionListener(e -> prevPageListener(e, toolWindow));
        nextPageButton.addActionListener(e -> nextPageListener(e, toolWindow));
        fontSubButton.addActionListener(e -> fontSubButtonListener(e, toolWindow));
        fontAddButton1.addActionListener(e -> fontAddButtonListener(e, toolWindow));
        colorTextField1.addVetoableChangeListener(evt -> colorTextField1Listener(evt, toolWindow));

        Font font = new Font(ConstUtil.DEFAULT_FONT_FAMILY, Font.PLAIN, ConstUtil.DEFAULT_FONT_SIZE);

        contentEditorPane1 = new JEditorPane();
        contentEditorPane1.setText(ConstUtil.WREADER_TOOL_WINDOW_CONTENT_INIT_TEXT);
        contentEditorPane1.setFont(font);
        contentEditorPane1.setContentType("text/html");
        contentEditorPane1.setEditable(false);
        contentEditorPane1.setBackground(JBColor.WHITE);

        contentScrollPane.setViewportView(contentEditorPane1);
    }

    /**
     * 目录列表按钮监听器
     *
     * @param event
     * @param toolWindow
     */
    private void menuLisListener(ActionEvent event, ToolWindow toolWindow) {
        showBookDirectory();
    }

    /**
     * 上一页按钮监听器
     *
     * @param event
     * @param toolWindow
     */
    private void prevPageListener(ActionEvent event, ToolWindow toolWindow) {
        try {
            if (currentChapterIndex <= 0) {
                return;
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
            currentChapterInfo.setChapterContent(chapterContentText);
            searchBookContent(prevChapterUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下一页按钮监听器
     *
     * @param event
     * @param toolWindow
     */
    private void nextPageListener(ActionEvent event, ToolWindow toolWindow) {
        try {
            if (currentChapterIndex >= chapterUrlList.size() - 1) {
                return;
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
            currentChapterInfo.setChapterContent(chapterContentText);
            searchBookContent(nextChapterUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字体大小减按钮监听器
     * @param event
     * @param toolWindow
     */
    private void fontSubButtonListener(ActionEvent event, ToolWindow toolWindow) {
        Font font = contentEditorPane1.getFont();
        if (fontSize == 0) {
            fontSize = font.getSize();
        }
        if (fontSize <= 1) {
            return;
        }

        fontSize = fontSize - 1;


        String style = "font-family: '" + font.getFamily() + "'; font-size: " + fontSize + "px;";
        String text = "<div style=\"" + style + "\">" + chapterContentText + "</div>";
        contentEditorPane1.setText(text);
        contentEditorPane1.updateUI();
    }

    /**
     * 字体大小加按钮监听器
     * @param event
     * @param toolWindow
     */
    private void fontAddButtonListener(ActionEvent event, ToolWindow toolWindow) {
        Font font = contentEditorPane1.getFont();
        if (fontSize == 0) {
            fontSize = font.getSize();
        }
        fontSize = fontSize + 1;
        String style = "font-family: '" + font.getFamily() + "'; font-size: " + fontSize + "px;";
        String text = "<div style=\"" + style + "\">" + chapterContentText + "</div>";
        contentEditorPane1.setText(text);
        contentEditorPane1.updateUI();
    }

    /**
     * 颜色输入框监听器
     * @param evt
     * @param toolWindow
     */
    private void colorTextField1Listener(java.beans.PropertyChangeEvent evt, ToolWindow toolWindow) {
        String color = colorTextField1.getText();
        Font font = contentEditorPane1.getFont();
        System.out.println(color);

        String style =
                "color:' " + color + "';" +
                "font-family: '" + font.getFamily() + "';" +
                "font-size: " + fontSize + "px;";
        String text = "<div style=\"" + style + "\">" + chapterContentText + "</div>";
        contentEditorPane1.setText(text);
        contentEditorPane1.updateUI();
    }

    /**
     * 搜索按钮监听器
     *
     * @param event
     * @param toolWindow
     */
    private void searchBookListener(ActionEvent event, ToolWindow toolWindow) {
        // 创建一个弹出窗, 包含一个选择下拉框和一个输入框
        ComboBox<String> comboBox = getStringComboBox();

        JTextField searchBookTextField = new JTextField(20);
        Object[] objs = {ConstUtil.WREADER_SEARCH_BOOK_TITLE, comboBox, searchBookTextField};
        int result = JOptionPane.showConfirmDialog(null, objs, ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            SwingUtilities.invokeLater(() -> {
                String bookName = searchBookTextField.getText();
                try {
                    String searchBookUrl = baseUrl + selectedBookSiteInfo.getSearchUrl() +
                            "?" + selectedBookSiteInfo.getSearchBookNameParam() + "=" + bookName;

                    System.out.println("Searching: " + searchBookUrl);

                    // 获取搜索结果
                    String searchBookResult = searchBookList(searchBookUrl);

                    if (searchBookResult == null || ConstUtil.STR_ONE.equals(searchBookResult)) {
                        Messages.showMessageDialog(ConstUtil.WREADER_SEARCH_BOOK_ERROR, "提示", Messages.getInformationIcon());
                        return;
                    }

                    handleBookList(searchBookResult);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private @NotNull ComboBox<String> getStringComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        for (BookSiteInfo bookName : siteList) {
            comboBox.addItem(bookName.getName() + "(" + bookName.getId() + ")");
        }

        comboBox.addActionListener(e -> {
            int selectedIndex = comboBox.getSelectedIndex();
            selectedBookSiteInfo = siteList.get(selectedIndex);
            baseUrl = selectedBookSiteInfo.getBaseUrl();
            System.out.println(selectedIndex + " " + selectedBookSiteInfo);
        });
        return comboBox;
    }

    /**
     * 处理搜索目录结果
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
                // 创建窗口
                JFrame frame = new JFrame("搜索结果列表");
                frame.setSize(350, 500);
                // 获取屏幕尺寸
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int x = (screenSize.width - frame.getWidth()) / 2;
                int y = (screenSize.height - frame.getHeight()) / 2;
                // 将窗口居中
                frame.setLocation(x, y);

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
                        BookInfo bookInfo = bookInfoList.get(selectedIndex);
                        String bookUrl = bookInfo.getBookUrl();
                        if (!bookInfo.getBookUrl().startsWith("http://") && !bookInfo.getBookUrl().startsWith("https://")) {
                            bookUrl = baseUrl + bookInfo.getBookUrl();
                        }

                        try {
                            // 搜索小说目录
                            chapterList = new ArrayList<>();
                            chapterUrlList = new ArrayList<>();
                            searchBookDirectory(bookUrl);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });

                // 使用滚动面板来添加滚动条
                frame.add(new JScrollPane(searchBookList));
                frame.setVisible(true);
            }
        }
    }

    /**
     * 搜索小说列表
     *
     * @param url
     * @return
     */
    public String searchBookList(String url) throws IOException {
        String result = null;
        // 获取小说列表的接口返回的是否是html
        if (selectedBookSiteInfo.isHtml()) {
            // 获取html
            Document document = Jsoup.connect(url)
                    .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                    .get();
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
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    /**
     * 获取小说目录
     * @param url
     * @throws IOException
     */
    public void searchBookDirectory(String url) throws IOException {
        Document document = Jsoup.connect(url)
                .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                .get();
        // 获取目录列表
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
            listMainElement.getElementsByTag("a").forEach(element -> {
                String href = element.attr("href");
                String text = element.text();
                chapterList.add(text);
                try {
                    href = JsUtil.buildFullURL(location, href);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                chapterUrlList.add(href);
            });

            JFrame frame = new JFrame("小说目录");
            frame.setSize(350, 500);

            // 获取屏幕尺寸
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (screenSize.width) / 2 - frame.getWidth() / 2;
            int y = (screenSize.height) / 2 - frame.getHeight() / 2;
            frame.setLocation(x + 50, y);

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
                    currentChapterInfo.setChapterContent(chapterContentText);
                    try {
                        searchBookContent(chapterUrl);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            frame.add(new JScrollPane(chapterListJBList));
            frame.setVisible(true);
        }
    }

    /**
     * 显示当前小说目录
     */
    public void showBookDirectory() {
        JFrame frame = new JFrame("小说目录");
        frame.setSize(350, 500);

        // 获取屏幕尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width) / 2 - frame.getWidth() / 2;
        int y = (screenSize.height) / 2 - frame.getHeight() / 2;
        frame.setLocation(x + 50, y);

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
                currentChapterInfo.setChapterContent(chapterContentText);
                try {
                    searchBookContent(chapterUrl);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        frame.add(new JScrollPane(chapterListJBList));
        frame.setVisible(true);
    }

    /**
     * 获取小说内容
     * @param url
     * @throws IOException
     */
    public void searchBookContent(String url) throws IOException {
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
            return;
        }

        String chapterContent = chapterContentElement.html();
        chapterContent = "<h4 style=\"text-align: center;margin-bottom: 20px;color: '" + ConstUtil.DEFAULT_FONT_COLOR_HEX +"'\">" +
                currentChapterInfo.getChapterTitle() + "</h4>" +
                chapterContent;
        chapterContentText = chapterContent;
        contentEditorPane1.setText(chapterContent);
        contentEditorPane1.setCaretPosition(0);
    }

    public JPanel getContent() {
        return readerPanel;
    }

}
