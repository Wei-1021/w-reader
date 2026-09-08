package com.wei.wreader.agent.v2.runtime;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.agent.v2.core.*;
import com.wei.wreader.agent.v2.event.AgentEvent;
import com.wei.wreader.agent.v2.llm.LlmClient;
import com.wei.wreader.agent.v2.llm.impl.OpenAiCompatibleLlmClient;
import com.wei.wreader.service.CredentialService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 服务 - 管理 Agent 生命周期
 * <p>
 * 职责：
 * - 创建和管理 Agent 会话
 * - 提供 LLM 客户端
 * - 管理 Agent 注册
 *
 * @author weizhanjie
 */
@Service(Service.Level.PROJECT)
public final class AgentService implements Disposable {

    private static final Logger LOG = Logger.getInstance(AgentService.class);
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

    private final Project project;
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, AgentRunner.AgentSession> sessions = new ConcurrentHashMap<>();

    public AgentService(Project project) {
        this.project = project;
    }

    public static AgentService getInstance(Project project) {
        return project.getService(AgentService.class);
    }

    // ==================== Agent 注册 ====================

    /**
     * 注册 Agent
     */
    public void registerAgent(Agent agent) {
        agents.put(agent.getId(), agent);
        LOG.info("Registered agent: " + agent.getId());
    }

    /**
     * 获取 Agent
     */
    public Agent getAgent(String agentId) {
        return agents.get(agentId);
    }

    // ==================== Agent 执行 ====================

    /**
     * 启动 Agent 执行
     *
     * @param agentId     Agent ID
     * @param userMessage 用户消息
     * @param eventHandler 事件处理器
     * @param callback    完成回调
     * @return 会话 ID
     */
    public String startAgent(String agentId, String userMessage,
                             Consumer<AgentEvent> eventHandler, Consumer<AgentResult> callback) {
        Agent agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }

        // 创建 LLM 客户端
        LlmClient llmClient = createLlmClient();

        // 创建运行器
        AgentRunner runner = new AgentRunner(llmClient, project);

        // 创建会话
        AgentRunner.AgentSession session = runner.createSession(agent);
        String sessionId = session.getContext().getSessionId();
        String targetBaseUrl = extractTargetBaseUrl(userMessage);
        if (targetBaseUrl != null) {
            session.getContext().put("targetBaseUrl", targetBaseUrl);
        }
        sessions.put(sessionId, session);

        // 异步执行
        session.runAsync(userMessage, eventHandler, result -> {
            // 清理会话
            sessions.remove(sessionId);
            if (callback != null) {
                callback.accept(result);
            }
        });

        return sessionId;
    }

    /**
     * 取消 Agent 执行
     */
    public void cancelAgent(String sessionId) {
        AgentRunner.AgentSession session = sessions.get(sessionId);
        if (session != null) {
            session.cancel();
        }
    }

    /**
     * 检查 Agent 是否正在运行
     */
    public boolean isAgentRunning(String sessionId) {
        AgentRunner.AgentSession session = sessions.get(sessionId);
        return session != null && session.isRunning();
    }

    // ==================== LLM 客户端 ====================

    /**
     * 创建 LLM 客户端
     */
    private LlmClient createLlmClient() {
        CredentialService cs = CredentialService.getInstance();
        String baseUrl = cs.getLlmBaseUrl();
        String apiKey = cs.getLlmApiKey();
        String model = cs.getLlmModel();

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先配置 LLM API Key");
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4";
        }

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(baseUrl, apiKey, model);
        client.setReadTimeout(180000); // 3 分钟

        return client;
    }

    private String extractTargetBaseUrl(String userMessage) {
        if (userMessage == null) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(userMessage);
        if (!matcher.find()) {
            return null;
        }
        try {
            URI uri = URI.create(matcher.group());
            if (uri.getHost() == null) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 生命周期 ====================

    @Override
    public void dispose() {
        // 取消所有运行中的会话
        for (AgentRunner.AgentSession session : sessions.values()) {
            session.cancel();
        }
        sessions.clear();
        agents.clear();
    }
}
