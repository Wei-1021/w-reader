package com.wei.wreader.utils.tts.mimo.v2;

import com.wei.wreader.utils.tts.mimo.v2.enums.AudioFormat;
import com.wei.wreader.utils.tts.mimo.v2.enums.Voice;
import com.wei.wreader.utils.tts.mimo.v2.enums.VoiceStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TTS 请求参数封装
 */
public class TTSRequest {

    private final String model;
    private final List<Message> messages;
    private final AudioConfig audio;
    private final boolean stream;

    private TTSRequest(Builder builder) {
        this.model = builder.model;
        this.messages = new ArrayList<>(builder.messages);
        this.audio = new AudioConfig(builder.voice, builder.format);
        this.stream = builder.stream;

        // 应用风格到最后一条 assistant 消息
        if (builder.style != null) {
            applyStyle(builder.style.wrapText(builder.lastAssistantText));
        } else if (builder.customStyle != null) {
            applyStyle(VoiceStyle.wrapCustomText(builder.customStyle, builder.lastAssistantText));
        }
    }

    private void applyStyle(String styledContent) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if ("assistant".equals(msg.role)) {
                messages.set(i, new Message(msg.role, styledContent));
                return;
            }
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("model", model);
        map.put("stream", stream);

        List<Map<String, String>> messageList = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", msg.role);
            msgMap.put("content", msg.content);
            messageList.add(msgMap);
        }
        map.put("messages", messageList);

        Map<String, String> audioMap = new HashMap<>();
        audioMap.put("format", audio.format.getValue());
        audioMap.put("voice", audio.voice.getValue());
        map.put("audio", audioMap);

        return map;
    }

    public boolean isStream() {
        return stream;
    }

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

        public AudioConfig(Voice voice, AudioFormat format) {
            this.voice = voice;
            this.format = format;
        }

        public Voice getVoice() { return voice; }
        public AudioFormat getFormat() { return format; }
    }

    /**
     * 快捷创建：纯文本合成（自动作为 assistant 消息）
     */
    public static Builder of(String text) {
        return new Builder().addAssistantMessage(text);
    }

    /**
     * Builder
     */
    public static class Builder {
        private String model = "mimo-v2-tts";
        private final List<Message> messages = new ArrayList<>();
        private Voice voice = Voice.MIMO_DEFAULT;
        private AudioFormat format = AudioFormat.WAV;
        private VoiceStyle style;
        private String customStyle;
        private String lastAssistantText;
        private boolean stream = false;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 添加用户消息
         */
        public Builder addUserMessage(String content) {
            messages.add(new Message("user", content));
            return this;
        }

        /**
         * 添加助手消息（要合成的文本）
         */
        public Builder addAssistantMessage(String content) {
            messages.add(new Message("assistant", content));
            this.lastAssistantText = content;
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

        /**
         * 设置预定义语音风格
         */
        public Builder style(VoiceStyle style) {
            this.style = style;
            this.customStyle = null;
            return this;
        }

        /**
         * 设置自定义语音风格
         */
        public Builder customStyle(String style) {
            this.customStyle = style;
            this.style = null;
            return this;
        }

        /**
         * 设置是否流式
         */
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public TTSRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalStateException("At least one message is required");
            }
            Message last = messages.get(messages.size() - 1);
//            if (!"assistant".equals(last.role)) {
//                throw new IllegalStateException("Last message must be assistant role, but got: " + last.role);
//            }
            return new TTSRequest(this);
        }
    }
}
