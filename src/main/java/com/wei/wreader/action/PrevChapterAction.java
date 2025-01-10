package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 上一章
 */
public class PrevChapterAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
        OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
        // 停止定时器
        operateAction.executorServiceShutdown();
        // 停止语音
        operateAction.stopTTS();

        switch (settings.getDisplayType()) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                ChapterInfo prevPageChapter = operateAction.prevPageChapter();

                if (prevPageChapter == null) {
                    return;
                }

                ChapterInfo selectedChapterInfoTemp = cacheService.getSelectedChapterInfo();
                selectedChapterInfoTemp.setLastReadLineNum(1);
                selectedChapterInfoTemp.setPrevReadLineNum(1);
                selectedChapterInfoTemp.setNextReadLineNum(1);
                selectedChapterInfoTemp.setChapterContentList(null);

                operateAction.updateContentText();

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
