package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jsoup.nodes.Element;

import javax.swing.*;
import java.util.List;

public class ChapterListAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        CacheService cacheService = CacheService.getInstance();
        Settings settings = cacheService.getSettings();

        OperateActionUtil operateAction = OperateActionUtil.getInstance();
        operateAction.showBookDirectory(new BookDirectoryListener() {
            @Override
            public void onClickItem(int position, List<String> chapterList, Element chapterElement) {
                super.onClickItem(position, chapterList, chapterElement);

                switch (settings.getDisplayType()) {
                    case Settings.DISPLAY_TYPE_SIDEBAR:
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
                                    contentTextPanel.setText(chapterElement.html());
                                    // 设置光标位置
                                    contentTextPanel.setCaretPosition(0);
                                }
                            }
                        }
                    case Settings.DISPLAY_TYPE_STATUSBAR:
                        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
                        selectedChapterInfo.setLastReadLineNum(1);
                        selectedChapterInfo.setPrevReadLineNum(1);
                        selectedChapterInfo.setNextReadLineNum(1);
                        selectedChapterInfo.setChapterContentList(null);
                        WReaderStatusBarWidget.update(project, "");
                        break;
                    case Settings.DISPLAY_TYPE_TERMINAL:
                        break;
                }
            }
        });
    }
}
