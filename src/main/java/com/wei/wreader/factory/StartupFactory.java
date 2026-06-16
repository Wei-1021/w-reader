package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.wei.wreader.model.Settings;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.ui.CustomSiteRuleDialogNew;
import com.wei.wreader.util.file.CacheMigrationUtil;
import com.wei.wreader.util.yml.ConfigYaml;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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

            // 从0.1.0开始，书源站点的配置发生了大的变化，为了兼容旧版配置，这里将缓存信息进行兼容性转换
            // 将旧版本缓存数据转换成新版本缓存数据
            SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
            if (selectedSiteBean == null || StringUtils.isBlank(selectedSiteBean.getId())) {
                CacheMigrationUtil cacheMigrationUtil = new CacheMigrationUtil();
                cacheMigrationUtil.convertBookSiteInfo();
            }

            // 启动时检查自定义书源规则临时文件是否存在，如果不存在则创建
            getOrCreateTempVirtualFile(project);

            WReaderToolWindowFactory wReaderToolWindowFactory = new WReaderToolWindowFactory();
            wReaderToolWindowFactory.setEnabled(project);
            WReaderStatusBarFactory wReaderStatusBarFactory = new WReaderStatusBarFactory();
            wReaderStatusBarFactory.setEnabled(project, true);

        });
    }
    
    private void getOrCreateTempVirtualFile(Project project) {
        String tempDirPath = project.getBasePath() + "/" + CustomSiteRuleDialogNew.TEMP_DIR;
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, CustomSiteRuleDialogNew.TEMP_FILE_NAME);
        // 仅文件不存在时写入初始内容，已有文件则复用
        if (!tempFile.exists()) {
            try {
                Files.writeString(tempFile.toPath(), "", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
