package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

/**
 * 状态栏模式时切换至上一行文字
 * @author weizhanjie
 */
public class PrevLineAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        WReaderStatusBarWidget.prevLine(project);
    }
}
