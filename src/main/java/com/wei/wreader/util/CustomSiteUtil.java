package com.wei.wreader.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.project.Project;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.JsonValidator;
import com.wei.wreader.util.file.FileUtil;
import com.wei.wreader.util.yml.ConfigYaml;
import org.apache.commons.lang3.StringUtils;

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
    private Project mProject;
    /** 配置文件 */
    private final ConfigYaml configYaml;
    /** 缓存服务 */
    private final CacheService cacheService;
    /** 自定义书源规则缓存服务 */
    private final SiteRuleService siteRuleService;
    /** 默认书源规则文件路径 */
    public static final String DEFAULT_SITE_RULE_PATH = "json/default-site-rule.json";
    //endregion

    public static CustomSiteUtil getInstance(Project project) {
        CustomSiteUtil util = new CustomSiteUtil();
        util.mProject = project;
        return util;
    }

    public CustomSiteUtil() {
        configYaml = ConfigYaml.getInstance();
        cacheService = CacheService.getInstance();
        siteRuleService = SiteRuleService.getInstance();
    }

    /**
     * 获取书源映射
     * @return
     */
    public Map<String, List<SiteBean>> getSiteMap() {
        Map<String, List<SiteBean>> siteMap = siteRuleService.getCustomSiteRuleGroupMap();
        if (siteMap == null) {
            siteMap = new HashMap<>();
        }

        if (siteMap.isEmpty()) {
            siteMap.put(
                    ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY,
                    FileUtil.readResourcesJsonList(
                            CustomSiteUtil.DEFAULT_SITE_RULE_PATH,
                            SiteBean.class
                    )
            );
            siteRuleService.setSelectedCustomSiteRuleKey(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY);
            siteRuleService.setCustomSiteRuleGroupMap(siteMap);

            // 原始JSON字符串
            Map<String, String> customSiteRuleOriginalStrMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
            if (customSiteRuleOriginalStrMap == null) {
                customSiteRuleOriginalStrMap = new HashMap<>();
            }
            String defaultSiteRuleJson = FileUtil.readResourcesJsonStr(DEFAULT_SITE_RULE_PATH);
            customSiteRuleOriginalStrMap.put(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY, defaultSiteRuleJson);
            siteRuleService.setCustomSiteRuleOriginalStrMap(customSiteRuleOriginalStrMap);
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
            throw new IllegalArgumentException(ConstUtil.WREADER_DIY_SITE_JSON_NULL_ERROR);
        }

        // 去除首尾空格，包括 \r \n \t
        jsonStr = jsonStr.trim();

        // 判断是否符合
        if (!jsonStr.startsWith("[")) {
            throw new IllegalArgumentException(ConstUtil.WREADER_DIY_SITE_JSON_ERROR + "：“[”符号缺失");
        }

        // 判断是否符合
        if (!jsonStr.endsWith("]")) {
            throw new IllegalArgumentException(ConstUtil.WREADER_DIY_SITE_JSON_ERROR + "：“]”符号缺失");
        }

        // 调用校验规则
        JsonValidator.ValidationResult validationResult = JsonValidator.validateList(jsonStr, SiteBean.class);
        if (!validationResult.isValid()) {
            StringBuilder errorMsg = new StringBuilder("规则校验失败：\n");
            List<JsonValidator.ErrorDetail> errors = validationResult.getErrors();
            for (JsonValidator.ErrorDetail error : errors) {
                errorMsg.append(error.toString()).append("\n");
            }
            throw new IllegalArgumentException(errorMsg.toString());
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
        Map<String, List<SiteBean>> customSiteRuleGroupMap = siteRuleService.getCustomSiteRuleGroupMap();
        if (customSiteRuleGroupMap == null || customSiteRuleGroupMap.isEmpty()) {
            customSiteRuleGroupMap = getSiteMap();
        }
        return customSiteRuleGroupMap.keySet().stream().toList();
    }

}
