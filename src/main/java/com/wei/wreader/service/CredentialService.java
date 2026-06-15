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

    private static final CredentialService INSTANCE = new CredentialService();

    // 缓存 API Key，避免频繁调用 PasswordSafe（慢操作）
    private volatile String cachedMimoApiKey;
    private volatile boolean keyLoaded = false;

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

    /**
     * 清除缓存（用于重新加载）
     */
    public void clearCache() {
        keyLoaded = false;
        cachedMimoApiKey = null;
    }

    /**
     * 创建凭证属性
     */
    private CredentialAttributes createCredentialAttributes(String serviceName, String userName) {
        return new CredentialAttributes(serviceName, userName, getClass(), false);
    }
}
