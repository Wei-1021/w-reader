package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.utils.ui.ToolWindowUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 字体放大
 *
 * @author weizhanjie
 */
public class FontSizeAddAction extends BaseAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    OperateActionUtil.getInstance(project).fontSizeAdd();

                    ToolWindowUtil.updateContentText(project, contentTextPanel -> {
                        int caretPosition = contentTextPanel.getCaretPosition();
                        String text = getContent(selectedChapterInfo);
                        contentTextPanel.setText(text);
                        contentTextPanel.setCaretPosition(caretPosition);
                    });
                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    break;
                case Settings.DISPLAY_TYPE_TERMINAL:
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * 获取内容
     * @param cacheService
     * @param selectedChapterInfo
     * @return
     */
    private @NotNull String getContent(CacheService cacheService, ChapterInfo selectedChapterInfo) {
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();
        String content = selectedChapterInfo.getChapterContent();
        // 设置内容
        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";
        return "<div style=\"" + style + "\">" + content + "</div>";
    }

    private @NotNull String getContent(ChapterInfo selectedChapterInfo) {
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();
        String content = selectedChapterInfo.getChapterContent();
        // 设置内容
        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";

        return "<div style=\"" + style + "\">" + content + "</div>";
    }
}
