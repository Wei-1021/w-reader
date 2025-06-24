package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.components.JBLabel;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.BookSiteInfo;
import com.wei.wreader.utils.LegadoSourceParser;
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

        // 获取当前选择的书本信息
        BookInfo selectedBookInfo = cacheService.getSelectedBookInfo();
        // 获取当前选择的书源
        BookSiteInfo selectedBookSiteInfo = cacheService.getSelectedBookSiteInfo();
        SwingUtilities.invokeLater(() -> {
//            LegadoSourceParser legadoSourceParser = new LegadoSourceParser();
//            legadoSourceParser.test();

            JBLabel bookSiteNameLabel = new JBLabel("书源：" + selectedBookSiteInfo.getName());
            bookSiteNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
            JBLabel bookSiteIdLabel = new JBLabel("书源网址：" + selectedBookSiteInfo.getId());
            bookSiteIdLabel.setHorizontalAlignment(SwingConstants.LEFT);
            JBLabel bookNameLabel = new JBLabel("书名：" + selectedBookInfo.getBookName());
            bookNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
            JBLabel bookAuthorLabel = new JBLabel("作者：" + selectedBookInfo.getBookAuthor());
            bookAuthorLabel.setHorizontalAlignment(SwingConstants.LEFT);
            JBLabel bookDescLabel = new JBLabel("简介：" + selectedBookInfo.getBookDesc());
            bookDescLabel.setHorizontalAlignment(SwingConstants.LEFT);

            Object[] objects = new Object[] {
                    bookSiteNameLabel,
                    bookSiteIdLabel,
                    bookNameLabel,
                    bookAuthorLabel,
                    bookDescLabel
            };

            MessageDialogUtil.showMessageDialog(project, "书籍信息", objects, 350, 250,
                    null);
        });
    }
}
