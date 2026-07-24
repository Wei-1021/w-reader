package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.ui.AgentSiteRuleDialog;
import org.jetbrains.annotations.NotNull;

/**
 * AI Agent 生成书源规则 Action
 *
 * @author weizhanjie
 */
public class AiGenerateSiteRuleAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        super.actionPerformed(anActionEvent);
        new AgentSiteRuleDialog(project).show();
    }
}
