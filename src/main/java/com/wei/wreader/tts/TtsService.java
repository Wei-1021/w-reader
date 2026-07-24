package com.wei.wreader.tts;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.AppConfigService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.CredentialService;
import com.wei.wreader.tts.edge.EdgeTTS;
import com.wei.wreader.tts.edge.VoiceRole;
import com.wei.wreader.tts.mimo.MimoTTS;
import com.wei.wreader.tts.mimo.MimoTTSConfig;
import com.wei.wreader.tts.mimo.MimoTTSException;
import com.wei.wreader.tts.mimo.MimoTTSRequest;
import com.wei.wreader.tts.mimo.enums.AudioFormat;
import com.wei.wreader.tts.mimo.enums.MimoModel;
import com.wei.wreader.tts.mimo.enums.Voice;
import com.wei.wreader.tts.mimo.enums.VoiceStyle;
import com.wei.wreader.tts.mimo.player.StreamTTSPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * TTS服务门面 - 管理TTS引擎生命周期
 */
public class TtsService {
    private static final Logger LOG = Logger.getInstance(TtsService.class);
    
    /**
     * MiMo TTS 每批最大文本长度
     */
    private static final int MAX_CHUNK_LENGTH = 1000;

    private final Project project;
    private volatile TtsEngine currentEngine;
    private volatile String currentEngineType;
    private volatile boolean userStopped = false;
    private volatile Thread batchThread;

    public TtsService(Project project) {
        this.project = project;
    }

    /**
     * 朗读章节内容（根据配置选择引擎）
     */
    public void speakChapterContent(String chapterContent) {
        CacheService cacheService = CacheService.getInstance();
        Settings settings = cacheService.getSettings();
        String engineType = settings.getTtsEngine();

        if ("mimo".equals(engineType)) {
            mimoTTSChapterContent(chapterContent);
        } else {
            edgeTTSChapterContent(chapterContent);
        }
    }

    /**
     * 使用Edge TTS朗读章节内容
     */
    public void edgeTTSChapterContent(String chapterContent) {
        if (StringUtils.isBlank(chapterContent)) {
            return;
        }

        // 如果已经在播放则停止
        if (currentEngine != null && currentEngine.isPlaying()) {
            userStopped = true;
            stopBatchThread();
            currentEngine.stop();
            currentEngine.dispose();
            currentEngine = null;
            currentEngineType = null;
            return;
        }

        userStopped = false;

        try {
            EdgeTTS edgeTTS = new EdgeTTS();
            configureEdgeTTS(edgeTTS);
            currentEngine = new EdgeTtsEngineWrapper(edgeTTS);
            currentEngineType = "edge";
            currentEngine.synthesize(chapterContent);
            currentEngine.start();
        } catch (Exception e) {
            LOG.error("Failed to initialize Edge TTS", e);
        }
    }

    /**
     * 使用MiMo TTS朗读章节内容
     */
    public void mimoTTSChapterContent(String chapterContent) {
        if (StringUtils.isBlank(chapterContent)) {
            return;
        }

        // 如果已经在播放则停止
        if (currentEngine != null && currentEngine.isPlaying()) {
            userStopped = true;
            stopBatchThread();
            currentEngine.stop();
            currentEngine.dispose();
            currentEngine = null;
            currentEngineType = null;
            return;
        }

        // 如果有引擎但已停止播放（可能是批处理线程还在运行），也停止
        if (batchThread != null && batchThread.isAlive()) {
            userStopped = true;
            stopBatchThread();
            if (currentEngine != null) {
                currentEngine.stop();
                currentEngine.dispose();
                currentEngine = null;
                currentEngineType = null;
            }
            return;
        }

        userStopped = false;

        try {
            String apiKey = CredentialService.getInstance().getMimoApiKey();

            if (StringUtils.isBlank(apiKey)) {
                LOG.warn("MiMo TTS API Key is not configured");
                return;
            }

            CacheService cacheService = CacheService.getInstance();
            Settings settings = cacheService.getSettings();
            MimoModel modelType = MimoModel.fromModelId(settings.getMimoModelType());

            // 根据模型类型处理
            if (modelType == MimoModel.VOICE_DESIGN) {
                // VoiceDesign 模型 - 使用音色描述
                mimoTTSWithVoiceDesign(apiKey, chapterContent, settings);
            } else {
                // 预置音色模型 - 使用风格标签
                mimoTTSWithPresetVoice(apiKey, chapterContent, settings);
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize MiMo TTS", e);
        }
    }

    /**
     * 使用预置音色的 MiMo TTS
     */
    private void mimoTTSWithPresetVoice(String apiKey, String chapterContent, Settings settings) {
        // 清理文本中的特殊字符
        String cleanedContent = cleanTextForTTS(chapterContent);

        // 获取风格和风格控制类型
        String audioStyle = settings.getAudioStyle();
        int styleControlType = settings.getMimoStyleControlType();

        // 兼容性处理：当风格控制没有值时，根据风格指令/音频标签自动推断
        if (styleControlType <= 0) {
            String voiceDescription = settings.getMimoVoiceDescription();
            if (StringUtils.isNotBlank(voiceDescription)) {
                // 风格指令有值，默认使用"自然语言控制"
                styleControlType = Settings.MIMO_STYLE_CONTROL_NATURAL_LANGUAGE;
            } else {
                // 否则使用"音频标签控制"
                styleControlType = Settings.MIMO_STYLE_CONTROL_AUDIO_TAG;
            }
        }

        // 分批处理文本
        List<String> textChunks = splitTextIntoChunks(cleanedContent, MAX_CHUNK_LENGTH);

        // 如果只有一批，直接处理
        if (textChunks.size() <= 1) {
            synthesizeAndPlayWithStyleControl(apiKey, cleanedContent, audioStyle, styleControlType, settings, false);
            return;
        }

        // 多批处理：启动后台线程顺序处理
        final String style = audioStyle;
        final int controlType = styleControlType;
        batchThread = new Thread(() -> {
            try {
                for (int i = 0; i < textChunks.size(); i++) {
                    if (userStopped || Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    String chunk = textChunks.get(i);
                    synthesizeAndPlayWithStyleControl(apiKey, chunk, style, controlType, settings, true);

                    if (i < textChunks.size() - 1) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } finally {
                batchThread = null;
            }
        }, "MimoTTS-Batch");
        batchThread.setDaemon(true);
        batchThread.start();
    }

    /**
     * 使用 VoiceDesign 模型的 MiMo TTS
     */
    private void mimoTTSWithVoiceDesign(String apiKey, String chapterContent, Settings settings) {
        String voiceDescription = settings.getMimoVoiceDescription();
        if (StringUtils.isBlank(voiceDescription)) {
            LOG.warn("MiMo TTS VoiceDesign description is not configured");
            return;
        }

        // 清理文本中的特殊字符
        String cleanedContent = cleanTextForTTS(chapterContent);

        // 分批处理文本
        List<String> textChunks = splitTextIntoChunks(cleanedContent, MAX_CHUNK_LENGTH);
        
        // 如果只有一批，直接处理
        if (textChunks.size() <= 1) {
            synthesizeAndPlayVoiceDesign(apiKey, voiceDescription, cleanedContent, false);
            return;
        }
        
        // 多批处理
        batchThread = new Thread(() -> {
            try {
                for (int i = 0; i < textChunks.size(); i++) {
                    if (userStopped || Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    String chunk = textChunks.get(i);
                    synthesizeAndPlayVoiceDesign(apiKey, voiceDescription, chunk, true);

                    if (i < textChunks.size() - 1) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } finally {
                batchThread = null;
            }
        }, "MimoTTS-VoiceDesign-Batch");
        batchThread.setDaemon(true);
        batchThread.start();
    }

    /**
     * 合成并播放（预置音色）- 保留向后兼容
     */
    private void synthesizeAndPlay(String apiKey, String text, Settings settings, boolean waitComplete) {
        synthesizeAndPlayWithStyleControl(apiKey, text, settings.getAudioStyle(), settings.getMimoStyleControlType(), settings, waitComplete);
    }

    /**
     * 合成并播放（预置音色）- 支持风格控制类型
     *
     * @param apiKey           API密钥
     * @param text             要合成的文本
     * @param style            风格内容（标签或自然语言指令）
     * @param styleControlType 风格控制类型：1-音频标签控制，2-自然语言控制
     * @param settings         设置
     * @param waitComplete     是否等待播放完成
     */
    private void synthesizeAndPlayWithStyleControl(String apiKey, String text, String style, int styleControlType, Settings settings, boolean waitComplete) {
        if (userStopped) return;
        try {
            // 获取Voice
            Voice voice = Voice.fromValue(settings.getVoiceRole());
            MimoTTSConfig config = new MimoTTSConfig.Builder(apiKey)
                    .model(MimoTTSRequest.MODEL_PRESET)
                    .defaultVoice(voice)
                    .build();

            MimoTTS mimoTTS = new MimoTTS(config);
            configureMimoTTS(mimoTTS);

            // 根据风格控制类型处理
            if (styleControlType == Settings.MIMO_STYLE_CONTROL_NATURAL_LANGUAGE) {
                // 自然语言控制：将风格指令放在 role: user 的 content 中
                if (StringUtils.isNotBlank(style) && !"默认".equals(style)) {
                    mimoTTS.setStyleInstruction(style);
                }
                // 文本保持原样，不添加风格标签
                currentEngine = new MimoTtsEngineWrapper(mimoTTS);
                currentEngineType = "mimo";
                currentEngine.synthesize(text);
            } else {
                // 音频标签控制（默认）：将风格标签放在 role: assistant 的 content 中
                String textWithStyle = applyStyleTag(text, style);
                currentEngine = new MimoTtsEngineWrapper(mimoTTS);
                currentEngineType = "mimo";
                currentEngine.synthesize(textWithStyle);
            }

            currentEngine.start();

            // 等待播放完成
            if (waitComplete) {
                while (currentEngine != null && currentEngine.isPlaying()) {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            LOG.error("MiMo TTS batch synthesis failed", e);
        }
    }

    /**
     * 合成并播放（VoiceDesign）
     */
    private void synthesizeAndPlayVoiceDesign(String apiKey, String voiceDescription, String text, boolean waitComplete) {
        if (userStopped) return;
        try {
            MimoTTSConfig config = new MimoTTSConfig.Builder(apiKey)
                    .model(MimoTTSRequest.MODEL_VOICE_DESIGN)
                    .build();
            
            MimoTTS mimoTTS = new MimoTTS(config);
            mimoTTS.setVoiceDescription(voiceDescription);
            
            currentEngine = new MimoTtsEngineWrapper(mimoTTS);
            currentEngineType = "mimo";
            currentEngine.synthesize(text);
            currentEngine.start();
            
            if (waitComplete) {
                while (currentEngine != null && currentEngine.isPlaying()) {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            LOG.error("MiMo TTS VoiceDesign batch synthesis failed", e);
        }
    }

    /**
     * 应用风格标签
     */
    private String applyStyleTag(String text, String style) {
        if (StringUtils.isNotBlank(style) && !"默认".equals(style)) {
            if (!VoiceStyle.hasStyleTag(text)) {
                return VoiceStyle.wrapCustomText(style, text);
            }
        }
        return text;
    }

    /**
     * 将文本分割成多个批次，在自然边界处分割
     * @param text 原始文本
     * @param maxLength 每批最大长度
     * @return 分割后的文本列表
     */
    private List<String> splitTextIntoChunks(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // 如果文本长度小于最大值，直接返回
        if (text.length() <= maxLength) {
            chunks.add(text);
            return chunks;
        }
        
        int startPos = 0;
        while (startPos < text.length()) {
            int endPos = Math.min(startPos + maxLength, text.length());
            
            // 如果不是最后一批，尝试在自然边界处分割
            if (endPos < text.length()) {
                endPos = findSplitPosition(text, startPos, endPos);
            }
            
            chunks.add(text.substring(startPos, endPos));
            startPos = endPos;
        }
        
        return chunks;
    }

    /**
     * 在自然边界处找到分割位置
     * 优先级：段落分隔 > 句号 > 逗号 > 空格
     */
    private int findSplitPosition(String text, int start, int end) {
        // 在 [start, end] 范围内从后向前查找分割点
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            
            // 段落分隔（最高优先级）
            if (c == '\n') {
                return i + 1;
            }
            
            // 句号、问号、感叹号
            if (c == '。' || c == '？' || c == '！' || c == '.' || c == '?' || c == '!') {
                return i + 1;
            }
        }
        
        // 如果没找到标点，尝试在逗号处分割
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '，' || c == '、' || c == ',') {
                return i + 1;
            }
        }
        
        // 如果都没找到，在空格处分割
        for (int i = end - 1; i > start; i--) {
            if (text.charAt(i) == ' ') {
                return i + 1;
            }
        }
        
        // 如果实在找不到合适的分割点，强制在 maxLength 处分割
        return end;
    }

    /**
     * 清理文本中的特殊字符，避免 TTS API 解析异常
     */
    private String cleanTextForTTS(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\uFEFF", "")      // 移除 BOM (Byte Order Mark)
                .replace("\u200B", "")      // 移除零宽空格
                .replace("\u200C", "")      // 移除零宽非连接符
                .replace("\u200D", "")      // 移除零宽连接符
                .replace("\u00A0", " ")     // 将不换行空格替换为普通空格
                .replace("\u2028", "\n")    // 将行分隔符替换为换行
                .replace("\u2029", "\n\n")  // 将段落分隔符替换为双换行
                .trim();
    }

    /**
     * 配置Edge TTS参数
     */
    private void configureEdgeTTS(EdgeTTS edgeTTS) {
        CacheService cacheService = CacheService.getInstance();
        AppConfigService appConfig = AppConfigService.getInstance();
        Settings settings = cacheService.getSettings();
        Settings defaultSettings = appConfig.getSettings();

        String voiceRole = settings.getVoiceRole();
        if (StringUtils.isBlank(voiceRole)) {
            voiceRole = defaultSettings.getVoiceRole();
        }
        VoiceRole voiceRoleEnum = VoiceRole.getByNickName(voiceRole);

        int audioTimeout = settings.getAudioTimeout();
        if (audioTimeout <= 0) {
            audioTimeout = defaultSettings.getAudioTimeout();
        }

        Float rate = settings.getRate();
        if (rate == null || rate <= 0) {
            rate = defaultSettings.getRate();
        }

        Integer volume = settings.getVolume();
        if (volume == null || volume < 0) {
            volume = defaultSettings.getVolume();
        }

        String audioStyle = settings.getAudioStyle();
        if (StringUtils.isBlank(audioStyle)) {
            audioStyle = defaultSettings.getAudioStyle();
        }

        edgeTTS.setVoiceRole(voiceRoleEnum)
                .setStyleName(audioStyle)
                .setRate(rate.toString())
                .setVolume(volume.toString());
    }

    /**
     * 配置MiMo TTS参数
     */
    private void configureMimoTTS(MimoTTS mimoTTS) {
        CacheService cacheService = CacheService.getInstance();
        Settings settings = cacheService.getSettings();
        String voiceRole = settings.getVoiceRole();

        if (StringUtils.isBlank(voiceRole)) {
            voiceRole = "mimo_default";
        }

        // 尝试将voiceRole解析为MiMo Voice枚举
        try {
            Voice voice = Voice.valueOf(voiceRole.toUpperCase().replace("-", "_"));
            mimoTTS.setVoice(voice);
        } catch (IllegalArgumentException e) {
            // 如果解析失败，使用默认音色
            mimoTTS.setVoice(Voice.MIMO_DEFAULT);
        }
        // 风格通过文本标签控制，不需要单独设置
    }

    /**
     * 停止当前TTS播放
     */
    public void stopTTS() {
        userStopped = true;
        stopBatchThread();
        if (currentEngine != null) {
            currentEngine.stop();
            currentEngine.dispose();
            currentEngine = null;
            currentEngineType = null;
        }
    }

    private void stopBatchThread() {
        Thread t = batchThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
        batchThread = null;
    }

    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        return currentEngine != null && currentEngine.isPlaying();
    }

    /**
     * EdgeTTS引擎包装器
     */
    private static class EdgeTtsEngineWrapper implements TtsEngine {
        private final EdgeTTS edgeTTS;

        EdgeTtsEngineWrapper(EdgeTTS edgeTTS) {
            this.edgeTTS = edgeTTS;
        }

        @Override
        public void synthesize(String text) {
            edgeTTS.synthesize(text);
        }

        @Override
        public void start() {
            edgeTTS.start();
        }

        @Override
        public void stop() {
            edgeTTS.dispose();
        }

        @Override
        public void dispose() {
            edgeTTS.dispose();
        }

        @Override
        public boolean isPlaying() {
            return edgeTTS != null && edgeTTS.isPlaying();
        }

        @Override
        public void setOnComplete(Runnable callback) {
            // EdgeTTS handles completion internally
        }

        @Override
        public void setOnError(Consumer<Exception> callback) {
            // EdgeTTS handles errors internally
        }
    }

    /**
     * MiMoTTS引擎包装器
     */
    private static class MimoTtsEngineWrapper implements TtsEngine {
        private final MimoTTS mimoTTS;

        MimoTtsEngineWrapper(MimoTTS mimoTTS) {
            this.mimoTTS = mimoTTS;
        }

        @Override
        public void synthesize(String text) {
            mimoTTS.synthesize(text);
        }

        @Override
        public void start() {
            try {
                mimoTTS.start();
            } catch (MimoTTSException e) {
                LOG.error("MiMo TTS start failed", e);
            }
        }

        @Override
        public void stop() {
            mimoTTS.stop();
        }

        @Override
        public void dispose() {
            mimoTTS.dispose();
        }

        @Override
        public boolean isPlaying() {
            return mimoTTS != null && mimoTTS.isPlaying();
        }

        @Override
        public void setOnComplete(Runnable callback) {
            // MiMoTTS handles completion internally
        }

        @Override
        public void setOnError(Consumer<Exception> callback) {
            // MiMoTTS handles errors internally
        }
    }
}
