package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.OperateActionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 改变字体颜色
 * @author weizhanjie
 */
public class ChangeFontColorAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            OperateActionUtil.getInstance().changeFontColor();
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                    ToolWindow toolWindow = toolWindowManager.getToolWindow(ConstUtil.WREADER_TOOL_WINDOW_ID);

                    if (toolWindow != null) {
                        ContentManager contentManager = toolWindow.getContentManager();
                        Content rootContent = contentManager.getContent(0);
                        if (rootContent != null) {
                            // 获取内容面板JTextPane
                            JTextPane contentTextPanel = OperateActionUtil.ToolWindow.getContentTextPanel(rootContent);
                            if (contentTextPanel != null) {
                                int caretPosition = contentTextPanel.getCaretPosition();
                                String text = getContent(contentTextPanel, cacheService, selectedChapterInfo);
                                contentTextPanel.setText(text);
                                contentTextPanel.setCaretPosition(caretPosition);
                            }
                        }
                    }
                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    break;
                case Settings.DISPLAY_TYPE_TERMINAL:
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * 获取内容
     * @param contentTextPanel
     * @param cacheService
     * @param selectedChapterInfo
     * @return
     */
    private static @NotNull String getContent(JTextPane contentTextPanel,
                                             CacheService cacheService,
                                             ChapterInfo selectedChapterInfo) {
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();
        String content = selectedChapterInfo.getChapterContent();
        // 设置内容
        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";
        String text = "<div style=\"" + style + "\">" + content + "</div>";
        return text;
    }
}
