package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NotNull;

/**
 * 隐藏/显示文字
 *
 * @author weizhanjie
 */
public class HideOrShowTextAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        boolean isHideText = cacheService.isHideText();
        boolean newHideText = !isHideText;
        cacheService.setHideText(newHideText);
        if (newHideText) {
            WReaderStatusBarWidget.hideText(project);
        } else {
            WReaderStatusBarWidget.showText(project);
        }
    }
}
