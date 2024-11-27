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
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

import javax.swing.*;
import java.util.List;

/**
 * 章节目录
 */
public class ChapterListAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
        operateAction.showBookDirectory(new BookDirectoryListener() {
            /**
             * 点击章节目录选项
             * @param position 下标
             * @param chapterList 章节列表
             * @param chapterInfo 章节内容
             */
            @Override
            public void onClickItem(int position, List<String> chapterList, ChapterInfo chapterInfo) {
                super.onClickItem(position, chapterList, chapterInfo);

                // 停止定时器
                OperateActionUtil.getInstance(project).executorServiceShutdown();
                // 停止语音
                OperateActionUtil.getInstance(project).stopTTS();

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
                                JTextPane contentTextPanel = OperateActionUtil.ToolWindowUtils.getContentTextPanel(rootContent);
                                if (contentTextPanel != null) {
                                    // 设置内容
                                    String fontColorHex = cacheService.getFontColorHex();
                                    String fontFamily = cacheService.getFontFamily();
                                    int fontSize = cacheService.getFontSize();
                                    String chapterContent = chapterInfo.getChapterContent();
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
