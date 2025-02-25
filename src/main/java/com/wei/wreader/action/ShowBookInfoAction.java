package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.components.JBLabel;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.utils.MessageDialogUtil;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;

/**
 * 书本信息展示
 */
public class ShowBookInfoAction extends BaseAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        BookInfo selectedBookInfo = cacheService.getSelectedBookInfo();
        SwingUtilities.invokeLater(() -> {
            JBLabel bookNameLabel = new JBLabel("书名：" + selectedBookInfo.getBookName());
            bookNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
            JBLabel bookAuthorLabel = new JBLabel("作者：" + selectedBookInfo.getBookAuthor());
            bookAuthorLabel.setHorizontalAlignment(SwingConstants.LEFT);
            JBLabel bookDescLabel = new JBLabel("简介：" + selectedBookInfo.getBookDesc());
            bookDescLabel.setHorizontalAlignment(SwingConstants.LEFT);

            Object[] objects = new Object[] {
                    bookNameLabel,
                    bookAuthorLabel,
                    bookDescLabel
            };

            MessageDialogUtil.showMessageDialog(project, MessageDialogUtil.TITLE_INFO, objects, null);
        });
    }
}
