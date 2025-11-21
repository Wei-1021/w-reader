package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.utils.SearchBookUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SearchBookNameAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        SwingUtilities.invokeLater(() -> {
//            OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
//            operateAction.buildSearchBookDialog(project);

            SearchBookUtil searchBookUtil = new SearchBookUtil(project);
            searchBookUtil.buildSearchBookDialog();
        });
    }


}
