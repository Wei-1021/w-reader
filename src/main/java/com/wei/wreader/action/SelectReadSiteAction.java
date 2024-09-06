package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowManager;
import com.wei.wreader.ui.SelectReadSiteDialog;

public class SelectReadSiteAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e == null) {
            Messages.showWarningDialog("功能出现异常！", "提示");
            return;
        }

        Project project = e.getProject();
        if (project == null) {
            Messages.showWarningDialog("当前项目为空！", "提示");
            return;
        }

        SelectReadSiteDialog dialog = new SelectReadSiteDialog();
        dialog.pack();
        dialog.setVisible(true);

    }
}
