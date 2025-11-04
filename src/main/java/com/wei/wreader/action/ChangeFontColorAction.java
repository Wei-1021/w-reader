package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.utils.ui.ToolWindowUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 改变字体颜色
 * @author weizhanjie
 */
public class ChangeFontColorAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            OperateActionUtil.getInstance(project).changeFontColor();
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    ToolWindowUtil.updateContentText(project, (textPanel) -> {
                        int caretPosition = textPanel.getCaretPosition();
                        String text = getContent(textPanel, cacheService, selectedChapterInfo);
                        textPanel.setText(text);
                        textPanel.setCaretPosition(caretPosition);
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
     * @param contentTextPanel
     * @param cacheService
     * @param selectedChapterInfo
     * @return
     */
    private static @NotNull String getContent(JTextPane contentTextPanel,
                                             CacheService cacheService,
                                             ChapterInfo selectedChapterInfo) {
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
