package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.wei.wreader.utils.MessageDialogUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * 帮助
 *
 * @author weizhanjie
 */
public class HelpAction extends BaseAction {

    private static final String HELP_FILE_PATH = "html/help.html";

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        try (InputStream is = HelpAction.class.getClassLoader().getResourceAsStream(HELP_FILE_PATH)) {
            if (is == null) {
                Messages.showErrorDialog(MessageDialogUtil.HELP_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
                return;
            }

            String content = new String(is.readAllBytes());

            JTextPane textPane = new JTextPane();
            textPane.setContentType("text/html");
            textPane.setText(content);

            JBScrollPane scrollPane = new JBScrollPane();
            scrollPane.setViewportView(textPane);
            scrollPane.setPreferredSize(new Dimension(500, 600));

            MessageDialogUtil.showMessageDialog(project, MessageDialogUtil.TITLE_HELP, scrollPane, null);
        } catch (IOException ex) {
            Messages.showErrorDialog(MessageDialogUtil.HELP_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
            throw new RuntimeException(ex);
        }
    }
}
