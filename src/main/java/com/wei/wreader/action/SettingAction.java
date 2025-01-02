package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.wei.wreader.ui.WReaderSettingForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SettingAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        SwingUtilities.invokeLater(() -> {
            ShowSettingsUtil showSettingsUtil = ShowSettingsUtil.getInstance();
            showSettingsUtil.showSettingsDialog(project, WReaderSettingForm.class);
        });
    }
}
