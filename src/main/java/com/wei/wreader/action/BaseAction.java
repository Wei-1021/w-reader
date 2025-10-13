package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.yml.ConfigYaml;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public abstract class BaseAction extends AnAction {

    /**
     * 信息缓存服务
     */
    protected CacheService cacheService;
    /**
     * 配置文件信息
     */
    protected ConfigYaml configYaml;
    /**
     * 设置信息
     */
    protected Settings settings;
    protected Project project;
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        cacheService = CacheService.getInstance();
        configYaml = ConfigYaml.getInstance();
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
            cacheService.setSettings(settings);
        }

        if (StringUtils.isBlank(settings.getCharset())) {
            settings.setCharset(configYaml.getSettings().getCharset());
            cacheService.setSettings(settings);
        }

        if (StringUtils.isBlank(settings.getVoiceRole())) {
            settings.setVoiceRole(configYaml.getSettings().getVoiceRole());
            cacheService.setSettings(settings);
        }

        if (StringUtils.isBlank(settings.getAudioStyle())) {
            settings.setAudioStyle(configYaml.getSettings().getAudioStyle());
            cacheService.setSettings(settings);
        }

        if (settings.getVolume() == null) {
            settings.setVolume(configYaml.getSettings().getVolume());
            cacheService.setSettings(settings);
        }


        project = anActionEvent.getProject();


    }
}
