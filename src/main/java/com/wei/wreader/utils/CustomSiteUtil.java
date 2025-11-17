package com.wei.wreader.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.wei.wreader.pojo.SiteBean;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.CustomSiteRuleCacheServer;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.data.JsonValidator;
import com.wei.wreader.utils.file.FileUtil;
import com.wei.wreader.utils.yml.ConfigYaml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 自定义书源工具类
 * @author weizhanjie
 */
public class CustomSiteUtil {
    //region 属性参数
    private static Project mProject;
    private static CustomSiteUtil instance;
    /** 配置文件 */
    private final ConfigYaml configYaml;
    /** 缓存服务 */
    private final CacheService cacheService;
    /** 自定义书源规则缓存服务 */
    private final CustomSiteRuleCacheServer customSiteRuleCacheServer;
    /** 默认书源规则文件路径 */
    private static final String DEFAULT_SITE_RULE_PATH = "json/default-site-rule.json";
    //endregion

    public static CustomSiteUtil getInstance(Project project) {
        if (instance == null) {
            instance = new CustomSiteUtil();
        }
        mProject = project;
        return instance;
    }

    public CustomSiteUtil() {
        configYaml = ConfigYaml.getInstance();
        cacheService = CacheService.getInstance();
        customSiteRuleCacheServer = CustomSiteRuleCacheServer.getInstance();
    }

    /**
     * 获取书源映射
     * @return
     */
    public Map<String, List<SiteBean>> getSiteMap() {
        Map<String, List<SiteBean>> siteMap = customSiteRuleCacheServer.getCustomSiteRuleGroupMap();
        if (siteMap == null) {
            siteMap = new HashMap<>();
        }

        if (siteMap.isEmpty()) {
            siteMap.put(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY, configYaml.getSiteList());
            customSiteRuleCacheServer.setSelectedCustomSiteRuleKey(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY);
            customSiteRuleCacheServer.setCustomSiteRuleGroupMap(siteMap);

            // 原始JSON字符串
            Map<String, String> customSiteRuleOriginalStrMap = customSiteRuleCacheServer.getCustomSiteRuleOriginalStrMap();
            if (customSiteRuleOriginalStrMap == null) {
                customSiteRuleOriginalStrMap = new HashMap<>();
            }
            String defaultSiteRuleJson = FileUtil.readResourcesJsonStr(DEFAULT_SITE_RULE_PATH);
            customSiteRuleOriginalStrMap.put(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY, defaultSiteRuleJson);
            customSiteRuleCacheServer.setCustomSiteRuleOriginalStrMap(customSiteRuleOriginalStrMap);
        }
        return siteMap;
    }

    /**
     * 解析自定义书源规则字符串
     * @param jsonStr 自定义书源规则字符串
     * @param successCallback 解析成功回调。
     *                        Parameter: JsonValidator.ValidationResult
     * @param failCallback 解析失败回调。
     *                     Parameter: JsonValidator.ValidationResult
     */
    public void parseCustomSiteRule(String jsonStr,
                                    Consumer<JsonValidator.ValidationResult> successCallback,
                                    Consumer<JsonValidator.ValidationResult> failCallback) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            Messages.showErrorDialog(ConstUtil.WREADER_DIY_SITE_JSON_NULL_ERROR, "错误");
            return;
        }
        // 判断是否符合
        if (!jsonStr.startsWith("[")) {
            Messages.showErrorDialog(ConstUtil.WREADER_DIY_SITE_JSON_ERROR + "：“[”符号缺失", "错误");
            return;
        }

        // 判断是否符合
        if (!jsonStr.endsWith("]")) {
            Messages.showErrorDialog(ConstUtil.WREADER_DIY_SITE_JSON_ERROR + "：“]”符号缺失", "错误");
            return;
        }

        // 调用校验规则
        JsonValidator.ValidationResult validationResult = JsonValidator.validateList(jsonStr, SiteBean.class);
        if (!validationResult.isValid()) {
            StringBuilder errorMsg = new StringBuilder("规则校验失败：\n");
            List<JsonValidator.ErrorDetail> errors = validationResult.getErrors();
            for (JsonValidator.ErrorDetail error : errors) {
                errorMsg.append(error.toString()).append("\n");
            }
            Messages.showErrorDialog(errorMsg.toString(), "错误");
            if (failCallback != null) {
                failCallback.accept(validationResult);
            }
            return;
        }

        if (successCallback != null) {
            successCallback.accept(validationResult);
        }
    }

    /**
     * 获取自定义书源规则分组名称列表
     * @return
     */
    public List<String> getCustomSiteKeyGroupList() {
        Map<String, List<SiteBean>> customSiteRuleGroupMap = customSiteRuleCacheServer.getCustomSiteRuleGroupMap();
        if (customSiteRuleGroupMap == null || customSiteRuleGroupMap.isEmpty()) {
            customSiteRuleGroupMap = getSiteMap();
        }
        return customSiteRuleGroupMap.keySet().stream().toList();
    }

}
