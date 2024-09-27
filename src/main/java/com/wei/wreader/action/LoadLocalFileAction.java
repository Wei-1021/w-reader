package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.OperateActionUtil;

/**
 * 加载本地文件
 * @author weizhanjie
 */
public class LoadLocalFileAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        Project project = e.getProject();
        if (project == null) {
            return;
        }

        OperateActionUtil.getInstance().loadLocalFile();
    }
}
