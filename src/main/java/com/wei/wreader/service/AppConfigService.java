package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.wei.wreader.model.ConfigYamlPojo;
import com.wei.wreader.model.Settings;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.util.yml.YamlReader;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service(Service.Level.APP)
public final class AppConfigService {
    private static final Logger LOG = Logger.getInstance(AppConfigService.class);

    private final ConfigYamlPojo config;
    private final Map<String, Object> rawData;

    public AppConfigService() {
        Map<String, Object> loaded = null;
        ConfigYamlPojo loadedConfig = null;
        try {
            Yaml yaml = new Yaml();
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (inputStream != null) {
                    loaded = yaml.load(inputStream);
                    if (loaded != null) {
                        loadedConfig = YamlReader.convertMapToPojo(loaded, ConfigYamlPojo.class);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to load config.yml, using defaults", e);
        }
        this.rawData = loaded != null ? loaded : Map.of();
        this.config = loadedConfig != null ? loadedConfig : new ConfigYamlPojo();
    }

    public static AppConfigService getInstance() {
        return ApplicationManager.getApplication().getService(AppConfigService.class);
    }

    public ConfigYamlPojo getConfig() { return config; }
    public Map<String, Object> getRawData() { return rawData; }

    public Settings getSettings() {
        ConfigYamlPojo.Wreader wreader = config.getWreader();
        return wreader != null ? wreader.getSettings() : new Settings();
    }

    public List<SiteBean> getSiteList() {
        ConfigYamlPojo.Wreader wreader = config.getWreader();
        return wreader != null ? wreader.getSiteList() : List.of();
    }

    public List<String> getAllowFileExtension() {
        ConfigYamlPojo.Wreader wreader = config.getWreader();
        return wreader != null ? wreader.getAllowFileExtension() : List.of("txt", "epub");
    }

    public Map<String, Object> getLanguage() {
        ConfigYamlPojo.Wreader wreader = config.getWreader();
        return wreader != null ? wreader.getLanguage() : Map.of();
    }

    public String getVersion() {
        ConfigYamlPojo.Wreader wreader = config.getWreader();
        return wreader != null ? wreader.getVersion() : "0.0.0";
    }
}
