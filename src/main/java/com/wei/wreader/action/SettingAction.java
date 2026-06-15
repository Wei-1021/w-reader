package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.Messages;
import com.wei.wreader.ui.WReaderSettingForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SettingAction extends BaseAction {
    private static final Logger LOG = Logger.getInstance(SettingAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        SwingUtilities.invokeLater(() -> {
            try {
                ShowSettingsUtil showSettingsUtil = ShowSettingsUtil.getInstance();
                showSettingsUtil.showSettingsDialog(project, WReaderSettingForm.class);
            } catch (Exception ex) {
                LOG.warn("Failed to show settings dialog", ex);
                Messages.showErrorDialog("打开设置页面失败: " + ex.getMessage(), "W-Reader");
            }
        });
    }
}
