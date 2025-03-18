package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 启动工厂
 *
 * @author weizhanjie
 */
public class StartupFactory implements StartupActivity {
    private CacheService cacheService;
    private ConfigYaml configYaml;
    private Settings settings;

    @Override
    public void runActivity(@NotNull Project project) {
        SwingUtilities.invokeLater(() -> {
            cacheService = CacheService.getInstance();
            settings = cacheService.getSettings();
            configYaml = ConfigYaml.getInstance();
            if (settings == null) {
                settings = configYaml.getSettings();
            }

            WReaderToolWindowFactory wReaderToolWindowFactory = new WReaderToolWindowFactory();
            wReaderToolWindowFactory.setEnabled(project);
            WReaderStatusBarFactory wReaderStatusBarFactory = new WReaderStatusBarFactory();
            wReaderStatusBarFactory.setEnabled(project, true);

        });
    }
}
