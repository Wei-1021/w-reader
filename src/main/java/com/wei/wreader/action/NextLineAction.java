package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.widget.WReaderStatusBarWidget;

/**
 * 状态栏模式时切换至下一行文字
 *
 * @author weizhanjie
 */
public class NextLineAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        WReaderStatusBarWidget.nextLine(project);
    }
}
