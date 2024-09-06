package com.wei.wreader.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.utils.ConstUtil;
import groovy.util.logging.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

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

    private static final String baseUrl = "https://www.3bqg.cc";

    public WReaderToolWindow(ToolWindow toolWindow) {
        // 添加监听器
        menuListButton.addActionListener(e -> menuLisListener(e, toolWindow));
        prevPageButton.addActionListener(e -> prevPageListener(e, toolWindow));
        nextPageButton.addActionListener(e -> nextPageListener(e, toolWindow));
        searchBookButton.addActionListener(e -> searchBookListener(e, toolWindow));
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

            int prevChapterIndex = currentChapterIndex - 1;
            String prevChapterSuffixUrl = chapterUrlList.get(prevChapterIndex);
            String prevChapterUrl = baseUrl + prevChapterSuffixUrl;
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

            int nextChapterIndex = currentChapterIndex + 1;
            String nextChapterSuffixUrl = chapterUrlList.get(nextChapterIndex);
            String nextChapterUrl = baseUrl + nextChapterSuffixUrl;
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
        int fontSize = font.getSize();
        if (fontSize <= 1) {
            return;
        }
        contentEditorPane1.setFont(new Font(font.getFamily(), font.getStyle(), fontSize - 1));
        contentEditorPane1.updateUI();
    }

    /**
     * 字体大小加按钮监听器
     * @param event
     * @param toolWindow
     */
    private void fontAddButtonListener(ActionEvent event, ToolWindow toolWindow) {
        Font font = contentEditorPane1.getFont();
        int fontSize = font.getSize();
        contentEditorPane1.setFont(new Font(font.getFamily(), font.getStyle(), fontSize + 1));
        contentEditorPane1.updateUI();
    }

    /**
     * 颜色输入框监听器
     * @param evt
     * @param toolWindow
     */
    private void colorTextField1Listener(java.beans.PropertyChangeEvent evt, ToolWindow toolWindow) {
        String color = colorTextField1.getText();
        System.out.println(color);
        contentEditorPane1.setForeground(Color.decode(color));
        contentEditorPane1.updateUI();
    }

    /**
     * 搜索按钮监听器
     *
     * @param event
     * @param toolWindow
     */
    private void searchBookListener(ActionEvent event, ToolWindow toolWindow) {
        // 创建一个弹出窗
        JTextField searchBookTextField = new JTextField(20);
        Object[] objs = {ConstUtil.WREADER_SEARCH_BOOK_TITLE, searchBookTextField};
        int result = JOptionPane.showConfirmDialog(null, objs, ConstUtil.WREADER_SEARCH_BOOK_TIP_TEXT, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            SwingUtilities.invokeLater(() -> {
                String bookName = searchBookTextField.getText();
                try {
                    String searchBookUrl = baseUrl + "/user/search.html?q=" + bookName;

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
                    String articleName = asJsonObject.get("articlename").getAsString();
                    String author = asJsonObject.get("author").getAsString();
                    String intro = asJsonObject.get("intro").getAsString();
                    String urlImg = asJsonObject.get("url_img").getAsString();
                    String urlList = asJsonObject.get("url_list").getAsString();

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
                        String bookUrl = baseUrl + bookInfo.getBookUrl();
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
    public String searchBookList(String url) {
        String result = null;

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
        Element bookListElement = document.body();

        // 获取目录列表
        Elements listMainElement = bookListElement.getElementsByClass("listmain");
        if (!listMainElement.isEmpty()) {
            listMainElement.get(0).getElementsByTag("a").forEach(element -> {
                String href = element.attr("href");
                String text = element.text();
                chapterList.add(text);
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
                    String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
                    String chapterUrl = baseUrl + chapterSuffixUrl;
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
                String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
                String chapterUrl = baseUrl + chapterSuffixUrl;
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
        Element chapterContentElement = bodyElement.getElementById("chaptercontent");
        if (chapterContentElement == null) {
            Messages.showMessageDialog(ConstUtil.WREADER_SEARCH_BOOK_CONTENT_ERROR, "提示", Messages.getInformationIcon());
            return;
        }

        String chapterContent = chapterContentElement.html();
        contentEditorPane1.setText(chapterContent);
        contentEditorPane1.setCaretPosition(0);
    }

    public JPanel getContent() {
        return readerPanel;
    }

}
