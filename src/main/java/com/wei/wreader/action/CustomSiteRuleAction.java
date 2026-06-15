package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.ui.CustomSiteRuleDialogNew;
import org.jetbrains.annotations.NotNull;

/**
 * 自定义书源规则Action
 *
 * @author weizhanjie
 */
public class CustomSiteRuleAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        super.actionPerformed(anActionEvent);

        CustomSiteRuleDialogNew dialog = new CustomSiteRuleDialogNew(project, settings);
        dialog.show();
    }
}
