package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.wei.wreader.utils.OperateActionUtil;
import org.jetbrains.annotations.NotNull;

public class SearchBookNameAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
        operateAction.buildSearchBookDialog(project);
    }


}
