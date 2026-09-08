package com.wei.wreader.agent.v2.siterule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.agent.v1.SiteRuleTools;
import com.intellij.openapi.project.Project;

import java.time.Duration;

/**
 * 在网站搜索书籍工具
 *
 * @author weizhanjie
 */
public final class SearchWebsiteTool implements Tool {

    @Override
    public String getName() {
        return "search_website";
    }

    @Override
    public String getDescription() {
        return "在小说网站搜索书籍。返回搜索结果列表，包含书名和链接。";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode keywordProp = properties.putObject("keyword");
        keywordProp.put("type", "string");
        keywordProp.put("description", "搜索关键词，如书名");

        schema.putArray("required").add("keyword");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) throws Exception {
        String keyword = arguments.path("keyword").asText();

        if (keyword == null || keyword.isEmpty()) {
            return ToolResult.failure("搜索关键词不能为空");
        }

        context.throwIfCancelled();

        try {
            SiteRuleTools tools = context.getAgentContext().get("siteRuleTools");
            if (tools == null) {
                return ToolResult.failure("请先调用 fetch_page 获取目标网站首页");
            }

            // 使用现有的工具方法
            JsonNode args = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                    .put("keyword", keyword);
            String result = tools.executeTool("search_website", args);

            if (result == null || result.isEmpty() || result.startsWith("错误:")) {
                return ToolResult.failure("搜索失败，请检查网站是否支持搜索");
            }

            // 截断过长的内容
            if (result.length() > 30000) {
                result = result.substring(0, 30000) + "\n... (内容已截断)";
            }

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure("搜索失败: " + e.getMessage());
        }
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(30);
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.NETWORK;
    }
}
