package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.wei.wreader.ui.WReaderToolWindow;
import org.jetbrains.annotations.NotNull;

/**
 * 工具窗口工厂
 * @author weizhanjie
 */
public class WReaderToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        WReaderToolWindow wReaderToolWindow = new WReaderToolWindow(toolWindow);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content wReader = contentFactory.createContent(wReaderToolWindow.getContent(), "WReader", false);
        toolWindow.getContentManager().addContent(wReader);
    }
}
