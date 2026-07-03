package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.ui.AiGenerateSiteRuleDialog;
import org.jetbrains.annotations.NotNull;

/**
 * AI生成书源规则Action
 */
public class AiGenerateSiteRuleAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        super.actionPerformed(anActionEvent);
        new AiGenerateSiteRuleDialog(project).show();
    }
}
