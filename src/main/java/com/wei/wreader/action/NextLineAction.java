package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.widget.ReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

/**
 * 状态栏模式时切换至下一行文字
 *
 * @author weizhanjie
 */
public class NextLineAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        ReaderStatusBarWidget.nextLine(project);
    }
}
