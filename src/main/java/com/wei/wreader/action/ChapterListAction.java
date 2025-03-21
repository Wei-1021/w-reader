package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;
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

                OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
                // 停止定时器
                operateAction.executorServiceShutdown();
                // 停止语音
                operateAction.stopTTS();
                // 重置编辑器消息垂直滚动条位置
                cacheService.setEditorMessageVerticalScrollValue(0);

                switch (settings.getDisplayType()) {
                    case Settings.DISPLAY_TYPE_SIDEBAR:
                        ChapterInfo selectedChapterInfoTemp = cacheService.getSelectedChapterInfo();
                        selectedChapterInfoTemp.setLastReadLineNum(1);
                        selectedChapterInfoTemp.setPrevReadLineNum(1);
                        selectedChapterInfoTemp.setNextReadLineNum(1);
                        selectedChapterInfoTemp.setChapterContentList(null);

                        operateAction.updateContentText();

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
