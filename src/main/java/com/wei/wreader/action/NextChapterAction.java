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
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class NextChapterAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        CacheService cacheService = CacheService.getInstance();
        Settings settings = cacheService.getSettings();
        if (settings == null) {
            return;
        }

        switch (settings.getDisplayType()) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                Element element = OperateActionUtil.getInstance().nextPageChapter();

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
                        JTextPane contentTextPanel = OperateActionUtil.ToolWindow.getContentTextPanel(rootContent);
                        if (contentTextPanel != null) {
                            // 设置内容
                            contentTextPanel.setText(element.html());
                            // 设置光标位置
                            contentTextPanel.setCaretPosition(0);
                        }
                    }
                }
                break;
            case Settings.DISPLAY_TYPE_STATUSBAR:
                WReaderStatusBarWidget.nextChapter(e.getProject());
                break;
            case Settings.DISPLAY_TYPE_TERMINAL:
                break;
        }
    }

}
