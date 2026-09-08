package com.wei.wreader.agent.v2.llm;

/**
 * LLM 客户端接口
 * <p>
 * 支持多种 LLM 提供商：
 * - OpenAI
 * - DeepSeek
 * - SiliconFlow
 * - Ollama
 * - 自建 API Gateway
 *
 * @author weizhanjie
 */
public interface LlmClient {

    /**
     * 调用 LLM
     *
     * @param request LLM 请求
     * @return LLM 响应
     * @throws LlmException 调用失败
     */
    LlmResponse chat(LlmRequest request) throws LlmException;

    /**
     * LLM 异常
     */
    class LlmException extends Exception {
        private final int statusCode;

        public LlmException(String message) {
            super(message);
            this.statusCode = -1;
        }

        public LlmException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
        }

        public LlmException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public LlmException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getUserFriendlyMessage() {
            if (statusCode == 401) {
                return "API Key 无效或已过期";
            } else if (statusCode == 429) {
                return "API 调用频率超限，请稍后重试";
            } else if (statusCode >= 500) {
                return "API 服务异常，请稍后重试";
            }
            return getMessage();
        }
    }
}
