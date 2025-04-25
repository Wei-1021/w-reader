package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.ui.WReaderToolWindow;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.WReaderIcons;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * 工具窗口工厂
 * @author weizhanjie
 */
public class WReaderToolWindowFactory implements ToolWindowFactory {
    private CacheService cacheService;
    private ConfigYaml configYaml;
    private Settings settings;

    public WReaderToolWindowFactory() {

    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        configYaml = ConfigYaml.getInstance();
        if (settings == null) {
            settings = configYaml.getSettings();
        }

        ContentFactory contentFactory = ContentFactory.getInstance();
        WReaderToolWindow wReaderToolWindow = new WReaderToolWindow(toolWindow);
        Content wReader = contentFactory.createContent(wReaderToolWindow.getContent(), ConstUtil.WREADER_TOOL_WINDOW_ID, false);
        toolWindow.getContentManager().addContent(wReader);
    }

    /**
     * 创建工具窗口
     * @param project
     */
    public void createToolWindow(@NotNull Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(ConstUtil.WREADER_TOOL_WINDOW_ID);
        if (toolWindow != null) {
            return;
        }

        toolWindowManager.registerToolWindow(ConstUtil.WREADER_TOOL_WINDOW_ID, registerToolWindowTaskBuilder -> {
            registerToolWindowTaskBuilder.anchor = ToolWindowAnchor.RIGHT;
            registerToolWindowTaskBuilder.canCloseContent = false;
            registerToolWindowTaskBuilder.icon = WReaderIcons.getMainIcon(project);
            // 创建工具窗口内容
            registerToolWindowTaskBuilder.contentFactory = WReaderToolWindowFactory.this;
            return Unit.INSTANCE;
        });
    }

    /**
     * 设置工具窗口是否显示
     * @param project
     */
    public void setEnabled(@NotNull Project project) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        configYaml = ConfigYaml.getInstance();
        if (settings == null) {
            settings = configYaml.getSettings();
        }

        boolean isShow = settings.getDisplayType() == Settings.DISPLAY_TYPE_SIDEBAR;
        if (isShow) {
            // 创建工具窗口
            createToolWindow(project);
        } else {
            // 移除工具窗口
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow(ConstUtil.WREADER_TOOL_WINDOW_ID);
            if (toolWindow != null) {
                toolWindow.setAvailable(false, null);
                toolWindow.remove();
            }
        }
    }


}
