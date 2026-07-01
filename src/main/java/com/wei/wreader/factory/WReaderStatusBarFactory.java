package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.yml.ConfigYaml;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.StringUtil;
import com.wei.wreader.widget.ReaderStatusBarWidget;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 状态栏工厂
 * @author weizhanjie
 */
public class WReaderStatusBarFactory implements StatusBarWidgetFactory {
    private CacheService cacheService;
    private Settings settings;
    private ConfigYaml configYaml;

    private String WIDGET_ID;
    private String DISPLAY_NAME;

    @Override
    public @NotNull @NonNls String getId() {
        WIDGET_ID = ConstUtil.WREADER_STATUS_BAR_ID;
        return WIDGET_ID;
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        DISPLAY_NAME = ConstUtil.WREADER_STATUS_BAR_ID;
        return DISPLAY_NAME;
    }


    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();

        return new ReaderStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();

        return settings != null && settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        return settings != null && settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
    }

}
