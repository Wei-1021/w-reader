package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.MessageDialogUtil;
import com.wei.wreader.utils.OperateActionUtil;

import java.io.IOException;

/**
 * 有声阅读
 *
 * @author weizhanjie
 */
public class AudioReadingAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);
        try {
            OperateActionUtil.getInstance(project).ttsChapterContent();
        } catch (IOException ex) {
            Messages.showErrorDialog(ConstUtil.WREADER_AUDIO_READING_ERROR, MessageDialogUtil.TITLE_ERROR);
            throw new RuntimeException(ex);
        }
    }
}
