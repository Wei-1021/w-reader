package com.wei.wreader.utils.tts.mimo.v2;

import java.util.Base64;

/**
 * TTS 流式响应数据块
 */
public class TTSStreamResponse {

    private final String id;
    private final String model;
    private final byte[] audioChunk;
    private final int chunkIndex;
    private final boolean done;

    private TTSStreamResponse(Builder builder) {
        this.id = builder.id;
        this.model = builder.model;
        this.audioChunk = builder.audioChunk;
        this.chunkIndex = builder.chunkIndex;
        this.done = builder.done;
    }

    public String getId() { return id; }
    public String getModel() { return model; }
    public byte[] getAudioChunk() { return audioChunk; }
    public int getChunkIndex() { return chunkIndex; }
    public boolean isDone() { return done; }

    public int getChunkLength() {
        return audioChunk != null ? audioChunk.length : 0;
    }

    public static class Builder {
        private String id;
        private String model;
        private byte[] audioChunk;
        private int chunkIndex;
        private boolean done = false;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder audioChunk(byte[] audioChunk) {
            this.audioChunk = audioChunk;
            return this;
        }

        public Builder audioChunkFromBase64(String base64Audio) {
            this.audioChunk = base64Audio != null && !base64Audio.isEmpty()
                ? Base64.getDecoder().decode(base64Audio)
                : new byte[0];
            return this;
        }

        public Builder chunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
            return this;
        }

        public Builder done(boolean done) {
            this.done = done;
            return this;
        }

        public TTSStreamResponse build() {
            return new TTSStreamResponse(this);
        }
    }
}
