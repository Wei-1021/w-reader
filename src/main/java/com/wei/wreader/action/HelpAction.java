package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import com.wei.wreader.utils.MessageDialogUtil;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLoadHandlerAdapter;

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

            JBScrollPane scrollPane = new JBScrollPane();
            scrollPane.setViewportView(textPane);
            scrollPane.setPreferredSize(new Dimension(500, 600));

            textPane.setCaretPosition(0);

            MessageDialogUtil.showMessageDialog(project, MessageDialogUtil.TITLE_HELP, scrollPane, null);
        } catch (IOException ex) {
            Messages.showErrorDialog(MessageDialogUtil.HELP_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
            throw new RuntimeException(ex);
        }
    }

    public void JCEF(String html) {
        if (!JBCefApp.isSupported()) {
            return;
        }

        JBCefApp cefApp = JBCefApp.getInstance();
        JBCefClient client = cefApp.createClient();
        // CefMessageRouter 用于处理来自 Chromium 浏览器的消息和事件，
        // 前端代码可以通过innerCefQuery和innerCefQueryCancel发起消息给插件进行处理
        CefMessageRouter.CefMessageRouterConfig routerConfig = new CefMessageRouter
                .CefMessageRouterConfig("innerCefQuery", "innerCefQueryCancel");
        CefMessageRouter messageRouter = CefMessageRouter.create(routerConfig);
        client.getCefClient().addMessageRouter(messageRouter);


        JBCefBrowser jbCefBrowser = new JBCefBrowser();

        JFrame frame = new JFrame("JCEF Swing Example");
        frame.setSize(800, 600);

        // 创建一个面板来容纳浏览器
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);

        // 添加面板到窗口
        frame.add(panel);

        // 显示窗口
        frame.setVisible(true);

        jbCefBrowser.loadHTML(html);
        jbCefBrowser.getCefBrowser().executeJavaScript("alert('Hello World!')", "http://127.0.0.1:8080", 0);
    }
}
