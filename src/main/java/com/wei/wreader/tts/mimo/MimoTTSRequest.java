package com.wei.wreader.tts.mimo;

import com.wei.wreader.tts.mimo.enums.AudioFormat;
import com.wei.wreader.tts.mimo.enums.MimoModel;
import com.wei.wreader.tts.mimo.enums.Voice;
import com.wei.wreader.tts.mimo.enums.VoiceStyle;

import java.util.*;

/**
 * MiMo TTS v2.5 请求参数封装
 * 支持三种模型：
 * - mimo-v2.5-tts: 使用预置音色
 * - mimo-v2.5-tts-voicedesign: 文本描述音色
 * - mimo-v2.5-tts-voiceclone: 音频样本复刻音色
 */
public class MimoTTSRequest {

    // 保留向后兼容的常量
    public static final String MODEL_PRESET = MimoModel.PRESET.getModelId();
    public static final String MODEL_VOICE_DESIGN = MimoModel.VOICE_DESIGN.getModelId();
    public static final String MODEL_VOICE_CLONE = MimoModel.VOICE_CLONE.getModelId();

    private final String model;
    private final List<Message> messages;
    private final AudioConfig audio;
    private final boolean stream;
    private final float temperature;
    private final float topP;
    private final Boolean optimizeTextPreview;

    private MimoTTSRequest(Builder builder) {
        this.model = builder.model;
        this.messages = new ArrayList<>(builder.messages);
        this.audio = new AudioConfig(builder.voice, builder.format, builder.voiceCloneData);
        this.stream = builder.stream;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.optimizeTextPreview = builder.optimizeTextPreview;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("model", model);
        map.put("stream", stream);
        map.put("temperature", temperature);
        map.put("top_p", topP);

        // 消息列表
        List<Map<String, String>> messageList = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", msg.role);
            msgMap.put("content", msg.content);
            messageList.add(msgMap);
        }
        map.put("messages", messageList);

        // 音频配置
        Map<String, Object> audioMap = new HashMap<>();
        audioMap.put("format", audio.format.getValue());
        if (audio.voice != null) {
            audioMap.put("voice", audio.voice.getValue());
        }
        if (audio.voiceCloneData != null) {
            audioMap.put("voice", audio.voiceCloneData);
        }
        if (optimizeTextPreview != null) {
            audioMap.put("optimize_text_preview", optimizeTextPreview);
        }
        map.put("audio", audioMap);

        return map;
    }

    public boolean isStream() {
        return stream;
    }

    public String getModel() {
        return model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    // ==================== 内部类 ====================

    public static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    public static class AudioConfig {
        private final Voice voice;
        private final AudioFormat format;
        private final String voiceCloneData;

        public AudioConfig(Voice voice, AudioFormat format, String voiceCloneData) {
            this.voice = voice;
            this.format = format;
            this.voiceCloneData = voiceCloneData;
        }

        public Voice getVoice() { return voice; }
        public AudioFormat getFormat() { return format; }
        public String getVoiceCloneData() { return voiceCloneData; }
    }

    // ==================== 快捷创建方法 ====================

    /**
     * 使用预置音色合成
     */
    public static Builder ofPreset(String text, Voice voice) {
        return new Builder()
                .model(MODEL_PRESET)
                .voice(voice)
                .addAssistantMessage(text);
    }

    /**
     * 使用预置音色 + 风格标签合成
     */
    public static Builder ofPresetWithStyle(String text, Voice voice, String style) {
        String styledText = style != null && !style.isEmpty() && !"默认".equals(style)
                ? VoiceStyle.wrapCustomText(style, text)
                : text;
        return new Builder()
                .model(MODEL_PRESET)
                .voice(voice)
                .addAssistantMessage(styledText);
    }

    /**
     * 使用预置音色 + 自然语言风格指令合成
     */
    public static Builder ofPresetWithInstruction(String text, Voice voice, String instruction) {
        Builder builder = new Builder()
                .model(MODEL_PRESET)
                .voice(voice);
        if (instruction != null && !instruction.isEmpty()) {
            builder.addUserMessage(instruction);
        }
        builder.addAssistantMessage(text);
        return builder;
    }

    /**
     * 使用文本描述音色合成 (Voice Design)
     */
    public static Builder ofVoiceDesign(String text, String voiceDescription) {
        return new Builder()
                .model(MODEL_VOICE_DESIGN)
                .addUserMessage(voiceDescription)
                .addAssistantMessage(text);
    }

    /**
     * 使用音色复刻合成 (Voice Clone)
     */
    public static Builder ofVoiceClone(String text, String base64AudioData) {
        String voiceData = "data:audio/mpeg;base64," + base64AudioData;
        return new Builder()
                .model(MODEL_VOICE_CLONE)
                .voiceCloneData(voiceData)
                .addAssistantMessage(text);
    }

    /**
     * 纯文本合成（默认预置音色）
     */
    public static Builder of(String text) {
        return new Builder()
                .model(MODEL_PRESET)
                .addAssistantMessage(text);
    }

    // ==================== Builder ====================

    public static class Builder {
        private String model = MODEL_PRESET;
        private final List<Message> messages = new ArrayList<>();
        private Voice voice = Voice.MIMO_DEFAULT;
        private AudioFormat format = AudioFormat.PCM16;
        private String voiceCloneData;
        private boolean stream = false;
        private float temperature = MimoTTSConfig.DEFAULT_TEMPERATURE;
        private float topP = MimoTTSConfig.DEFAULT_TOP_P;
        private Boolean optimizeTextPreview;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder addUserMessage(String content) {
            messages.add(new Message("user", content));
            return this;
        }

        public Builder addAssistantMessage(String content) {
            messages.add(new Message("assistant", content));
            return this;
        }

        public Builder voice(Voice voice) {
            this.voice = voice;
            return this;
        }

        public Builder format(AudioFormat format) {
            this.format = format;
            return this;
        }

        public Builder voiceCloneData(String voiceCloneData) {
            this.voiceCloneData = voiceCloneData;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
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

        public Builder optimizeTextPreview(Boolean optimizeTextPreview) {
            this.optimizeTextPreview = optimizeTextPreview;
            return this;
        }

        public MimoTTSRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalStateException("At least one message is required");
            }
            return new MimoTTSRequest(this);
        }
    }
}
