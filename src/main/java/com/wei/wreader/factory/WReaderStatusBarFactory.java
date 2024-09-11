package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 状态栏工厂
 * @author weizhanjie
 */
public class WReaderStatusBarFactory implements StatusBarWidgetFactory {

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
        return new WReaderStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        StatusBarWidgetFactory.super.disposeWidget(widget);
    }

}
