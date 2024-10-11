package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.ui.MessageUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.utils.MessageDialogUtil;
import com.wei.wreader.utils.OperateActionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * 代码注释块
 *
 * @author weizhanjie
 */
public class CommentBlockAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        // 获取编辑器实例
        Editor editor = e.getData(LangDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        // 获取主文本插入符
        CaretModel caretModel = editor.getCaretModel();
        List<Caret> allCarets = caretModel.getAllCarets();
        if (allCarets.isEmpty()) {
            return;
        }

        Caret primaryCaret = caretModel.getPrimaryCaret();

        // 在文本插入符处插入文本
        Document document = editor.getDocument();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
            operateAction.splitChapterContent();

            ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
            if (selectedChapterInfo == null) {
                Messages.showErrorDialog(MessageDialogUtil.TITLE_INFO, "请先选择章节");
                return;
            }

            List<String> chapterContentList = selectedChapterInfo.getChapterContentList();
            if (chapterContentList == null || chapterContentList.isEmpty()) {
                Messages.showErrorDialog(MessageDialogUtil.TITLE_INFO, "章节内容加载失败");
                return;
            }

            StringBuilder text = new StringBuilder();
            for (String content : chapterContentList) {
                text.append("/* ").append(content).append(" */\n");
            }

            document.insertString(primaryCaret.getOffset(), text.toString());
        });
    }
}
