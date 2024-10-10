package com.wei.wreader.action;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 编辑器提示信息
 *
 * @author weizhanjie
 */
public class EditorMessageAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        // 获取当前编辑器实例
        Editor editor = e.getRequiredData(LangDataKeys.EDITOR);

        // 获取小说内容
        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            return;
        }

        // 获取小说内容
        String content = selectedChapterInfo.getChapterContent();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();
        String style = String.format("font-family:%s;font-size:%dpx;color:%s;",
                ConstUtil.LINE_COMMENT_COLOR, fontSize, fontColorHex);
        content = String.format("<div style='%s'>%s</div>", style, content);

        // 设置文本显示区域组件
        JTextPane contentTextPane = new JTextPane();
        contentTextPane.setContentType("text/html");
        contentTextPane.setEditable(false);
        contentTextPane.setAlignmentY(Component.TOP_ALIGNMENT);
        contentTextPane.setText(content);

        // 获取提示框背景色
        Color tooltipBackground = JBUI.CurrentTheme.Editor.Tooltip.BACKGROUND;
        contentTextPane.setBackground(tooltipBackground);

        // 设置滚动面板
        JBScrollPane scrollPane = new JBScrollPane();
        scrollPane.setViewportView(contentTextPane);
        scrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
        scrollPane.setPreferredSize(new Dimension(350, 200));
        scrollPane.setBorder(JBUI.Borders.empty());

        // 滚动到顶部
        contentTextPane.setCaretPosition(0);

        // 显示提示框
        HintManager hintManager = HintManager.getInstance();
        hintManager.showInformationHint(editor, scrollPane);
    }
}
