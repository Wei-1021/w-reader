package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.model.Settings;

import com.wei.wreader.widget.ReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

public class NextChapterAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        // 停止定时器
        orchestrator.executorServiceShutdown();
        // 停止语音
        orchestrator.stopTTS();
        // 重置编辑器消息垂直滚动条位置
        cacheService.setEditorMessageVerticalScrollValue(0);

        switch (settings.getDisplayType()) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                orchestrator.nextPageChapter((nextPageChapter, bodyElement) -> {
                    if (nextPageChapter == null) {
                        return;
                    }

                    orchestrator.updateContentText();

                    if (bodyElement != null) {
                        orchestrator.loadThisChapterNextContent(nextPageChapter.getChapterUrl(), bodyElement);
                    }
                });

                break;
            case Settings.DISPLAY_TYPE_STATUSBAR:
                ReaderStatusBarWidget.nextChapter(project);
                break;
        }

    }

}
