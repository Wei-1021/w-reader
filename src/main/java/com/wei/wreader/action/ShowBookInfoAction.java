package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.pojo.SiteBean;
import com.wei.wreader.utils.comm.script.RhinoJsEngine;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.ui.MessageDialogUtil;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.*;

/**
 * 书本信息展示
 */
public class ShowBookInfoAction extends BaseAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        // 获取当前选择的书本信息
        BookInfo selectedBookInfo = cacheService.getSelectedBookInfo();
        if (selectedBookInfo == null) {
            Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_BOOK_ERROR, "提示");
            return;
        }
        // 获取当前选择的书源
        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
        SwingUtilities.invokeLater(() -> {
            String bookInfoStr =
                    "<p>书名：" + selectedBookInfo.getBookName() + "</p>" +
                    "<p>作者：" + selectedBookInfo.getBookAuthor() + "</p>" +
                    "<p>简介：" + selectedBookInfo.getBookDesc() + "</p>";

            if (Settings.DATA_LOAD_TYPE_NETWORK == settings.getDataLoadType()) {
                bookInfoStr =
                        "<p>书名：" + selectedBookInfo.getBookName() + "</p>" +
                        "<p>作者：" + selectedBookInfo.getBookAuthor() + "</p>" +
                        "<p>书源：" + selectedSiteBean.getName() + "</p>" +
                        "<p>书源网址：" + selectedSiteBean.getId() + "</p>" +
                        "<p>简介：" + selectedBookInfo.getBookDesc() + "</p>";
            }

            bookInfoStr = "<div>" + bookInfoStr + "</div>";

            JTextPane bookInfoTextPane = new JTextPane();
            bookInfoTextPane.setContentType("text/html");
            bookInfoTextPane.setEditable(false);
            bookInfoTextPane.setPreferredSize(new Dimension(450, 380));
            bookInfoTextPane.setMaximumSize(new Dimension(450, 550));
            bookInfoTextPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            bookInfoTextPane.setEditorKit(new HTMLEditorKitBuilder().withWordWrapViewFactory().build());
            bookInfoTextPane.setText(bookInfoStr);

            Object[] objects = new Object[] {
                    bookInfoTextPane
            };

            MessageDialogUtil.showMessageDialog(project, "书籍信息", objects, 500, 0, null);

            RhinoJsEngine.test();
        });
    }
}
