package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.pojo.SiteBean;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.file.CacheOldToNewConvert;
import com.wei.wreader.utils.yml.ConfigYaml;
import org.apache.commons.lang3.StringUtils;
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

            // 从0.1.0开始，书源站点的配置发生了大的变化，为了兼容旧版配置，这里将缓存信息进行兼容性转换
            // 将旧版本缓存数据转换成新版本缓存数据
            SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
            if (selectedSiteBean == null || StringUtils.isBlank(selectedSiteBean.getId())) {
                CacheOldToNewConvert cacheOldToNewConvert = new CacheOldToNewConvert();
                cacheOldToNewConvert.convertBookSiteInfo();
            }

            WReaderToolWindowFactory wReaderToolWindowFactory = new WReaderToolWindowFactory();
            wReaderToolWindowFactory.setEnabled(project);
            WReaderStatusBarFactory wReaderStatusBarFactory = new WReaderStatusBarFactory();
            wReaderStatusBarFactory.setEnabled(project, true);

        });
    }
}
