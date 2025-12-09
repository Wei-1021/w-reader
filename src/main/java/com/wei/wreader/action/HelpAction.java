package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.*;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.utils.ui.MessageDialogUtil;
import com.wei.wreader.utils.file.ImagePreviewer;
//import com.wei.wreader.utils.jcef.JCEFUtil;

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
//        // 统一外观模式
//        JCEFUtil.injectTheme(jbCefBrowser);
//        // JavaScript调用Java
//        JCEFUtil.addJSQuery(jbCefBrowser,
//                (query) -> {
//                    ImagePreviewer imagePreviewer = new ImagePreviewer(project, query);
//                    imagePreviewer.openImagePreview();
//                    return null;
//                },
//                """
//                document.addEventListener('click', function (e) {
//                    if (e.target.tagName === 'IMG') {
//                        let imgSrc = e.target.src;
//                        alert("图片被点击了：" + imgSrc);
//                        if (imgSrc && imgSrc != '') {
//                            window.jsCallJavaFunction(imgSrc);
//                        }
//                    }
//                });
//                """);
//        jbCefBrowser.loadHTML(html);

//        JCEFHelper jcefHelper = new JCEFHelper(project);
//        // 注册默认方法
//        jcefHelper.registerDefaultMethods();
//        jcefHelper.registerJavaMethod("showImage", (query) -> {
//            ImagePreviewer imagePreviewer = new ImagePreviewer(project, String.valueOf(query));
//            imagePreviewer.openImagePreview();
//            return null;
//        });
//
//        // 把 IDE 主题变量注入页面
//        int labelFontSize = UIUtil.getLabelFont().getSize();
//        String script = "window.IDE_THEME='" + (ThemeUtils.isDarkTheme() ? "dark" : "light") + "';" +
//                "document.documentElement.style.setProperty('--jb-font-size','" + labelFontSize + "px');";
//        // 滚动条样式一键同步
//        script += "const s=document.createElement('style');" +
//                "s.innerHTML='* {color: #FFF;} ';" +
//                "document.head.appendChild(s);";
//        script += """
//                    document.addEventListener('click', function (e) {
//                        console.log(e)
//                        if (e.target.tagName === 'IMG') {
//                            let imgSrc = e.target.src;
//                            if (imgSrc && imgSrc != '') {
//                                javaBridge.showImage(imgSrc);
//                            }
//                        }
//                    });
//                    """;
//        jcefHelper.registerJsCode(script);
//        jcefHelper.loadHTML(html);

        panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
        frame.add(panel);
        frame.setVisible(true);
        jbCefBrowser.openDevtools();
    }


}
