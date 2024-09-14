package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.OperateActionUtil;

public class ChapterListAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        OperateActionUtil operateAction = OperateActionUtil.getInstance();
        operateAction.showBookDirectory();
    }
}
