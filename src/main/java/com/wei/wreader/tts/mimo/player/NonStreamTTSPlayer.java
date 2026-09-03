package com.wei.wreader.tts.mimo.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.wei.wreader.tts.mimo.MimoTTSCallback;
import com.wei.wreader.tts.mimo.MimoTTSConfig;
import com.wei.wreader.tts.mimo.MimoTTSException;
import com.wei.wreader.tts.mimo.MimoTTSRequest;
import com.wei.wreader.tts.mimo.enums.AudioFormat;
import com.wei.wreader.tts.mimo.enums.Voice;
import com.wei.wreader.tts.mimo.enums.VoiceStyle;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiMo TTS 非流式播放器
 * <pre>
 * 1. 文本分割：将长文本按自然边界分割，每段最大200字
 * 2. 生产者-消费者模式：
 *    - 合成线程（生产者）：后台合成音频，放入队列
 *    - 播放线程（消费者）：从队列取出音频播放
 * 3. 边合成边播放：当前播放时，后台合成剩余内容
 * 4. 连续播放：减少两段文字之间的等待时间
 *</pre>
 *
 * @author weizhanjie
 */
public class NonStreamTTSPlayer {
    private static final Logger LOG = Logger.getInstance(NonStreamTTSPlayer.class);

    /** 每段文本最大长度 */
    private static final int MAX_CHUNK_LENGTH = 200;

    /** 音频队列结束标记 */
    private static final byte[] END_MARKER = new byte[0];

    /** 文本队列结束标记 */
    private static final String TEXT_END_MARKER = "<<END>>";

    /** 预合成队列大小：当队列中音频块数量超过此值时，暂停合成 */
    private static final int PREFETCH_QUEUE_SIZE = 3;

    public enum State {
        IDLE, SYNTHESIZING, PLAYING, PAUSED, STOPPED, COMPLETED, ERROR
    }

    private final MimoTTSConfig config;
    private final ObjectMapper objectMapper;

    private volatile State state = State.IDLE;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong chunkCount = new AtomicLong(0);

    private Thread synthesizeThread;  // 合成线程（生产者）
    private Thread playerThread;      // 播放线程（消费者）
    private PCMStreamPlayer pcmPlayer;

    /** 音频块队列：合成线程放入，播放线程取出 */
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();

    /** 文本块队列：将分割后的文本放入 */
    private final BlockingQueue<String> textQueue = new LinkedBlockingQueue<>();

    private PlayerListener listener;
    private MimoTTSCallback callback;

    // 合成参数
    private Voice voice;
    private String style;
    private String styleInstruction;
    private String voiceDescription;

    public interface PlayerListener {
        default void onStarted() {}
        default void onSynthesizeStart(int totalChunks) {}  // 合成开始
        default void onSynthesizeProgress(int currentChunk, int totalChunks) {}  // 合成进度
        default void onChunkPlayed(byte[] data, long totalBytes, long chunkCount) {}
        default void onCompleted(long totalBytes, long durationMs) {}
        default void onError(Exception e) {}
        default void onStateChanged(State oldState, State newState) {}
    }

    public NonStreamTTSPlayer(MimoTTSConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 设置参数 ====================

    public void setVoice(Voice voice) {
        this.voice = voice;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setStyleInstruction(String styleInstruction) {
        this.styleInstruction = styleInstruction;
    }

    public void setVoiceDescription(String voiceDescription) {
        this.voiceDescription = voiceDescription;
    }

    // ==================== 启动播放 ====================

    /**
     * 使用预置音色播放
     *
     * @param text   要合成的文本
     * @param voice  音色
     */
    public void startPreset(String text, Voice voice) throws MimoTTSException {
        startPreset(text, voice, null);
    }

    /**
     * 使用预置音色播放（带风格指令）
     *
     * @param text             要合成的文本
     * @param voice            音色
     * @param styleInstruction 风格指令
     */
    public void startPreset(String text, Voice voice, String styleInstruction) throws MimoTTSException {
        if (running.get()) {
            throw new MimoTTSException("Player is already running, stop it first");
        }

        this.voice = voice;
        this.styleInstruction = styleInstruction;

        // 分割文本
        List<String> chunks = splitTextIntoChunks(text);
        LOG.info("NonStreamTTS: Split text into " + chunks.size() + " chunks");

        // 启动播放
        startInternal(chunks, false);
    }

    /**
     * 使用 VoiceDesign 模型播放
     *
     * @param voiceDescription 音色描述
     * @param text             要合成的文本
     */
    public void startVoiceDesign(String voiceDescription, String text) throws MimoTTSException {
        if (running.get()) {
            throw new MimoTTSException("Player is already running, stop it first");
        }

        this.voiceDescription = voiceDescription;

        // 分割文本
        List<String> chunks = splitTextIntoChunks(text);
        LOG.info("NonStreamTTS VoiceDesign: Split text into " + chunks.size() + " chunks");

        // 启动播放
        startInternal(chunks, true);
    }

    /**
     * 内部启动方法
     */
    private void startInternal(List<String> textChunks, boolean isVoiceDesign) throws MimoTTSException {
        reset();

        // 记录总文本块数量
        int totalChunks = textChunks.size();

        // 将文本块放入队列
        textQueue.addAll(textChunks);
        textQueue.offer(TEXT_END_MARKER);  // 结束标记

        running.set(true);
        setState(State.SYNTHESIZING);

        // 通知合成开始
        if (listener != null) {
            listener.onSynthesizeStart(totalChunks);
        }

        // 启动播放线程（消费者）
        startPlayerThread();

        // 启动合成线程（生产者）
        startSynthesizeThread(isVoiceDesign, callback, totalChunks);
    }

    // ==================== 控制方法 ====================

    public void stop() {
        if (!running.get()) return;

        running.set(false);

        // 清空队列
        textQueue.clear();
        audioQueue.clear();
        audioQueue.offer(END_MARKER);

        if (synthesizeThread != null) {
            synthesizeThread.interrupt();
        }

        if (pcmPlayer != null) {
            pcmPlayer.stop();
            pcmPlayer = null;
        }

        setState(State.STOPPED);
    }

    public void pause() {
        if (state == State.PLAYING && pcmPlayer != null) {
            pcmPlayer.pause();
            setState(State.PAUSED);
        }
    }

    public void resume() {
        if (state == State.PAUSED && pcmPlayer != null) {
            pcmPlayer.resume();
            setState(State.PLAYING);
        }
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    public void awaitCompletion() throws InterruptedException {
        if (playerThread != null) {
            playerThread.join();
        }
    }

    public boolean awaitCompletion(long timeoutMs) throws InterruptedException {
        if (playerThread != null) {
            playerThread.join(timeoutMs);
            return !running.get();
        }
        return true;
    }

    // ==================== 状态查询 ====================

    public State getState() { return state; }
    public boolean isRunning() { return running.get(); }
    public long getTotalBytes() { return totalBytes.get(); }
    public long getChunkCount() { return chunkCount.get(); }

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public void setCallback(MimoTTSCallback callback) {
        this.callback = callback;
    }

    // ==================== 内部方法 ====================

    private void reset() {
        totalBytes.set(0);
        chunkCount.set(0);
        audioQueue.clear();
        textQueue.clear();
        state = State.IDLE;
    }

    /**
     * 启动合成线程（生产者）
     * 从 textQueue 取出文本，调用 API 合成音频，放入 audioQueue
     */
    private void startSynthesizeThread(boolean isVoiceDesign, MimoTTSCallback callback, int totalChunks) {
        synthesizeThread = new Thread(() -> {
            try {
                int sequenceIndex = 0;

                while (running.get()) {
                    // 从文本队列取出文本
                    String text = textQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (text == null) {
                        // 队列为空，继续等待
                        continue;
                    }

                    // 检查是否为结束标记
                    if (TEXT_END_MARKER.equals(text)) {
                        break;
                    }

                    // 检查音频队列大小，如果太多则暂停合成（背压控制）
                    while (running.get() && audioQueue.size() >= PREFETCH_QUEUE_SIZE) {
                        Thread.sleep(100);
                    }

                    if (!running.get()) break;

                    // 通知合成进度
                    if (listener != null) {
                        listener.onSynthesizeProgress(sequenceIndex + 1, totalChunks);
                    }

                    // 合成音频
                    byte[] audioData = synthesizeText(text, isVoiceDesign);
                    if (audioData != null && audioData.length > 0) {
                        audioQueue.offer(audioData);
                        sequenceIndex++;
                        LOG.debug("NonStreamTTS: Synthesized chunk " + sequenceIndex + ", size=" + audioData.length);

                        if (callback != null) {
                            callback.onAudioChunk(audioData, sequenceIndex);
                        }
                    }
                }

                // 合成完成，放入结束标记
                audioQueue.offer(END_MARKER);

                if (callback != null) {
                    callback.onComplete();
                }

            } catch (InterruptedException e) {
                LOG.info("NonStreamTTS: Synthesize thread interrupted");
            } catch (Exception e) {
                LOG.error("NonStreamTTS: Synthesize failed", e);
                if (listener != null) listener.onError(e);
                if (callback != null) callback.onError(e);
                setState(State.ERROR);
                // 放入结束标记以释放播放线程
                audioQueue.offer(END_MARKER);
            }
        }, "MimoTTS-NonStream-Synthesize");
        synthesizeThread.setDaemon(true);
        synthesizeThread.start();
    }

    /**
     * 启动播放线程（消费者）
     * 从 audioQueue 取出音频数据播放
     */
    private void startPlayerThread() {
        playerThread = new Thread(() -> {
            try {
                pcmPlayer = new PCMStreamPlayer();
                if (listener != null) listener.onStarted();

                setState(State.PLAYING);

                while (running.get()) {
                    // 从音频队列取出数据
                    byte[] audioChunk = audioQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (audioChunk == null) {
                        continue;  // 超时但未收到数据，继续等待
                    }

                    // 结束标记
                    if (audioChunk == END_MARKER) {
                        break;
                    }

                    // 再次检查是否停止
                    if (!running.get()) break;

                    // 播放音频（PCMStreamPlayer.write 内部会检查 playing 状态）
                    if (pcmPlayer != null && running.get()) {
                        pcmPlayer.write(audioChunk, 0, audioChunk.length);
                    }

                    // 播放后检查是否停止
                    if (!running.get()) break;

                    long currentTotal = totalBytes.addAndGet(audioChunk.length);
                    long currentCount = chunkCount.incrementAndGet();

                    if (listener != null) {
                        listener.onChunkPlayed(audioChunk, currentTotal, currentCount);
                    }
                }
            } catch (InterruptedException e) {
                // 被中断，正常退出
                LOG.info("NonStreamTTS: Player thread interrupted");
            } catch (Exception e) {
                LOG.error("NonStreamTTS: Player failed", e);
                if (listener != null) listener.onError(e);
                setState(State.ERROR);
            } finally {
                if (pcmPlayer != null) {
                    pcmPlayer.stop();
                    pcmPlayer = null;
                }
                if (state == State.PLAYING || state == State.SYNTHESIZING) {
                    setState(State.COMPLETED);
                    if (listener != null) listener.onCompleted(totalBytes.get(), 0);
                }
                running.set(false);
            }
        }, "MimoTTS-NonStream-Player");
        playerThread.setDaemon(true);
        playerThread.start();
    }

    /**
     * 调用 MiMo TTS API 合成文本
     *
     * @param text         要合成的文本
     * @param isVoiceDesign 是否为 VoiceDesign 模型
     * @return 音频数据（PCM16 格式）
     */
    private byte[] synthesizeText(String text, boolean isVoiceDesign) throws IOException, MimoTTSException {
        // 构建请求
        MimoTTSRequest request;
        if (isVoiceDesign) {
            request = MimoTTSRequest.ofVoiceDesign(text, voiceDescription)
                    .format(AudioFormat.WAV)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .voice(voice == null ? null : voice)
                    .build();
        } else {
            // 应用风格标签
            String finalText = text;
            if (style != null && !style.isEmpty() && !"默认".equals(style)) {
                if (!VoiceStyle.hasStyleTag(text)) {
                    finalText = VoiceStyle.wrapCustomText(style, text);
                }
            }

            request = MimoTTSRequest.ofPresetWithInstruction(finalText, this.voice, this.styleInstruction)
                    .voice(voice != null ? voice : config.getDefaultVoice())
                    .format(AudioFormat.WAV)
                    .temperature(config.getTemperature())
                    .topP(config.getTopP())
                    .build();
        }

        // 发送请求
        Map<String, Object> requestBody = request.toMap();
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        URL url = URI.create(config.getBaseUrl() + MimoTTSConfig.DEFAULT_SUFFIX_URL).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("api-key", config.getApiKey());
            connection.setDoOutput(true);
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new MimoTTSException("HTTP error: " + responseCode, responseCode);
            }

            // 解析响应
            return parseResponse(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 解析 API 响应，提取音频数据
     */
    private byte[] parseResponse(InputStream inputStream) throws IOException, MimoTTSException {
        String responseBody = readStream(inputStream);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode audioNode = root.path("choices").get(0).path("message").path("audio");
            String base64Audio = audioNode.path("data").asText();

            if (base64Audio == null || base64Audio.isEmpty()) {
                throw new MimoTTSException("No audio data in response");
            }

            return Base64.getDecoder().decode(base64Audio);
        } catch (Exception e) {
            throw new MimoTTSException("Failed to parse response", e);
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * 将文本分割成多个块，在自然边界处分割
     *
     * @param text 原始文本
     * @return 分割后的文本列表
     */
    private List<String> splitTextIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // 如果文本长度小于最大值，直接返回
        if (text.length() <= MAX_CHUNK_LENGTH) {
            chunks.add(text);
            return chunks;
        }

        int startPos = 0;
        while (startPos < text.length()) {
            int endPos = Math.min(startPos + MAX_CHUNK_LENGTH, text.length());

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

    private void setState(State newState) {
        State oldState = this.state;
        this.state = newState;
        if (listener != null && oldState != newState) {
            listener.onStateChanged(oldState, newState);
        }
    }
}
