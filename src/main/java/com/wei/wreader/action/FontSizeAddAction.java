package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.wei.wreader.content.ContentFormatter;
import com.wei.wreader.content.HtmlContentRenderer;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.Settings;
import com.wei.wreader.util.ui.ToolWindowUtil;
import org.jetbrains.annotations.NotNull;

public class FontSizeAddAction extends BaseAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    orchestrator.fontSizeAdd();

                    ToolWindowUtil.updateContentText(project, contentTextPanel -> {
                        int caretPosition = contentTextPanel.getCaretPosition();
                        String chapterContent = ContentFormatter.removeStyleTags(selectedChapterInfo.getChapterContent());
                        String fontFamily = cacheService.getFontFamily();
                        int fontSize = cacheService.getFontSize();
                        String fontColorHex = cacheService.getFontColorHex();
                        String text = HtmlContentRenderer.getStyledContent(chapterContent, fontFamily, fontSize, fontColorHex);
                        contentTextPanel.setText(text);
                        contentTextPanel.setCaretPosition(caretPosition);
                    });
                    break;
            }
        });
    }
}
