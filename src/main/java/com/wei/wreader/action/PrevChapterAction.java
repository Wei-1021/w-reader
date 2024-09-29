package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.wei.wreader.factory.WReaderToolWindowFactory;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.ui.WReaderToolWindow;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jsoup.nodes.Element;

import javax.swing.*;

public class PrevChapterAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        switch (settings.getDisplayType()) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
                ChapterInfo prevPageChapter = operateAction.prevPageChapter();

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
                            String fontColorHex = cacheService.getFontColorHex();
                            String fontFamily = cacheService.getFontFamily();
                            int fontSize = cacheService.getFontSize();
                            String chapterContent = prevPageChapter.getChapterContent();
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
                WReaderStatusBarWidget.prevChapter(project);
                break;
            case Settings.DISPLAY_TYPE_TERMINAL:
                break;
            default:
                break;
        }
    }
}
