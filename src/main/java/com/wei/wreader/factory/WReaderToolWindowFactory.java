package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.ui.WReaderToolWindow;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.jogamp.common.os.AndroidVersion.isAvailable;

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

        WReaderToolWindow wReaderToolWindow = new WReaderToolWindow(toolWindow);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content wReader = contentFactory.createContent(wReaderToolWindow.getContent(), ConstUtil.WREADER_TOOL_WINDOW_ID, false);
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(wReader);
    }

    /**
     * 创建工具窗口
     * @param project
     */
    public void createToolWindow(@NotNull Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindowManager.registerToolWindow(ConstUtil.WREADER_TOOL_WINDOW_ID, registerToolWindowTaskBuilder -> {
            registerToolWindowTaskBuilder.anchor = ToolWindowAnchor.RIGHT;
            registerToolWindowTaskBuilder.canCloseContent = true;
            registerToolWindowTaskBuilder.icon = IconLoader.getIcon("/icon/mainIcon.svg", WReaderToolWindowFactory.class);
            // 创建工具窗口内容
            registerToolWindowTaskBuilder.contentFactory = this;
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
