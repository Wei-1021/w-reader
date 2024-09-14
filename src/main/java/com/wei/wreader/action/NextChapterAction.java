package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.OperateActionUtil;
import org.jsoup.nodes.Element;

public class NextChapterAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        CacheService cacheService = CacheService.getInstance();
        Settings settings = cacheService.getSettings();
        if (settings == null) {
            return;
        }

        OperateActionUtil operateAction = OperateActionUtil.getInstance();
        Element element = operateAction.nextPageChapter();
        if (element != null) {
            switch (settings.getDisplayType()) {
                case Settings.DISPLAY_TYPE_SIDEBAR:
                    break;
                case Settings.DISPLAY_TYPE_STATUSBAR:
                    break;
                case Settings.DISPLAY_TYPE_TERMINAL:
                    break;
                default:
                    break;
            }
        }
    }
}
