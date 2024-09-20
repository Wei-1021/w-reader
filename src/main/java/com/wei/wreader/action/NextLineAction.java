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
public class NextLineAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        System.out.println("NextLineAction");
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        CacheService cacheService = CacheService.getInstance();
        Settings settings = cacheService.getSettings();

        if (settings == null) {
            return;
        }

        WReaderStatusBarWidget.nextLine(project);
    }
}
