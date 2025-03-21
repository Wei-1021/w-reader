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
import com.wei.wreader.utils.ImagePreviewer;
import com.wei.wreader.utils.OperateActionUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Editor editor = e.getData(LangDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

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
        scrollPane.setPreferredSize(new Dimension(ConstUtil.HINT_MANAGER_DIALOG_WIDTH, ConstUtil.HINT_MANAGER_DIALOG_HEIGHT));
        scrollPane.setBorder(JBUI.Borders.empty());

        contentTextPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int pos = OperateActionUtil.getClickedPosition(contentTextPane, mouseEvent);
                if (pos == -1) {
                    return;
                }

                // 获取点击位置处的HTML标签
                String htmlTag = OperateActionUtil.getHTMLTagAtPosition(contentTextPane, pos);
                if (StringUtils.isNotBlank(htmlTag) && htmlTag.contains("<img")) {
                    // 提取img标签中的src属性
                    Matcher matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(htmlTag);
                    if (matcher.find()) {
                        String imageUrl = matcher.group(1);
                        if (StringUtils.isNotBlank(imageUrl)) {
                            // 图片预览
                            ImagePreviewer imagePreviewer = new ImagePreviewer(project, imageUrl);
                            imagePreviewer.openImagePreview();
                        }
                    }
                }
            }
        });

        // 滚动到上一次阅读的位置
        SwingUtilities.invokeLater(() -> {
            int value = cacheService.getEditorMessageVerticalScrollValue();
            scrollPane.getVerticalScrollBar().setValue(value);
        });

        // 显示提示框
        HintManager hintManager = HintManager.getInstance();
        hintManager.showInformationHint(editor, scrollPane, () -> {
            int value = scrollPane.getVerticalScrollBar().getValue();
            cacheService.setEditorMessageVerticalScrollValue(value);
        });
    }
}
