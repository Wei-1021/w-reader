package com.wei.wreader.agent.v2.siterule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.util.CustomSiteUtil;

import java.time.Duration;

/**
 * 提交最终书源规则工具
 *
 * @author weizhanjie
 */
public final class CompleteSiteRuleTool implements Tool {

    @Override
    public String getName() {
        return "complete_rule";
    }

    @Override
    public String getDescription() {
        return "提交最终的书源规则。这是生成流程的最后一步。";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode ruleProp = properties.putObject("rule_json");
        ruleProp.put("type", "string");
        ruleProp.put("description", "最终的书源规则 JSON 字符串");

        schema.putArray("required").add("rule_json");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) throws Exception {
        String ruleJson = arguments.path("rule_json").asText();

        if (ruleJson == null || ruleJson.isEmpty()) {
            return ToolResult.failure("规则 JSON 不能为空");
        }

        if (context.getProject() == null) {
            return ToolResult.failure("当前运行环境缺少项目上下文，无法提交书源规则");
        }

        try {
            // complete_rule 是最终闸门：必须通过与导入一致的实际规则校验。
            CustomSiteUtil.getInstance(context.getProject()).parseCustomSiteRule(ruleJson, result -> {}, null);
        } catch (Exception e) {
            return ToolResult.failure("规则未通过校验: " + e.getMessage());
        }

        // 保存到上下文，由 AgentLoop 直接返回该 JSON 并结束运行。
        context.getAgentContext().put("finalRule", ruleJson);

        // 返回成功，标记为完成
        return ToolResult.success("{\"completed\": true, \"message\": \"规则生成完成\"}");
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(5);
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ_ONLY;
    }
}
