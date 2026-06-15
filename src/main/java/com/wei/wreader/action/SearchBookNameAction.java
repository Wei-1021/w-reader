package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.search.SearchDialog;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SearchBookNameAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        SwingUtilities.invokeLater(() -> {
            SearchDialog searchDialog = new SearchDialog(project);
            searchDialog.showSearchDialog(null);
        });
    }


}
