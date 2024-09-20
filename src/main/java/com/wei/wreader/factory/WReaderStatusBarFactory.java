package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.intellij.ui.content.ContentFactory;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 状态栏工厂
 * @author weizhanjie
 */
public class WReaderStatusBarFactory implements StatusBarWidgetFactory {
    private CacheService cacheService;
    private Settings settings;

    private static final String WIDGET_ID = ConfigYaml.getInstance().getNameHump() + "StatusBar";
    private static final String DISPLAY_NAME = ConfigYaml.getInstance().getNameHump() + "StatusBar";

    @Override
    public @NotNull @NonNls String getId() {
        return WIDGET_ID;
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        return new WReaderStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        StatusBarWidgetFactory.super.disposeWidget(widget);
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

    /**
     * 是否启用底部状态栏
     * @param project
     * @param isStartupApp 是否是启动项目
     */
    public void setEnabled(@NotNull Project project, boolean isStartupApp) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();

        WindowManager windowManager = WindowManager.getInstance();
        StatusBar statusBar = windowManager.getStatusBar(project);
        if (statusBar != null) {
            boolean isVisible = settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
            StatusBarWidget wReaderStatusBarWidget = statusBar.getWidget(WReaderStatusBarWidget.getWidgetId());
            if (isVisible) {
                if (wReaderStatusBarWidget == null && !isStartupApp) {
                    statusBar.addWidget(new WReaderStatusBarWidget(project));
                }
            } else {
                statusBar.removeWidget(WReaderStatusBarWidget.getWidgetId());
            }
        }
    }

}
