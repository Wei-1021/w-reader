package com.wei.wreader.tts.mimo;

import com.wei.wreader.tts.mimo.enums.Voice;
import com.wei.wreader.tts.mimo.enums.VoiceStyle;
import com.wei.wreader.tts.mimo.player.StreamTTSPlayer;

/**
 * MiMo TTS v2.5 主入口
 *
 * 使用方式：
 * <pre>
 * // 使用预置音色
 * MimoTTS tts = new MimoTTS("your-api-key");
 * tts.setVoice(Voice.BINGTANG)
 *    .synthesize("你好，世界");
 * tts.start();
 *
 * // 使用风格标签
 * MimoTTS tts = new MimoTTS("your-api-key");
 * tts.setVoice(Voice.BINGTANG)
 *    .setStyle("开心")
 *    .synthesize("你好，世界");
 * tts.start();
 *
 * // 使用自然语言风格指令
 * MimoTTS tts = new MimoTTS("your-api-key");
 * tts.setVoice(Voice.BINGTANG)
 *    .setStyleInstruction("用欢快的语调")
 *    .synthesize("你好，世界");
 * tts.start();
 * </pre>
 */
public class MimoTTS {

    private final MimoTTSConfig config;
    private final MimoTTSClient client;

    private Voice voice;
    private String style;
    private String styleInstruction;
    private String voiceDescription;
    private String currentText;
    private StreamTTSPlayer currentPlayer;

    private volatile boolean disposed = false;

    /**
     * 使用 API Key 构造
     */
    public MimoTTS(String apiKey) {
        this.config = new MimoTTSConfig.Builder(apiKey).build();
        this.client = new MimoTTSClient(this.config);
        this.voice = this.config.getDefaultVoice();
    }

    /**
     * 使用配置构造
     */
    public MimoTTS(MimoTTSConfig config) {
        this.config = config;
        this.client = new MimoTTSClient(config);
        this.voice = config.getDefaultVoice();
    }

    // ==================== 设置方法 ====================

    public MimoTTS setVoice(Voice voice) {
        this.voice = voice;
        return this;
    }

    /**
     * 设置风格标签（添加到文本开头）
     */
    public MimoTTS setStyle(String style) {
        this.style = style;
        return this;
    }

    /**
     * 设置风格标签（使用 VoiceStyle 枚举）
     */
    public MimoTTS setStyleName(String name) {
        VoiceStyle vs = VoiceStyle.fromValue(name);
        this.style = vs != null ? vs.getValue() : name;
        return this;
    }

    /**
     * 设置自然语言风格指令（放在 user 消息中）
     */
    public MimoTTS setStyleInstruction(String instruction) {
        this.styleInstruction = instruction;
        return this;
    }

    /**
     * 设置音色描述（用于 VoiceDesign 模型）
     */
    public MimoTTS setVoiceDescription(String description) {
        this.voiceDescription = description;
        return this;
    }

    public Voice getVoice() { return voice; }
    public String getStyle() { return style; }
    public String getStyleInstruction() { return styleInstruction; }
    public String getVoiceDescription() { return voiceDescription; }

    // ==================== 合成与播放 ====================

    /**
     * 合成文本（设置当前文本，不立即播放）
     */
    public void synthesize(String text) {
        this.currentText = text;
    }

    /**
     * 开始播放已合成的文本
     */
    public void start() throws MimoTTSException {
        if (disposed) throw new MimoTTSException("MimoTTS has been disposed");
        if (currentText == null || currentText.isEmpty()) throw new MimoTTSException("No text to synthesize");

        stopCurrentPlayer();

        // 确定最终文本（应用风格标签）
        String finalText = currentText;
        if (style != null && !style.isEmpty() && !"默认".equals(style)) {
            if (!VoiceStyle.hasStyleTag(currentText)) {
                finalText = VoiceStyle.wrapCustomText(style, currentText);
            }
        }

        currentPlayer = new StreamTTSPlayer(config);
        
        // 判断是否为 VoiceDesign 模型
        if (voiceDescription != null && !voiceDescription.isEmpty()) {
            // VoiceDesign 模型：将音色描述作为 user 消息
            currentPlayer.startVoiceDesign(voiceDescription, finalText);
        } else {
            // 预置音色模型
            currentPlayer.startPresetWithInstruction(styleInstruction, config.getDefaultVoice(), finalText);
        }
    }

    /**
     * 直接播放文本（合成+播放一步完成）
     */
    public void play(String text) throws MimoTTSException {
        if (disposed) throw new MimoTTSException("MimoTTS has been disposed");

        stopCurrentPlayer();

        currentPlayer = new StreamTTSPlayer(config);
        currentPlayer.start(text, style);
    }

    /**
     * 直接播放文本（带回调）
     */
    public void play(String text, MimoTTSCallback callback) throws MimoTTSException {
        if (disposed) throw new MimoTTSException("MimoTTS has been disposed");

        stopCurrentPlayer();

        currentPlayer = new StreamTTSPlayer(config);
        currentPlayer.start(text, style, null, null, callback);
    }

    /**
     * 停止当前播放
     */
    public void stop() {
        stopCurrentPlayer();
    }

    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        return currentPlayer != null && currentPlayer.isRunning();
    }

    /**
     * 释放资源
     */
    public void dispose() {
        disposed = true;
        stopCurrentPlayer();
    }

    private void stopCurrentPlayer() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer = null;
        }
    }

    // ==================== Builder ====================

    public static class Builder {
        private final String apiKey;
        private Voice voice = Voice.MIMO_DEFAULT;
        private String style;
        private String styleInstruction;

        public Builder(String apiKey) {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API key cannot be null or empty");
            }
            this.apiKey = apiKey;
        }

        public Builder voice(Voice voice) {
            this.voice = voice;
            return this;
        }

        public Builder style(String style) {
            this.style = style;
            return this;
        }

        public Builder styleInstruction(String instruction) {
            this.styleInstruction = instruction;
            return this;
        }

        public MimoTTS build() {
            MimoTTS tts = new MimoTTS(apiKey);
            tts.setVoice(voice);
            if (style != null) tts.setStyle(style);
            if (styleInstruction != null) tts.setStyleInstruction(styleInstruction);
            return tts;
        }
    }
}
