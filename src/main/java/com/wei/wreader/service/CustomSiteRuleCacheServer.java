package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.wei.wreader.pojo.SiteBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 自定义书源规则缓存服务
 */
@Service(Service.Level.APP)
@State(name = "CustomSiteRuleCacheServer", storages = {@Storage("w-reader-custom-site-rule.xml")})
public final class CustomSiteRuleCacheServer implements PersistentStateComponent<CustomSiteRuleCacheServer> {
    /**
     * 自定义书源规则缓存
     */
    private Map<String, List<SiteBean>> customSiteRuleGroupMap;
    /**
     * 自定义书源规则原始字符串缓存
     */
    private Map<String, String> customSiteRuleOriginalStrMap;

    /**
     * 已选择自定义书源规则的Key
     */
    private String selectedCustomSiteRuleKey;

    /**
     * 临时选择的自定义书源规则的Key--搜索时使用
     */
    private String tempSelectedCustomSiteRuleKey;

    public Map<String, List<SiteBean>> getCustomSiteRuleGroupMap() {
        return customSiteRuleGroupMap;
    }

    public void setCustomSiteRuleGroupMap(Map<String, List<SiteBean>> customSiteRuleGroupMap) {
        this.customSiteRuleGroupMap = customSiteRuleGroupMap;
    }

    public Map<String, String> getCustomSiteRuleOriginalStrMap() {
        return customSiteRuleOriginalStrMap;
    }

    public void setCustomSiteRuleOriginalStrMap(Map<String, String> customSiteRuleOriginalStrMap) {
        this.customSiteRuleOriginalStrMap = customSiteRuleOriginalStrMap;
    }

    public String getSelectedCustomSiteRuleKey() {
        return selectedCustomSiteRuleKey;
    }

    public void setSelectedCustomSiteRuleKey(String selectedCustomSiteRuleKey) {
        this.selectedCustomSiteRuleKey = selectedCustomSiteRuleKey;
    }

    public String getTempSelectedCustomSiteRuleKey() {
        return tempSelectedCustomSiteRuleKey;
    }

    public void setTempSelectedCustomSiteRuleKey(String tempSelectedCustomSiteRuleKey) {
        this.tempSelectedCustomSiteRuleKey = tempSelectedCustomSiteRuleKey;
    }

    private static CustomSiteRuleCacheServer instance;

    public static CustomSiteRuleCacheServer getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(CustomSiteRuleCacheServer.class);
        }
        return instance;
    }

    @Override
    public @Nullable CustomSiteRuleCacheServer getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CustomSiteRuleCacheServer customSiteRuleCacheServer) {
        XmlSerializerUtil.copyBean(customSiteRuleCacheServer, this);
    }
}
