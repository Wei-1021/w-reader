package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.WReaderIcons;
import org.jetbrains.annotations.NotNull;

public class AutoScrollAction extends BaseAction {

    public AutoScrollAction() {
        super();
        getTemplatePresentation().setIcon(WReaderIcons.AUTO_SCROLL_PLAY);
        getTemplatePresentation().setSelectedIcon(WReaderIcons.AUTO_SCROLL_STOP);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        orchestrator.toggleAutoScroll();
        updateIcon(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        CacheService cs = CacheService.getInstance();
        Settings settings = cs != null ? cs.getSettings() : null;
        boolean isSidebar = settings != null
                && settings.getDisplayType() == Settings.DISPLAY_TYPE_SIDEBAR;
        e.getPresentation().setEnabledAndVisible(isSidebar);

        if (isSidebar) {
            boolean isScrolling = orchestrator != null && orchestrator.getAutoScrollController().isAutoScrolling();
            e.getPresentation().setIcon(isScrolling ? WReaderIcons.AUTO_SCROLL_STOP : WReaderIcons.AUTO_SCROLL_PLAY);
        }
    }

    private void updateIcon(AnActionEvent e) {
        if (orchestrator != null) {
            boolean isScrolling = orchestrator.getAutoScrollController().isAutoScrolling();
            e.getPresentation().setIcon(isScrolling ? WReaderIcons.AUTO_SCROLL_STOP : WReaderIcons.AUTO_SCROLL_PLAY);
        }
    }
}