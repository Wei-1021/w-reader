package com.wei.wreader.widget;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.wm.StatusBarWidget;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * 底部状态栏试图
 * @author weizhanjie
 */
public class WReaderStatusBarPresentation implements StatusBarWidget.MultipleTextValuesPresentation {

    private final ConfigYaml configYaml;
    private final CacheService cacheService;
    private final BookInfo selectedBookInfo;
    private final ChapterInfo selectedChapterInfo;
    private Settings settings;

    public WReaderStatusBarPresentation() {
        configYaml = ConfigYaml.getInstance();
        cacheService = CacheService.getInstance();
        selectedBookInfo = cacheService.getSelectedBookInfo();
        selectedChapterInfo = cacheService.getSelectedChapterInfo();
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }
    }

    @Nullable
    @Override
    public String getTooltipText() {
        if (selectedBookInfo == null || selectedChapterInfo == null) {
            return configYaml.getNameHump();
        }

        return selectedBookInfo.getBookName() + "|" + selectedChapterInfo.getChapterTitle();
    }

    @Nullable
    @Override
    public String getSelectedValue() {
        return "";
    }

    @Nullable
    @Override
    public JBPopup getPopup() {
        return StatusBarWidget.MultipleTextValuesPresentation.super.getPopup();
    }

}
