package com.wei.wreader.service;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

/**
 * 凭证存储服务 - 使用 IntelliJ 平台的 PasswordSafe 存储敏感信息
 */
public class CredentialService {
    private static final String SERVICE_NAME_PREFIX = "W-Reader";
    private static final String MIMO_API_KEY_SERVICE = SERVICE_NAME_PREFIX + ".MimoApiKey";
    private static final String MIMO_API_KEY_USER = "mimo-tts";

    private static final String LLM_API_KEY_SERVICE = SERVICE_NAME_PREFIX + ".LlmApiKey";
    private static final String LLM_API_KEY_USER = "llm-site-rule";
    private static final String LLM_BASE_URL_SERVICE = SERVICE_NAME_PREFIX + ".LlmBaseUrl";
    private static final String LLM_BASE_URL_USER = "llm-site-rule";
    private static final String LLM_MODEL_SERVICE = SERVICE_NAME_PREFIX + ".LlmModel";
    private static final String LLM_MODEL_USER = "llm-site-rule";

    private static final CredentialService INSTANCE = new CredentialService();

    // 缓存 API Key，避免频繁调用 PasswordSafe（慢操作）
    private volatile String cachedMimoApiKey;
    private volatile boolean keyLoaded = false;

    // LLM 凭证缓存
    private volatile String cachedLlmApiKey;
    private volatile boolean llmKeyLoaded = false;
    private volatile String cachedLlmBaseUrl;
    private volatile boolean llmBaseUrlLoaded = false;
    private volatile String cachedLlmModel;
    private volatile boolean llmModelLoaded = false;

    public static CredentialService getInstance() {
        return INSTANCE;
    }

    private CredentialService() {
    }

    /**
     * 保存 MiMo API Key
     */
    public void saveMimoApiKey(String apiKey) {
        this.cachedMimoApiKey = apiKey;
        this.keyLoaded = true;
        
        CredentialAttributes attributes = createCredentialAttributes(MIMO_API_KEY_SERVICE, MIMO_API_KEY_USER);
        if (apiKey == null || apiKey.isEmpty()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            Credentials credentials = new Credentials(MIMO_API_KEY_USER, apiKey);
            // 第三个参数 false 表示持久化存储（跨 IDE 重启保留）
            PasswordSafe.getInstance().set(attributes, credentials, false);
        }
    }

    /**
     * 获取 MiMo API Key（优先从缓存读取）
     */
    public String getMimoApiKey() {
        // 如果已缓存，直接返回
        if (keyLoaded) {
            return cachedMimoApiKey;
        }
        
        // 否则从 PasswordSafe 加载（仅首次）
        CredentialAttributes attributes = createCredentialAttributes(MIMO_API_KEY_SERVICE, MIMO_API_KEY_USER);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if (credentials != null) {
            cachedMimoApiKey = credentials.getPasswordAsString();
        }
        keyLoaded = true;
        return cachedMimoApiKey;
    }

    // ==================== LLM API Key ====================

    public void saveLlmApiKey(String apiKey) {
        this.cachedLlmApiKey = apiKey;
        this.llmKeyLoaded = true;
        CredentialAttributes attributes = createCredentialAttributes(LLM_API_KEY_SERVICE, LLM_API_KEY_USER);
        if (apiKey == null || apiKey.isEmpty()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            Credentials credentials = new Credentials(LLM_API_KEY_USER, apiKey);
            PasswordSafe.getInstance().set(attributes, credentials, false);
        }
    }

    public String getLlmApiKey() {
        if (llmKeyLoaded) {
            return cachedLlmApiKey;
        }
        CredentialAttributes attributes = createCredentialAttributes(LLM_API_KEY_SERVICE, LLM_API_KEY_USER);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if (credentials != null) {
            cachedLlmApiKey = credentials.getPasswordAsString();
        }
        llmKeyLoaded = true;
        return cachedLlmApiKey;
    }

    // ==================== LLM Base URL ====================

    public void saveLlmBaseUrl(String baseUrl) {
        this.cachedLlmBaseUrl = baseUrl;
        this.llmBaseUrlLoaded = true;
        CredentialAttributes attributes = createCredentialAttributes(LLM_BASE_URL_SERVICE, LLM_BASE_URL_USER);
        if (baseUrl == null || baseUrl.isEmpty()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            Credentials credentials = new Credentials(LLM_BASE_URL_USER, baseUrl);
            PasswordSafe.getInstance().set(attributes, credentials, false);
        }
    }

    public String getLlmBaseUrl() {
        if (llmBaseUrlLoaded) {
            return cachedLlmBaseUrl;
        }
        CredentialAttributes attributes = createCredentialAttributes(LLM_BASE_URL_SERVICE, LLM_BASE_URL_USER);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if (credentials != null) {
            cachedLlmBaseUrl = credentials.getPasswordAsString();
        }
        llmBaseUrlLoaded = true;
        return cachedLlmBaseUrl;
    }

    // ==================== LLM Model ====================

    public void saveLlmModel(String model) {
        this.cachedLlmModel = model;
        this.llmModelLoaded = true;
        CredentialAttributes attributes = createCredentialAttributes(LLM_MODEL_SERVICE, LLM_MODEL_USER);
        if (model == null || model.isEmpty()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            Credentials credentials = new Credentials(LLM_MODEL_USER, model);
            PasswordSafe.getInstance().set(attributes, credentials, false);
        }
    }

    public String getLlmModel() {
        if (llmModelLoaded) {
            return cachedLlmModel;
        }
        CredentialAttributes attributes = createCredentialAttributes(LLM_MODEL_SERVICE, LLM_MODEL_USER);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if (credentials != null) {
            cachedLlmModel = credentials.getPasswordAsString();
        }
        llmModelLoaded = true;
        return cachedLlmModel;
    }

    // ==================== Cache ====================

    /**
     * 清除缓存（用于重新加载）
     */
    public void clearCache() {
        keyLoaded = false;
        cachedMimoApiKey = null;
        llmKeyLoaded = false;
        cachedLlmApiKey = null;
        llmBaseUrlLoaded = false;
        cachedLlmBaseUrl = null;
        llmModelLoaded = false;
        cachedLlmModel = null;
    }

    /**
     * 创建凭证属性
     */
    private CredentialAttributes createCredentialAttributes(String serviceName, String userName) {
        return new CredentialAttributes(serviceName, userName);
    }
}
