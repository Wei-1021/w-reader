package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.MessageUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.MessageDialogUtil;
import com.wei.wreader.utils.OperateActionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * 代码注释块
 *
 * @author weizhanjie
 */
public class CommentBlockAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = fileEditorManager.getSelectedEditor();
        if (selectedEditor == null) {
            return;
        }

        // 获取编辑器实例
//        Editor editor = e.getData(LangDataKeys.EDITOR);
        Editor editor = fileEditorManager.getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        Document document = editor.getDocument();

        // 获取文本插入符
        CaretModel caretModel = editor.getCaretModel();
        List<Caret> allCarets = caretModel.getAllCarets();
        if (allCarets.isEmpty()) {
            return;
        }

        // 获取文件名称
        VirtualFile file = selectedEditor.getFile();
        String editorFileName = file.getName();
        // 获取文件类型
        String editorFileExtension = editorFileName.substring(editorFileName.lastIndexOf(".") + 1);
        // 代码注释符号
        String commentStartSymbol = "/*";
        String commentEndSymbol = "*/";
        String commentLineSymbol = "*";
        Map<String, Object> languageMap = ConfigYaml.getInstance().getLanguage();
        for (String language : languageMap.keySet()) {
            if (editorFileExtension.equals(language)) {
                Map<String, Object> languageObj = (Map<String, Object>) languageMap.get(language);
                commentStartSymbol = languageObj.get("commentStart").toString();
                commentEndSymbol = languageObj.get("commentEnd").toString();
                commentLineSymbol = languageObj.get("commentLine").toString();
                break;
            }
        }

        // 获取主文本插入符
        Caret primaryCaret = caretModel.getPrimaryCaret();

        // 在文本插入符处插入文本
        String finalCommentStartSymbol = commentStartSymbol;
        String finalCommentEndSymbol = commentEndSymbol;
        String finalCommentLineSymbol = commentLineSymbol;
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

            StringBuilder text = new StringBuilder(finalCommentStartSymbol);
            text.append("\n");
            // 计算并设置每行缩进的空格数，于第一行对应
            StringBuilder linePrefix = new StringBuilder();
            int column = primaryCaret.getVisualPosition().column;
            if (column > 0) {
                linePrefix.append(" ".repeat(column));
            }
            for (String content : chapterContentList) {
                text.append(linePrefix)
                        .append(finalCommentLineSymbol)
                        .append(" ")
                        .append(content)
                        .append("\n");
            }
            text.append(linePrefix).append(finalCommentEndSymbol);

            document.insertString(primaryCaret.getOffset(), text.toString());
        });
    }
}

