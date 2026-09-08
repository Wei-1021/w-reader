package com.wei.wreader.agent.v2.siterule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.agent.v1.SiteRuleTools;
import com.intellij.openapi.project.Project;

import java.time.Duration;

/**
 * 提取 HTML 元素工具
 *
 * @author weizhanjie
 */
public final class ExtractHtmlTool implements Tool {

    @Override
    public String getName() {
        return "extract_html_elements";
    }

    @Override
    public String getDescription() {
        return "使用 CSS 选择器从网页中提取 HTML 元素。可以提取元素的文本、href 属性或 HTML 内容。";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode htmlProp = properties.putObject("html");
        htmlProp.put("type", "string");
        htmlProp.put("description", "从 fetch_page 或 search_website 结果中取得的 HTML 内容");

        ObjectNode selectorProp = properties.putObject("css_selector");
        selectorProp.put("type", "string");
        selectorProp.put("description", "CSS 选择器，如 '.book-list a'");

        ObjectNode maxResultsProp = properties.putObject("max_results");
        maxResultsProp.put("type", "integer");
        maxResultsProp.put("description", "最大返回数量，默认为 20");

        schema.putArray("required").add("html").add("css_selector");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) throws Exception {
        String html = extractPagePayload(arguments.path("html").asText());
        String cssSelector = arguments.path("css_selector").asText();
        int maxResults = arguments.path("max_results").asInt(20);

        if (html == null || html.isEmpty()) {
            return ToolResult.failure("HTML 内容不能为空");
        }

        if (cssSelector == null || cssSelector.isEmpty()) {
            return ToolResult.failure("CSS 选择器不能为空");
        }

        context.throwIfCancelled();

        try {
            Project project = context.getProject();
            SiteRuleTools tools = new SiteRuleTools(project);

            // 使用现有的工具方法
            JsonNode args = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                    .put("html", html)
                    .put("css_selector", cssSelector)
                    .put("max_results", Math.max(1, Math.min(maxResults, 50)));
            String result = tools.executeTool("extract_html_elements", args);

            if (result == null || result.isEmpty() || result.startsWith("错误:")) {
                return ToolResult.failure("未找到匹配的元素");
            }

            // 截断过长的内容
            if (result.length() > 30000) {
                result = result.substring(0, 30000) + "\n... (内容已截断)";
            }

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure("提取元素失败: " + e.getMessage());
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

    private String extractPagePayload(String content) {
        if (content == null) {
            return "";
        }
        for (String marker : new String[]{"--- 页面内容 ---", "--- 搜索结果 ---", "--- 原始内容 ---"}) {
            int index = content.indexOf(marker);
            if (index >= 0) {
                return content.substring(index + marker.length()).trim();
            }
        }
        return content;
    }
}
