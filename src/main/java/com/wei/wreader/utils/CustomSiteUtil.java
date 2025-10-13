package com.wei.wreader.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.wei.wreader.pojo.SiteBean;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.CustomSiteRuleCacheServer;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.data.JsonValidator;
import com.wei.wreader.utils.yml.ConfigYaml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /**  书源映射 */
    private final Map<String, List<SiteBean>> siteMap = new HashMap<>();
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
        if (siteMap.isEmpty()) {
            siteMap.put(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY, configYaml.getSiteList());
            customSiteRuleCacheServer.setSelectedCustomSiteRuleKey(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY);
        }
        customSiteRuleCacheServer.setCustomSiteRuleGroupMap(customSiteRuleCacheServer.getCustomSiteRuleGroupMap());
        return siteMap;
    }

    /**
     * 解析自定义书源规则字符串
     */
    public void parseCustomSiteRule(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            Messages.showErrorDialog(ConstUtil.WREADER_DIY_SITE_JSON_ERROR, "错误");
            return;
        }
        // 判断是否符合
        if (!jsonStr.startsWith("[") || !jsonStr.endsWith("]")) {
            Messages.showErrorDialog(ConstUtil.WREADER_DIY_SITE_JSON_ERROR, "错误");
            return;
        }

        // 调用校验规则
        JsonValidator.ValidationResult validationResult = JsonValidator.validateList(jsonStr, SiteBean.class);
        System.out.println(validationResult);
        if (!validationResult.isValid()) {
            StringBuilder errorMsg = new StringBuilder("规则校验失败：\n");
            List<JsonValidator.ErrorDetail> errors = validationResult.getErrors();
            for (JsonValidator.ErrorDetail error : errors) {
                errorMsg.append(error.toString()).append("\n");
            }
            Messages.showErrorDialog(errorMsg.toString(), "错误");
            return;
        }

        List<SiteBean> siteBeans = validationResult.getBeanList();
        for (SiteBean siteBean : siteBeans) {
            System.out.println(siteBean);
        }

    }

}
