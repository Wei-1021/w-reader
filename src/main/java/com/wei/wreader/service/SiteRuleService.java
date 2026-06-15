package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.wei.wreader.model.SiteBean;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Service(Service.Level.APP)
@State(name = "CustomSiteRuleCacheServer", storages = {@Storage("w-reader-custom-site-rule.xml")})
public final class SiteRuleService implements PersistentStateComponent<SiteRuleService> {
    private Map<String, List<SiteBean>> customSiteRuleGroupMap;
    private Map<String, String> customSiteRuleOriginalStrMap;
    private String selectedCustomSiteRuleKey;
    private String tempSelectedCustomSiteRuleKey;

    public Map<String, List<SiteBean>> getCustomSiteRuleGroupMap() { return customSiteRuleGroupMap; }
    public void setCustomSiteRuleGroupMap(Map<String, List<SiteBean>> customSiteRuleGroupMap) { this.customSiteRuleGroupMap = customSiteRuleGroupMap; }

    public Map<String, String> getCustomSiteRuleOriginalStrMap() { return customSiteRuleOriginalStrMap; }
    public void setCustomSiteRuleOriginalStrMap(Map<String, String> customSiteRuleOriginalStrMap) { this.customSiteRuleOriginalStrMap = customSiteRuleOriginalStrMap; }

    public String getSelectedCustomSiteRuleKey() { return selectedCustomSiteRuleKey; }
    public void setSelectedCustomSiteRuleKey(String selectedCustomSiteRuleKey) { this.selectedCustomSiteRuleKey = selectedCustomSiteRuleKey; }

    public String getTempSelectedCustomSiteRuleKey() { return tempSelectedCustomSiteRuleKey; }
    public void setTempSelectedCustomSiteRuleKey(String tempSelectedCustomSiteRuleKey) { this.tempSelectedCustomSiteRuleKey = tempSelectedCustomSiteRuleKey; }

    public static SiteRuleService getInstance() {
        return ApplicationManager.getApplication().getService(SiteRuleService.class);
    }

    @Override
    public @NotNull SiteRuleService getState() { return this; }

    @Override
    public void loadState(@NotNull SiteRuleService state) { XmlSerializerUtil.copyBean(state, this); }
}
