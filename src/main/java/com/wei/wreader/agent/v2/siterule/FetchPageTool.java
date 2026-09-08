package com.wei.wreader.agent.v2.siterule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.agent.v1.SiteRuleTools;
import com.intellij.openapi.project.Project;

import java.time.Duration;
import java.net.URI;

/**
 * 获取网页内容工具
 *
 * @author weizhanjie
 */
public final class FetchPageTool implements Tool {

    @Override
    public String getName() {
        return "fetch_page";
    }

    @Override
    public String getDescription() {
        return "获取指定 URL 的网页内容。返回 HTML 或 JSON 内容。";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode urlProp = properties.putObject("url");
        urlProp.put("type", "string");
        urlProp.put("description", "要获取的网页 URL");

        schema.putArray("required").add("url");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) throws Exception {
        String url = arguments.path("url").asText();

        if (url == null || url.isEmpty()) {
            return ToolResult.failure("URL 不能为空");
        }

        context.throwIfCancelled();

        try {
            Project project = context.getProject();
            SiteRuleTools tools = context.getAgentContext().get("siteRuleTools");
            if (tools == null) {
                String targetBaseUrl = context.getAgentContext().get("targetBaseUrl");
                if (targetBaseUrl != null && !isAllowedTarget(url, targetBaseUrl)) {
                    return ToolResult.failure("请求地址不属于用户指定的网站");
                }
                tools = new SiteRuleTools(project);
                // 首次抓取确定本次会话可访问的目标站点，后续请求复用同一实例和限制。
                tools.setBaseUrl(targetBaseUrl != null ? targetBaseUrl : url);
                context.getAgentContext().put("siteRuleTools", tools);
                context.getAgentContext().put("baseUrl", targetBaseUrl != null ? targetBaseUrl : tools.getExplorer().normalizeBaseUrl(url));
            }

            // 使用现有的工具方法
            JsonNode args = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                    .put("url", url);
            String result = tools.executeTool("fetch_page", args);

            if (result == null || result.isEmpty() || isError(result)) {
                return ToolResult.failure("无法获取页面: " + url);
            }

            // 截断过长的内容
            if (result.length() > 50000) {
                result = result.substring(0, 50000) + "\n... (内容已截断)";
            }

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure("获取页面失败: " + e.getMessage());
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

    private boolean isError(String result) {
        return result.startsWith("错误:") || result.contains("\n错误:") || result.contains("状态: HTTP -1");
    }

    private boolean isAllowedTarget(String url, String targetBaseUrl) {
        try {
            String host = URI.create(url).getHost();
            String targetHost = URI.create(targetBaseUrl).getHost();
            return host != null && targetHost != null &&
                    (host.equalsIgnoreCase(targetHost) || host.toLowerCase().endsWith("." + targetHost.toLowerCase()));
        } catch (Exception e) {
            return false;
        }
    }
}
