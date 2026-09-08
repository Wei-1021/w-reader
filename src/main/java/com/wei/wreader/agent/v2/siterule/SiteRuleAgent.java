package com.wei.wreader.agent.v2.siterule;

import com.wei.wreader.agent.v2.core.Agent;
import com.wei.wreader.agent.v2.core.AgentContext;
import com.wei.wreader.agent.v2.core.Tool;
import com.wei.wreader.agent.v2.runtime.AgentLimits;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WReader 书源生成 Agent
 * <p>
 * 这是通用 Agent Runtime 的一个具体实现
 * 只负责声明 System Prompt 和 Tools，不负责循环
 *
 * @author weizhanjie
 */
public final class SiteRuleAgent implements Agent {

    public static final String ID = "wreader.site-rule";
    public static final String NAME = "WReader 书源生成 Agent";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getSystemPrompt() {
        return SiteRuleSystemPrompt.build();
    }

    @Override
    public List<Tool> getTools() {
        return List.of(
                new FetchPageTool(),
                new SearchWebsiteTool(),
                new ExtractHtmlTool(),
                new ParseJsonPathTool(),
                new ValidateSiteRuleTool(),
                new CompleteSiteRuleTool()
        );
    }

    @Override
    public AgentContext createContext() {
        AgentContext context = Agent.super.createContext();
        AgentLimits limits = AgentLimits.siteRule();
        context.setMaxIterations(limits.getMaxIterations());
        context.setMaxToolCalls(limits.getMaxToolCalls());
        context.setMaxContextChars(limits.getMaxContextChars());
        context.setMaxDurationMillis(limits.getMaxDuration().toMillis());
        return context;
    }

    /**
     * 加载资源文件
     */
    static String loadResource(String path) {
        try (InputStream is = SiteRuleAgent.class.getClassLoader().getResourceAsStream(path)) {
            return is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
