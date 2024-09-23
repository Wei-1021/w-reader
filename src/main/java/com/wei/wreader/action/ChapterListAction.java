package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jsoup.nodes.Element;

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
                if (settings.getDisplayType() == Settings.DISPLAY_TYPE_SIDEBAR) {
                } else if (settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR) {
                    ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
                    selectedChapterInfo.setLastReadLineNum(1);
                    selectedChapterInfo.setPrevReadLineNum(1);
                    selectedChapterInfo.setNextReadLineNum(1);
                    selectedChapterInfo.setChapterContentList(null);
                    WReaderStatusBarWidget.update(project, "");
                }
            }
        });
    }
}
