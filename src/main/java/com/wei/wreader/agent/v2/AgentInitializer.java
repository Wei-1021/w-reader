package com.wei.wreader.agent.v2;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.wei.wreader.agent.v2.runtime.AgentService;
import com.wei.wreader.agent.v2.siterule.SiteRuleAgent;
import org.jetbrains.annotations.NotNull;

/**
 * Agent 初始化器 - 在项目启动时注册所有 Agent
 *
 * @author weizhanjie
 */
public class AgentInitializer implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(AgentInitializer.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("Initializing WReader Agent v2...");

        try {
            AgentService agentService = AgentService.getInstance(project);
            if (agentService != null) {
                // 注册书源生成 Agent
                agentService.registerAgent(new SiteRuleAgent());
                LOG.info("WReader Agent v2 initialized successfully");
            } else {
                LOG.warn("AgentService not available");
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize Agent v2", e);
        }
    }
}
