package com.wei.wreader.tts.mimo;

import java.util.Base64;

/**
 * MiMo TTS v2.5 非流式响应数据
 */
public class MimoTTSResponse {

    private final String id;
    private final String model;
    private final byte[] audioData;
    private final Usage usage;

    private MimoTTSResponse(Builder builder) {
        this.id = builder.id;
        this.model = builder.model;
        this.audioData = builder.audioData;
        this.usage = builder.usage;
    }

    public String getId() { return id; }
    public String getModel() { return model; }
    public byte[] getAudioData() { return audioData; }
    public Usage getUsage() { return usage; }

    public String getAudioDataBase64() {
        return Base64.getEncoder().encodeToString(audioData);
    }

    public int getAudioLength() {
        return audioData != null ? audioData.length : 0;
    }

    public static class Usage {
        private final int promptTokens;
        private final int completionTokens;
        private final int totalTokens;

        public Usage(int promptTokens, int completionTokens, int totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public int getPromptTokens() { return promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
        public int getTotalTokens() { return totalTokens; }
    }

    public static class Builder {
        private String id;
        private String model;
        private byte[] audioData;
        private Usage usage;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder audioData(byte[] audioData) {
            this.audioData = audioData;
            return this;
        }

        public Builder audioDataFromBase64(String base64Audio) {
            this.audioData = Base64.getDecoder().decode(base64Audio);
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public MimoTTSResponse build() {
            if (audioData == null || audioData.length == 0) {
                throw new IllegalStateException("Audio data is required");
            }
            return new MimoTTSResponse(this);
        }
    }
}
