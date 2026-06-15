package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.*;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.util.ui.MessageDialogUtil;
import com.wei.wreader.util.file.ImagePreviewer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 帮助
 *
 * @author weizhanjie
 */
public class HelpAction extends BaseAction {

    private static final String HELP_FILE_PATH = "html/help.html";

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        try (InputStream is = HelpAction.class.getClassLoader().getResourceAsStream(HELP_FILE_PATH)) {
            if (is == null) {
                Messages.showErrorDialog(MessageDialogUtil.HELP_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
                return;
            }

            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            JEditorPane textPane = new JEditorPane();
            textPane.setEditable(false);
            textPane.setContentType("text/html");
            textPane.setText(content);
            // 设置背景色为主题背景色
            textPane.setBackground(UIManager.getColor("Panel.background"));

            JBScrollPane scrollPane = new JBScrollPane();
            scrollPane.setViewportView(textPane);
            scrollPane.setPreferredSize(new Dimension(500, 450));
            scrollPane.setMaximumSize(new Dimension(500, 450));
            scrollPane.setBorder(JBUI.Borders.empty());

            textPane.setCaretPosition(0);
            MessageDialogUtil.showMessageDialog(project, MessageDialogUtil.TITLE_HELP, scrollPane, null);
        } catch (IOException ex) {
            Messages.showErrorDialog(MessageDialogUtil.HELP_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
            throw new RuntimeException(ex);
        }
    }

    // TODO: JCEF 测试
    public void JCEF(String html) {
        if (!JBCefApp.isSupported()) {
            return;
        }

        JFrame frame = new JFrame("JCEF Swing Example");
        frame.setSize(800, 600);
        // 创建一个面板来容纳浏览器
        JPanel panel = new JPanel(new BorderLayout());
        JBCefBrowser jbCefBrowser = new JBCefBrowser();

        panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
        frame.add(panel);
        frame.setVisible(true);
        jbCefBrowser.openDevtools();
    }


}
