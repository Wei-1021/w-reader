package com.wei.wreader.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.impl.customFrameDecorations.style.StyleProperty;
import com.intellij.ui.components.JBList;
import com.wei.wreader.utils.ConstUtil;
import groovy.util.logging.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.yaml.snakeyaml.util.UriEncoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


/**
 * 工具窗口视图
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
    private JTextArea contentTextArea1;

    public WReaderToolWindow(ToolWindow toolWindow) {
        // 添加监听器
        menuListButton.addActionListener(e -> menuLisListener(e, toolWindow));
        prevPageButton.addActionListener(e -> prevPageListener(e, toolWindow));
        nextPageButton.addActionListener(e -> nextPageListener(e, toolWindow));
        searchBookButton.addActionListener(e -> searchBookListener(e, toolWindow));

        contentTextArea1.append(ConstUtil.WREADER_TOOL_WINDOW_CONTENT_INIT_TEXT);
    }

    /**
     * 目录列表按钮监听器
     * @param event
     * @param toolWindow
     */
    private void menuLisListener(ActionEvent event, ToolWindow toolWindow) {

    }

    /**
     * 上一页按钮监听器
     * @param event
     * @param toolWindow
     */
    private void prevPageListener(ActionEvent event, ToolWindow toolWindow) {

    }

    /**
     * 下一页按钮监听器
     * @param event
     * @param toolWindow
     */
    private void nextPageListener(ActionEvent event, ToolWindow toolWindow) {

    }

    /**
     * 搜索按钮监听器
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
                    String searchBookUrl = "https://www.3bqg.cc/user/search.html?q=" + bookName;
                    Document document = Jsoup.connect(searchBookUrl)
                            .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                            .get();

                    System.out.println(document.toString());
//                    if (response != null && response.statusCode() == 200) {
//                        String body = response.body();
//
//                        System.out.println(body);
//
//                        Gson gson = new Gson();
//                        JsonArray jsonArray = gson.fromJson(body, JsonArray.class);
//
//                        if (jsonArray != null && !jsonArray.isEmpty()) {
//                            JFrame frame = new JFrame("搜索结果列表");
//                            frame.setSize(400, 500);
//                            // 获取屏幕尺寸
//                            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//                            int x = (screenSize.width - frame.getWidth()) / 2;
//                            int y = (screenSize.height - frame.getHeight()) / 2;
//                            // 将窗口居中
//                            frame.setLocation(x, y);
//
//                            JBList<String> searchBookList = new JBList<>();
//                            for (int i = 0, len =  jsonArray.size(); i < len; i++) {
//                                JsonObject asJsonObject = jsonArray.get(i).getAsJsonObject();
//                                String articleName = asJsonObject.get("articlename").getAsString();
//                                searchBookList.add(new JLabel(articleName));
//                            }
//
//                            searchBookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//
//                            searchBookList.addListSelectionListener(e -> {
//                                if (!e.getValueIsAdjusting()) {
//                                    String selectedValue = searchBookList.getSelectedValue();
//                                    JOptionPane.showMessageDialog(frame, "点击了: " + selectedValue, "提示", JOptionPane.INFORMATION_MESSAGE);
//                                }
//                            });
//
//                            frame.add(new JScrollPane(searchBookList)); // 使用滚动面板来添加滚动条
//                            frame.setVisible(true);
//                        }
//                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public JPanel getContent() {
        return readerPanel;
    }
}
