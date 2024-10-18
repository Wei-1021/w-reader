package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.utils.OperateActionUtil;

/**
 * 自动阅读
 * @author weizhanjie
 */
public class AutoReadAction extends BaseAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        OperateActionUtil.getInstance(project).autoReadNextLine();
    }
}
