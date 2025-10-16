package com.wei.wreader.utils.comm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.wei.wreader.utils.data.ConstUtil;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ToolWindowUtil {
    /**
     * 获取ToolWindow的根内容面板
     *
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
     *
     * @param rootContent
     * @return
     */
    public static JPanel getReaderPanel(Content rootContent) {
        return (JPanel) rootContent.getComponent();
    }

    /**
     * 获取ToolWindow的内容面板
     *
     * @param rootContent
     * @return
     */
    public static JPanel getContentPanel(Content rootContent) {
        JPanel readerPanel = getReaderPanel(rootContent);
        return (JPanel) readerPanel.getComponent(0);
    }

    /**
     * 获取ToolWindow的内容滚动面板
     *
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
     *
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

    /**
     * 更新内容面板
     * @param project
     */
    public static void updateContentText(Project project, String content) {
        updateContentText(project, textPane -> textPane.setText(content));
    }

    /**
     * 更新内容面板
     * @param project
     */
    public static void updateContentText(Project project, Consumer<JTextPane> consumer) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(ConstUtil.WREADER_TOOL_WINDOW_ID);
        if (toolWindow != null) {
            ContentManager contentManager = toolWindow.getContentManager();
            Content rootContent = contentManager.getContent(0);
            if (rootContent != null) {
                // 获取内容面板JTextPane
                JTextPane contentTextPanel = getContentTextPanel(rootContent);
                if (contentTextPanel != null && consumer != null) {
                    consumer.accept(contentTextPanel);
                }
            }
        }
    }

    /**
     * 内容面板插入新的内容
     */
    public static void insertContentText(Project project, String content) {
        updateContentText(project, textPane -> {
            String oldContent = textPane.getText();
            textPane.setText(oldContent + content);
        });
    }
}
