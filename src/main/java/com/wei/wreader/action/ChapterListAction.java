package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;

import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

import java.util.List;

/**
 * 章节目录
 */
public class ChapterListAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        operateAction.showBookDirectory(new BookDirectoryListener() {
            /**
             * 点击章节目录选项
             * @param position 下标
             * @param chapterList 章节列表
             * @param chapterInfo 章节内容
             * @param element 文章内容html页面{@code <body></body>}部分的元素
             */
            @Override
            public void onClickItem(int position, List<String> chapterList, ChapterInfo chapterInfo, Element element) {
                super.onClickItem(position, chapterList, chapterInfo, element);

                // 停止定时器
                operateAction.executorServiceShutdown();
                // 停止语音
                operateAction.stopTTS();
                // 重置编辑器消息垂直滚动条位置
                cacheService.setEditorMessageVerticalScrollValue(0);

                switch (settings.getDisplayType()) {
                    case Settings.DISPLAY_TYPE_SIDEBAR:
//                        ChapterInfo selectedChapterInfoTemp = cacheService.getSelectedChapterInfo();
                        chapterInfo.initLineNum(1, 1, 1);
                        cacheService.setSelectedChapterInfo(chapterInfo);

                        operateAction.updateContentText();
                        if (element != null) {
                            operateAction.loadThisChapterNextContent(chapterInfo.getChapterUrl(), element);
                        }
                    case Settings.DISPLAY_TYPE_STATUSBAR:
                        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
                        selectedChapterInfo.initLineNum(1, 1, 1);
                        WReaderStatusBarWidget.update(project, "");
                        break;
                    case Settings.DISPLAY_TYPE_TERMINAL:
                        break;
                }
            }
        });
    }
}
