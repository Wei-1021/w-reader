package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

public class NextChapterAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        OperateActionUtil operateActionUtil = OperateActionUtil.getInstance(project);
        // 停止定时器
        operateActionUtil.executorServiceShutdown();
        // 停止语音
        operateActionUtil.stopTTS();
        // 重置编辑器消息垂直滚动条位置
        cacheService.setEditorMessageVerticalScrollValue(0);

        switch (settings.getDisplayType()) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                operateActionUtil.nextPageChapter((nextPageChapter, bodyElement) -> {
                    if (nextPageChapter == null) {
                        return;
                    }

                    ChapterInfo selectedChapterInfoTemp = cacheService.getSelectedChapterInfo();
                    selectedChapterInfoTemp.initLineNum(1, 1, 1);

                    operateActionUtil.updateContentText();

                    if (bodyElement != null) {
                        operateActionUtil.loadThisChapterNextContent(nextPageChapter.getChapterUrl(), bodyElement);
                    }
                });

                break;
            case Settings.DISPLAY_TYPE_STATUSBAR:
                WReaderStatusBarWidget.nextChapter(project);
                break;
        }

    }

}
