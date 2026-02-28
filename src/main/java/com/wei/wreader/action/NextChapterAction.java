package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;

import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

public class NextChapterAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        // 停止定时器
        operateAction.executorServiceShutdown();
        // 停止语音
        operateAction.stopTTS();
        // 重置编辑器消息垂直滚动条位置
        cacheService.setEditorMessageVerticalScrollValue(0);

        switch (settings.getDisplayType()) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                operateAction.nextPageChapter((nextPageChapter, bodyElement) -> {
                    if (nextPageChapter == null) {
                        return;
                    }

                    operateAction.updateContentText();

                    if (bodyElement != null) {
                        operateAction.loadThisChapterNextContent(nextPageChapter.getChapterUrl(), bodyElement);
                    }
                });

                break;
            case Settings.DISPLAY_TYPE_STATUSBAR:
                WReaderStatusBarWidget.nextChapter(project);
                break;
        }

    }

}
