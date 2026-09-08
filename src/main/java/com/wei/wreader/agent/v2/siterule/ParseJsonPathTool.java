package com.wei.wreader.agent.v2.siterule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.agent.v1.SiteRuleTools;
import com.intellij.openapi.project.Project;

import java.time.Duration;

/**
 * 解析 JSON Path 工具
 *
 * @author weizhanjie
 */
public final class ParseJsonPathTool implements Tool {

    @Override
    public String getName() {
        return "parse_json_path";
    }

    @Override
    public String getDescription() {
        return "使用 JsonPath 从 JSON 响应中提取数据。适用于 JSON API 返回的数据。";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode jsonTextProp = properties.putObject("json_text");
        jsonTextProp.put("type", "string");
        jsonTextProp.put("description", "从 fetch_page 或 search_website 结果中取得的 JSON 文本");

        ObjectNode pathProp = properties.putObject("json_path");
        pathProp.put("type", "string");
        pathProp.put("description", "JsonPath 表达式，如 '$.data.list[*]'");

        schema.putArray("required").add("json_text").add("json_path");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) throws Exception {
        String jsonText = extractJsonPayload(arguments.path("json_text").asText());
        String jsonPath = arguments.path("json_path").asText();

        if (jsonText == null || jsonText.isEmpty()) {
            return ToolResult.failure("JSON 内容不能为空");
        }

        if (jsonPath == null || jsonPath.isEmpty()) {
            return ToolResult.failure("JsonPath 不能为空");
        }

        context.throwIfCancelled();

        try {
            Project project = context.getProject();
            SiteRuleTools tools = new SiteRuleTools(project);

            // 使用现有的工具方法
            JsonNode args = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                    .put("json_text", jsonText)
                    .put("json_path", jsonPath);
            String result = tools.executeTool("parse_json_path", args);

            if (result == null || result.isEmpty() || result.startsWith("错误:")) {
                return ToolResult.failure("未找到匹配的数据");
            }

            // 截断过长的内容
            if (result.length() > 30000) {
                result = result.substring(0, 30000) + "\n... (内容已截断)";
            }

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure("解析 JSON 失败: " + e.getMessage());
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

    private String extractJsonPayload(String content) {
        if (content == null) {
            return "";
        }
        int marker = content.indexOf("--- 页面内容 ---");
        if (marker < 0) {
            marker = content.indexOf("--- 搜索结果 ---");
        }
        String payload = marker >= 0 ? content.substring(content.indexOf('\n', marker) + 1).trim() : content.trim();
        int objectStart = payload.indexOf('{');
        int arrayStart = payload.indexOf('[');
        int start = objectStart < 0 ? arrayStart : arrayStart < 0 ? objectStart : Math.min(objectStart, arrayStart);
        return start >= 0 ? payload.substring(start) : payload;
    }
}
