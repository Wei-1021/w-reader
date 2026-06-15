package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.reader.ReaderOrchestrator;
import com.wei.wreader.util.yml.ConfigYaml;
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
    protected ReaderOrchestrator orchestrator;
    protected boolean isInitOrchestrator = true;
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        project = anActionEvent.getProject();
        if (project == null) {
            Messages.showWarningDialog("无法获取当前项目，请确保已打开项目", "W-Reader");
            return;
        }

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

        if (isInitOrchestrator) {
            orchestrator = ReaderOrchestrator.getInstance(project);
        }

    }
}
