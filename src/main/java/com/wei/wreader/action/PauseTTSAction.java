package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.util.WReaderIcons;
import org.jetbrains.annotations.NotNull;

/**
 * 暂停/恢复听书
 *
 * @author weizhanjie
 */
public class PauseTTSAction extends BaseAction {
    public PauseTTSAction() {
        super();
        getTemplatePresentation().setIcon(WReaderIcons.AUDIO_PAUSE);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
        if (orchestrator == null) return;

        if (orchestrator.isTtsPaused()) {
            orchestrator.resumeTTS();
        } else if (orchestrator.isTtsPlaying()) {
            orchestrator.pauseTTS();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        if (orchestrator != null) {
            boolean isPlaying = orchestrator.isTtsPlaying();
            boolean isPaused = orchestrator.isTtsPaused();
            boolean visible = isPlaying || isPaused;
            e.getPresentation().setEnabledAndVisible(visible);

            if (visible) {
                e.getPresentation().setIcon(isPaused ? WReaderIcons.AUDIO_RESUME : WReaderIcons.AUDIO_PAUSE);
                e.getPresentation().setText(isPaused ? "恢复听书" : "暂停听书");
            }
        }
    }
}
