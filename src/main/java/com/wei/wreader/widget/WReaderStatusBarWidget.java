package com.wei.wreader.widget;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WReaderStatusBarWidget implements StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation {
    private final Project project;

    public WReaderStatusBarWidget(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String ID() {
        return "";
    }

    @Override
    public void dispose() {
        StatusBarWidget.super.dispose();
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation() {
        return StatusBarWidget.super.getPresentation();
    }

    @Nullable
    @Override
    public String getSelectedValue() {
        return "";
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "";
    }
}
