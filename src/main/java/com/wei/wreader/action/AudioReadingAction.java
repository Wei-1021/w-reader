package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.ui.MessageDialogUtil;
import com.wei.wreader.utils.OperateActionUtil;


/**
 * 听书
 *
 * @author weizhanjie
 */
public class AudioReadingAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);
        try {
            OperateActionUtil.getInstance(project).ttsChapterContent();
        } catch (Exception ex) {
            Messages.showErrorDialog(ConstUtil.WREADER_AUDIO_READING_ERROR, MessageDialogUtil.TITLE_ERROR);
            throw new RuntimeException(ex);
        }
    }
}
