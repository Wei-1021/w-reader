package com.wei.wreader.tts.mimo;

import com.wei.wreader.tts.mimo.enums.AudioFormat;
import com.wei.wreader.tts.mimo.enums.MimoModel;
import com.wei.wreader.tts.mimo.enums.Voice;

/**
 * MiMo TTS v2.5 配置
 */
public class MimoTTSConfig {

    public static final String DEFAULT_BASE_URL = "https://api.xiaomimimo.com/v1";
    public static final String DEFAULT_SUFFIX_URL = "/chat/completions";
    public static final String DEFAULT_MODEL = MimoModel.PRESET.getModelId();
    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    public static final int DEFAULT_READ_TIMEOUT = 60000;
    public static final float DEFAULT_TEMPERATURE = 0.2f;
    public static final float DEFAULT_TOP_P = 0.95f;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Voice defaultVoice;
    private final AudioFormat defaultFormat;
    private final int connectTimeout;
    private final int readTimeout;
    private final float temperature;
    private final float topP;

    private MimoTTSConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.model = builder.model;
        this.defaultVoice = builder.defaultVoice;
        this.defaultFormat = builder.defaultFormat;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
    }

    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getModel() { return model; }
    public Voice getDefaultVoice() { return defaultVoice; }
    public AudioFormat getDefaultFormat() { return defaultFormat; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public float getTemperature() { return temperature; }
    public float getTopP() { return topP; }

    public static class Builder {
        private final String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private String model = DEFAULT_MODEL;
        private Voice defaultVoice = Voice.MIMO_DEFAULT;
        private AudioFormat defaultFormat = AudioFormat.PCM16;
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private int readTimeout = DEFAULT_READ_TIMEOUT;
        private float temperature = DEFAULT_TEMPERATURE;
        private float topP = DEFAULT_TOP_P;

        public Builder(String apiKey) {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API key cannot be null or empty");
            }
            this.apiKey = apiKey;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder defaultVoice(Voice voice) {
            this.defaultVoice = voice;
            return this;
        }

        public Builder defaultFormat(AudioFormat format) {
            this.defaultFormat = format;
            return this;
        }

        public Builder connectTimeout(int timeoutMs) {
            this.connectTimeout = timeoutMs;
            return this;
        }

        public Builder readTimeout(int timeoutMs) {
            this.readTimeout = timeoutMs;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(float topP) {
            this.topP = topP;
            return this;
        }

        public MimoTTSConfig build() {
            return new MimoTTSConfig(this);
        }
    }
}
