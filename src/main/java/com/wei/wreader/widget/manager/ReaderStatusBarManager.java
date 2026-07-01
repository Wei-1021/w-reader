package com.wei.wreader.widget.manager;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.wei.wreader.factory.WReaderStatusBarFactory;
import com.wei.wreader.model.BookInfo;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.data.StringUtil;
import com.wei.wreader.util.yml.ConfigYaml;
import com.wei.wreader.widget.ReaderStatusBarWidget;

import java.util.List;

public class ReaderStatusBarManager {

    private CacheService cacheService;
    private ConfigYaml configYaml;
    private Settings settings;

    public void setEnabled(Project project) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        configYaml = ConfigYaml.getInstance();
        if (settings == null) {
            settings = configYaml.getSettings();
        }

        boolean isStatusBarMode = settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;

        // 确保 widget 已被平台创建（首次切到状态栏模式时需要）
        StatusBarWidgetsManager statusBarWidgetsManager = project.getService(StatusBarWidgetsManager.class);
        statusBarWidgetsManager.updateWidget(WReaderStatusBarFactory.class);

        // 根据当前模式切换 widget 可见性
//        ReaderStatusBarWidget widget = ReaderStatusBarWidget.findWidget(project);
//        if (widget != null) {
//            widget.setVisible(isStatusBarMode);
//        }

        // 状态栏模式下刷新章节内容缓存
        if (isStatusBarMode) {
            ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
            if (selectedChapterInfo != null) {
                String chapterContentStr = selectedChapterInfo.getChapterContentStr();
                int singleLineChars = settings.getSingleLineChars();
                List<String> contentList = StringUtil.splitStringByMaxCharList(chapterContentStr, singleLineChars);
                selectedChapterInfo.setChapterContentList(contentList);
            }
        }
    }
}
