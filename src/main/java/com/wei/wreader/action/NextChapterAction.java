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
import com.wei.wreader.ui.WReaderToolWindow;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class NextChapterAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        OperateActionUtil.getInstance(project).executorServiceShutdown();

        switch (settings.getDisplayType()) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                ChapterInfo nextPageChapter = OperateActionUtil.getInstance(project).nextPageChapter();

                if (nextPageChapter == null) {
                    return;
                }

                ChapterInfo selectedChapterInfoTemp = cacheService.getSelectedChapterInfo();
                selectedChapterInfoTemp.setLastReadLineNum(1);
                selectedChapterInfoTemp.setPrevReadLineNum(1);
                selectedChapterInfoTemp.setNextReadLineNum(1);
                selectedChapterInfoTemp.setChapterContentList(null);

                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                ToolWindow toolWindow = toolWindowManager.getToolWindow(ConstUtil.WREADER_TOOL_WINDOW_ID);
                if (toolWindow != null) {
                    ContentManager contentManager = toolWindow.getContentManager();
                    Content rootContent = contentManager.getContent(0);
                    if (rootContent != null) {
                        // 获取内容面板JTextPane
                        JTextPane contentTextPanel = OperateActionUtil.ToolWindowUtils.getContentTextPanel(rootContent);
                        if (contentTextPanel != null) {
                            String fontColorHex = cacheService.getFontColorHex();
                            String fontFamily = cacheService.getFontFamily();
                            int fontSize = cacheService.getFontSize();
                            String chapterContent = nextPageChapter.getChapterContent();
                            // 设置内容
                            String style = "color:" + fontColorHex + ";" +
                                    "font-family: '" + fontFamily + "';" +
                                    "font-size: " + fontSize + "px;";
                            chapterContent = "<div style=\"" + style + "\">" + chapterContent + "</div>";
                            contentTextPanel.setText(chapterContent);
                            // 设置光标位置
                            contentTextPanel.setCaretPosition(0);
                        }
                    }
                }
                break;
            case Settings.DISPLAY_TYPE_STATUSBAR:
                WReaderStatusBarWidget.nextChapter(project);
                break;
        }

    }

}
