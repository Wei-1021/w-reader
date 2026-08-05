package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.wei.wreader.ui.BookshelfPanel;
import com.wei.wreader.util.WReaderIcons;
import com.wei.wreader.util.ui.MessageDialogUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class BookshelfAction extends BaseAction {

    public BookshelfAction() {
        super();
        getTemplatePresentation().setText("书架");
        getTemplatePresentation().setIcon(WReaderIcons.BOOKSHELF);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        if (project == null) return;

        SwingUtilities.invokeLater(() -> {
            BookshelfPanel bookshelfPanel = new BookshelfPanel(project);

            MessageDialogUtil.showMessageDialog(
                    project, "书架", new Object[]{bookshelfPanel}, 500, 500, null);
        });
    }
}