package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.wei.wreader.ui.WReaderSettingForm;

import javax.swing.*;

public class SettingAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        SwingUtilities.invokeLater(() -> {
            Project project = e.getProject();
            ShowSettingsUtil showSettingsUtil = ShowSettingsUtil.getInstance();
            showSettingsUtil.showSettingsDialog(project, WReaderSettingForm.class);
        });
    }
}
