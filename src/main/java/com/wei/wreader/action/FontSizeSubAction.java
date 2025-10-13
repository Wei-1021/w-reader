package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.utils.comm.ToolWindowUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 字体大小减小
 *
 * @author weizhanjie
 */
public class FontSizeSubAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            return;
        }


        SwingUtilities.invokeLater(() -> {
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    OperateActionUtil.getInstance(project).fontSizeSub();

                    ToolWindowUtil.updateContentText(project, contentTextPanel -> {
                        int caretPosition = contentTextPanel.getCaretPosition();
                        String text = getContent(cacheService, selectedChapterInfo);
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
    private static @NotNull String getContent(CacheService cacheService, ChapterInfo selectedChapterInfo) {
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();
        String content = selectedChapterInfo.getChapterContent();
        // 设置内容
        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px;" +
                "color:" + fontColorHex + ";";
        String text = "<div style=\"" + style + "\">" + content + "</div>";
        return text;
    }
}
