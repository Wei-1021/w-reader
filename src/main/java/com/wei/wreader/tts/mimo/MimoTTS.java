package com.wei.wreader.tts.mimo;

import com.wei.wreader.tts.mimo.enums.Voice;
import com.wei.wreader.tts.mimo.enums.VoiceStyle;
import com.wei.wreader.tts.mimo.player.NonStreamTTSPlayer;
import com.wei.wreader.tts.mimo.player.StreamTTSPlayer;

import java.util.function.Consumer;

/**
 * MiMo TTS v2.5 主入口
 *
 * 支持两种播放模式：
 * 1. 流式模式（Stream）：使用 SSE 流式 API，边接收边播放
 * 2. 非流式模式（NonStream）：使用非流式 API，文本分段合成，边合成边播放
 *
 * 使用方式：
 * <pre>
 * // 使用预置音色（流式模式）
 * MimoTTS tts = new MimoTTS("your-api-key");
 * tts.setVoice(Voice.BINGTANG)
 *    .synthesize("你好，世界");
 * tts.start();
 *
 * // 使用预置音色（非流式模式）
 * MimoTTS tts = new MimoTTS("your-api-key");
 * tts.setVoice(Voice.BINGTANG)
 *    .setStreamMode(false)
 *    .synthesize("很长的文本...");
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

    /** 是否使用流式模式，默认 true */
    private boolean streamMode = true;

    /** 流式播放器 */
    private StreamTTSPlayer streamPlayer;
    /** 非流式播放器 */
    private NonStreamTTSPlayer nonStreamPlayer;

    private volatile boolean disposed = false;

    // 回调函数
    private Consumer<String> onStatusUpdate;  // 状态更新回调
    // 回调函数
    private NonStreamTTSPlayer.PlayerListener playerListener;  // 播放器监听器

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

    /**
     * 设置是否使用流式模式
     *
     * @param streamMode true=流式模式，false=非流式模式
     */
    public MimoTTS setStreamMode(boolean streamMode) {
        this.streamMode = streamMode;
        return this;
    }

    public Voice getVoice() { return voice; }
    public String getStyle() { return style; }
    public String getStyleInstruction() { return styleInstruction; }
    public String getVoiceDescription() { return voiceDescription; }
    public boolean isStreamMode() { return streamMode; }

    /**
     * 设置状态更新回调（用于显示合成进度等信息）
     */
    public MimoTTS setOnStatusUpdate(Consumer<String> callback) {
        this.onStatusUpdate = callback;
        return this;
    }
    /**
     * 播放合成状态更新回调
     */
    public MimoTTS setPlayerListener(NonStreamTTSPlayer.PlayerListener listener) {
        this.playerListener = listener;
        return this;
    }

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

        if (streamMode) {
            // 流式模式
            startStreamMode(finalText);
        } else {
            // 非流式模式
            startNonStreamMode(finalText);
        }
    }

    /**
     * 使用流式模式播放
     */
    private void startStreamMode(String text) throws MimoTTSException {
        streamPlayer = new StreamTTSPlayer(config);

        // 判断是否为 VoiceDesign 模型
        if (voiceDescription != null && !voiceDescription.isEmpty()) {
            // VoiceDesign 模型：将音色描述作为 user 消息
            streamPlayer.startVoiceDesign(voiceDescription, text);
        } else {
            // 预置音色模型
            streamPlayer.startPresetWithInstruction(styleInstruction, config.getDefaultVoice(), text);
        }
    }

    /**
     * 使用非流式模式播放
     * 特点：
     * 1. 文本分段：每段最大200字
     * 2. 边合成边播放：当前播放时，后台合成剩余内容
     * 3. 连续播放：减少两段文字之间的等待时间
     */
    private void startNonStreamMode(String text) throws MimoTTSException {
        nonStreamPlayer = new NonStreamTTSPlayer(config);
        if (voice != null) {
            nonStreamPlayer.setVoice(voice);
        }
        nonStreamPlayer.setStyle(style);
        nonStreamPlayer.setStyleInstruction(styleInstruction);
        nonStreamPlayer.setVoiceDescription(voiceDescription);

        // 设置播放监听器
        if (playerListener == null) {
            playerListener = new NonStreamTTSPlayer.PlayerListener() {
                @Override
                public void onSynthesizeStart(int totalChunks) {
                    if (onStatusUpdate != null) {
                        onStatusUpdate.accept("正在合成音频，共 " + totalChunks + " 段...");
                    }
                }

                @Override
                public void onSynthesizeProgress(int currentChunk, int totalChunks) {
                    if (onStatusUpdate != null) {
                        onStatusUpdate.accept("正在合成第 " + currentChunk + "/" + totalChunks + " 段...");
                    }
                }

                @Override
                public void onStarted() {
                    if (onStatusUpdate != null) {
                        onStatusUpdate.accept("开始播放");
                    }
                }

                @Override
                public void onCompleted(long totalBytes, long durationMs) {
                    if (onStatusUpdate != null) {
                        onStatusUpdate.accept("播放完成");
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (onStatusUpdate != null) {
                        onStatusUpdate.accept("播放出错: " + e.getMessage());
                    }
                }
            };
        }
        nonStreamPlayer.setListener(playerListener);

        // 判断是否为 VoiceDesign 模型
        boolean isVoiceDesign = voiceDescription != null && !voiceDescription.isEmpty();

        // VoiceDesign 模型需要添加段间停顿
        nonStreamPlayer.setAddChunkPause(isVoiceDesign);

        if (isVoiceDesign) {
            nonStreamPlayer.startVoiceDesign(voiceDescription, text);
        } else {
            nonStreamPlayer.startPreset(text, voice, styleInstruction);
        }
    }

    /**
     * 直接播放文本（合成+播放一步完成）
     */
    public void play(String text) throws MimoTTSException {
        if (disposed) throw new MimoTTSException("MimoTTS has been disposed");

        stopCurrentPlayer();

        if (streamMode) {
            streamPlayer = new StreamTTSPlayer(config);
            streamPlayer.start(text, style);
        } else {
            nonStreamPlayer = new NonStreamTTSPlayer(config);
            nonStreamPlayer.setVoice(voice);
            nonStreamPlayer.setStyle(style);
            nonStreamPlayer.startPreset(text, voice);
        }
    }

    /**
     * 直接播放文本（带回调）
     */
    public void play(String text, MimoTTSCallback callback) throws MimoTTSException {
        if (disposed) throw new MimoTTSException("MimoTTS has been disposed");

        stopCurrentPlayer();

        if (streamMode) {
            streamPlayer = new StreamTTSPlayer(config);
            streamPlayer.start(text, style, null, null, callback);
        } else {
            nonStreamPlayer = new NonStreamTTSPlayer(config);
            nonStreamPlayer.setVoice(voice);
            nonStreamPlayer.setStyle(style);
            nonStreamPlayer.setCallback(callback);
            nonStreamPlayer.startPreset(text, voice);
        }
    }

    /**
     * 停止当前播放
     */
    public void stop() {
        stopCurrentPlayer();
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (streamPlayer != null) {
            streamPlayer.pause();
        } else if (nonStreamPlayer != null) {
            nonStreamPlayer.pause();
        }
    }

    /**
     * 恢复播放
     */
    public void resume() {
        if (streamPlayer != null) {
            streamPlayer.resume();
        } else if (nonStreamPlayer != null) {
            nonStreamPlayer.resume();
        }
    }

    /**
     * 是否已暂停
     */
    public boolean isPaused() {
        if (streamPlayer != null) {
            return streamPlayer.isPaused();
        } else if (nonStreamPlayer != null) {
            return nonStreamPlayer.isPaused();
        }
        return false;
    }

    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        if (streamPlayer != null) {
            return streamPlayer.isRunning() && !streamPlayer.isPaused();
        } else if (nonStreamPlayer != null) {
            return nonStreamPlayer.isRunning() && !nonStreamPlayer.isPaused();
        }
        return false;
    }

    /**
     * 释放资源
     */
    public void dispose() {
        disposed = true;
        stopCurrentPlayer();
    }

    private void stopCurrentPlayer() {
        if (streamPlayer != null) {
            streamPlayer.stop();
            streamPlayer = null;
        }
        if (nonStreamPlayer != null) {
            nonStreamPlayer.stop();
            nonStreamPlayer = null;
        }
    }

    // ==================== Builder ====================

    public static class Builder {
        private final String apiKey;
        private Voice voice = Voice.MIMO_DEFAULT;
        private String style;
        private String styleInstruction;
        private boolean streamMode = true;

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

        public Builder streamMode(boolean streamMode) {
            this.streamMode = streamMode;
            return this;
        }

        public MimoTTS build() {
            MimoTTS tts = new MimoTTS(apiKey);
            tts.setVoice(voice);
            tts.setStreamMode(streamMode);
            if (style != null) tts.setStyle(style);
            if (styleInstruction != null) tts.setStyleInstruction(styleInstruction);
            return tts;
        }
    }
}
