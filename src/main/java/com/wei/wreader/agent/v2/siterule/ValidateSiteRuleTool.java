package com.wei.wreader.agent.v2.siterule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.util.CustomSiteUtil;

import java.time.Duration;

/**
 * 校验书源规则工具
 *
 * @author weizhanjie
 */
public final class ValidateSiteRuleTool implements Tool {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "validate_site_rule";
    }

    @Override
    public String getDescription() {
        return "校验生成的书源规则 JSON 是否有效。检查必填字段和格式。";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode ruleProp = properties.putObject("rule_json");
        ruleProp.put("type", "string");
        ruleProp.put("description", "书源规则 JSON 字符串");

        schema.putArray("required").add("rule_json");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) throws Exception {
        String ruleJson = arguments.path("rule_json").asText();

        if (ruleJson == null || ruleJson.isEmpty()) {
            return ToolResult.failure("规则 JSON 不能为空");
        }

        context.throwIfCancelled();

        try {
            // 解析 JSON
            JsonNode node = objectMapper.readTree(ruleJson);

            // 检查是否为数组
            if (!node.isArray()) {
                return ToolResult.failure("规则必须是 JSON 数组格式");
            }

            if (node.isEmpty()) {
                return ToolResult.failure("规则数组不能为空");
            }

            // 验证每个规则
            StringBuilder errors = new StringBuilder();
            for (int i = 0; i < node.size(); i++) {
                JsonNode rule = node.get(i);
                String error = validateRule(rule, i);
                if (error != null) {
                    errors.append(error).append("\n");
                }
            }

            if (errors.length() > 0) {
                return ToolResult.failure("校验失败:\n" + errors.toString().trim());
            }

            if (context.getProject() == null) {
                return ToolResult.failure("当前运行环境缺少项目上下文，无法校验书源规则");
            }
            // 使用实际导入路径的反序列化校验，避免只通过最小字段检查。
            CustomSiteUtil.getInstance(context.getProject()).parseCustomSiteRule(ruleJson, result -> {}, null);

            return ToolResult.success("校验通过，共 " + node.size() + " 条规则");
        } catch (Exception e) {
            return ToolResult.failure("JSON 解析失败: " + e.getMessage());
        }
    }

    private String validateRule(JsonNode rule, int index) {
        String prefix = "规则[" + index + "]: ";

        // 检查必填字段
        if (rule.path("id").isMissingNode() || rule.path("id").asText("").isEmpty()) {
            return prefix + "id 字段不能为空";
        }
        if (rule.path("name").isMissingNode() || rule.path("name").asText("").isEmpty()) {
            return prefix + "name 字段不能为空";
        }
        if (rule.path("baseUrl").isMissingNode() || rule.path("baseUrl").asText("").isEmpty()) {
            return prefix + "baseUrl 字段不能为空";
        }

        // 检查 baseUrl 格式
        String baseUrl = rule.path("baseUrl").asText("");
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            return prefix + "baseUrl 必须以 http:// 或 https:// 开头";
        }

        return null; // 校验通过
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(10);
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ_ONLY;
    }
}
