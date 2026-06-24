package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.content.ContentFormatter;
import com.wei.wreader.content.HtmlContentRenderer;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.Settings;
import com.wei.wreader.util.ui.ToolWindowUtil;
import com.wei.wreader.widget.ReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ChangeFontColorAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) return;

        SwingUtilities.invokeLater(() -> {
            orchestrator.changeFontColor();
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    ToolWindowUtil.updateContentText(project, textPanel -> {
                        int caretPosition = textPanel.getCaretPosition();
                        String chapterContent = ContentFormatter.removeStyleTags(selectedChapterInfo.getChapterContent());
                        String fontFamily = cacheService.getFontFamily();
                        int fontSize = cacheService.getFontSize();
                        String fontColorHex = cacheService.getFontColorHex();
                        String text = HtmlContentRenderer.getStyledContent(chapterContent, fontFamily, fontSize, fontColorHex);
                        textPanel.setText(text);
                        textPanel.setCaretPosition(caretPosition);
                    });
                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    ReaderStatusBarWidget.updateFont(project);
                    break;
            }
        });
    }
}
