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
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MiMo TTS v2.5 流式 TTS 播放控制器
 *
 * 特点：
 * 1. 非阻塞 - start() 立即返回，播放在后台进行
 * 2. 可控制 - stop() 随时中断
 * 3. 边收边播 - 音频块到达即播放
 * 4. 可选保存 - 同时保存到文件
 */
public class StreamTTSPlayer {
    private static final Logger LOG = Logger.getInstance(StreamTTSPlayer.class);

    public enum State {
        IDLE, PLAYING, STOPPED, COMPLETED, ERROR
    }

    private final MimoTTSConfig config;
    private final ObjectMapper objectMapper;

    private volatile State state = State.IDLE;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong chunkCount = new AtomicLong(0);

    private Thread networkThread;
    private Thread playerThread;
    private PCMStreamPlayer pcmPlayer;

    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private static final byte[] END_MARKER = new byte[0];

    private FileOutputStream fileOutputStream;
    private PlayerListener listener;

    public interface PlayerListener {
        default void onStarted() {}
        default void onChunkPlayed(byte[] data, long totalBytes, long chunkCount) {}
        default void onCompleted(long totalBytes, long durationMs) {}
        default void onError(Exception e) {}
        default void onStateChanged(State oldState, State newState) {}
    }

    public StreamTTSPlayer(MimoTTSConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 启动播放 ====================

    public void start(String text) throws MimoTTSException {
        start(text, null, null, null, null);
    }

    public void start(String text, MimoTTSCallback callback) throws MimoTTSException {
        start(text, null, null, null, callback);
    }

    public void start(String text, String style) throws MimoTTSException {
        start(text, style, null, null, null);
    }

    public void start(String text, String style, String saveFilePath) throws MimoTTSException {
        start(text, style, saveFilePath, null, null);
    }

    public void start(String text, String style, String saveFilePath, PlayerListener listener) throws MimoTTSException {
        start(text, style, saveFilePath, listener, null);
    }

    /**
     * 使用 VoiceDesign 模型播放
     * @param voiceDescription (风格指令)音色描述（user 消息）
     * @param voice 音色
     * @param text 要合成的文本（assistant 消息）
     */
    public void startPresetWithInstruction(String voiceDescription, Voice voice, String text) throws MimoTTSException {
        if (running.get()) {
            throw new MimoTTSException("Player is already running, stop it first");
        }

        this.totalBytes.set(0);
        this.chunkCount.set(0);
        this.audioQueue.clear();

        // 构建 VoiceDesign 请求
        MimoTTSRequest request = MimoTTSRequest.ofPresetWithInstruction(text, voice, voiceDescription)
                .format(AudioFormat.PCM16)
                .temperature(config.getTemperature())
                .topP(config.getTopP())
                .stream(true)
                .build();

        running.set(true);
        setState(State.PLAYING);

        startPlayerThread();
        startNetworkThread(request, null);
    }

    /**
     * 使用 VoiceDesign 模型播放
     * @param voiceDescription 音色描述（user 消息）
     * @param text 要合成的文本（assistant 消息）
     */
    public void startVoiceDesign(String voiceDescription, String text) throws MimoTTSException {
        if (running.get()) {
            throw new MimoTTSException("Player is already running, stop it first");
        }

        this.totalBytes.set(0);
        this.chunkCount.set(0);
        this.audioQueue.clear();

        // 构建 VoiceDesign 请求
        MimoTTSRequest request = MimoTTSRequest.ofVoiceDesign(text, voiceDescription)
                .format(AudioFormat.PCM16)
                .temperature(config.getTemperature())
                .topP(config.getTopP())
                .stream(true)
                .build();

        running.set(true);
        setState(State.PLAYING);

        startPlayerThread();
        startNetworkThread(request, null);
    }

    public void start(String text, String style, String saveFilePath, PlayerListener listener, MimoTTSCallback callback)
            throws MimoTTSException {
        if (running.get()) {
            throw new MimoTTSException("Player is already running, stop it first");
        }

        this.listener = listener;
        this.totalBytes.set(0);
        this.chunkCount.set(0);
        this.audioQueue.clear();

        // 应用风格标签到文本
        String finalText = text;
        if (style != null && !style.isEmpty() && !"默认".equals(style)) {
            if (!VoiceStyle.hasStyleTag(text)) {
                finalText = VoiceStyle.wrapCustomText(style, text);
            }
        }

        // 构建请求
        MimoTTSRequest request = MimoTTSRequest.of(finalText)
                .voice(config.getDefaultVoice())
                .format(AudioFormat.PCM16)
                .temperature(config.getTemperature())
                .topP(config.getTopP())
                .build();

        // 打开文件输出（如果需要）
        if (saveFilePath != null && !saveFilePath.isEmpty()) {
            try {
                fileOutputStream = new FileOutputStream(saveFilePath);
            } catch (IOException e) {
                throw new MimoTTSException("Failed to open save file: " + saveFilePath, e);
            }
        }

        running.set(true);
        setState(State.PLAYING);

        startPlayerThread();
        startNetworkThread(request, callback);
    }

    // ==================== 控制方法 ====================

    public void stop() {
        if (!running.get()) return;

        running.set(false);

        if (networkThread != null) {
            networkThread.interrupt();
        }

        audioQueue.clear();
        audioQueue.offer(END_MARKER);

        if (pcmPlayer != null) {
            pcmPlayer.stop();
            pcmPlayer = null;
        }

        closeFileOutput();
        setState(State.STOPPED);
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

    // ==================== 内部方法 ====================

    private void startPlayerThread() {
        playerThread = new Thread(() -> {
            try {
                pcmPlayer = new PCMStreamPlayer();
                if (listener != null) listener.onStarted();

                while (running.get()) {
                    // 使用带超时的阻塞等待，避免空转
                    byte[] audioChunk = audioQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (audioChunk == null) {
                        continue;  // 超时但未收到数据，继续等待
                    }
                    if (audioChunk == END_MARKER) break;

                    // 等待播放器就绪，避免丢弃音频块
                    while (running.get() && (pcmPlayer == null || !pcmPlayer.isPlaying())) {
                        Thread.sleep(10);
                    }
                    
                    if (!running.get()) break;

                    if (pcmPlayer != null) {
                        pcmPlayer.write(audioChunk, 0, audioChunk.length);
                    }

                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.write(audioChunk);
                        } catch (IOException e) {
                            LOG.warn("Failed to write audio chunk to file", e);
                        }
                    }

                    long currentTotal = totalBytes.addAndGet(audioChunk.length);
                    long currentCount = chunkCount.incrementAndGet();

                    if (listener != null) {
                        listener.onChunkPlayed(audioChunk, currentTotal, currentCount);
                    }
                }
            } catch (InterruptedException e) {
                // 被中断，正常退出
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
                setState(State.ERROR);
            } finally {
                if (pcmPlayer != null) {
                    pcmPlayer.stop();
                    pcmPlayer = null;
                }
                closeFileOutput();
                if (state == State.PLAYING) {
                    setState(State.COMPLETED);
                    if (listener != null) listener.onCompleted(totalBytes.get(), 0);
                }
                running.set(false);
            }
        }, "MimoTTS-Player");
        playerThread.setDaemon(true);
        playerThread.start();
    }

    private void startNetworkThread(MimoTTSRequest request, MimoTTSCallback callback) {
        networkThread = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = URI.create(config.getBaseUrl() + MimoTTSConfig.DEFAULT_SUFFIX_URL).toURL();
                connection = (HttpURLConnection) url.openConnection();

                Map<String, Object> requestBody = request.toMap();
                requestBody.put("stream", true);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("api-key", config.getApiKey());
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setDoOutput(true);
                connection.setConnectTimeout(config.getConnectTimeout());
                connection.setReadTimeout(config.getReadTimeout());

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new MimoTTSException("HTTP error: " + responseCode, responseCode);
                }

                parseSSEStream(connection.getInputStream(), callback);

            } catch (InterruptedException e) {
                if (callback != null) callback.onError(e);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
                if (callback != null) callback.onError(e);
                setState(State.ERROR);
            } finally {
                if (connection != null) connection.disconnect();
                audioQueue.offer(END_MARKER);
            }
        }, "MimoTTS-Network");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    private void parseSSEStream(InputStream inputStream, MimoTTSCallback callback) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            String line;
            int sequenceIndex = 0;

            while (running.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (!line.startsWith("data: ")) continue;

                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    if (callback != null) callback.onComplete();
                    return;
                }

                try {
                    JsonNode node = objectMapper.readTree(data);
                    JsonNode choicesNode = node.path("choices");
                    if (choicesNode.isMissingNode() || !choicesNode.isArray() || choicesNode.isEmpty()) continue;

                    JsonNode messageNode = choicesNode.get(0).path("delta");
                    if (messageNode.isMissingNode()) continue;

                    JsonNode audioNode = messageNode.path("audio");
                    if (audioNode.isMissingNode()) continue;

                    JsonNode dataNode = audioNode.path("data");
                    if (dataNode.isMissingNode() || dataNode.isNull()) continue;

                    String base64Audio = dataNode.asText();
                    if (base64Audio == null || base64Audio.isEmpty()) continue;

                    byte[] audioChunk = Base64.getDecoder().decode(base64Audio);
                    if (audioChunk.length > 0) {
                        audioQueue.offer(audioChunk);
                        sequenceIndex++;
                        if (callback != null) callback.onAudioChunk(audioChunk, sequenceIndex);
                    }
                } catch (Exception e) {
                    System.err.println("[MimoTTS] Failed to parse SSE data at sequence " + sequenceIndex + ": " + e.getMessage());
                    if (callback != null) callback.onError(e);
                }
            }
            if (callback != null) callback.onComplete();
        } finally {
            try { reader.close(); } catch (IOException ignored) {}
        }
    }

    private void setState(State newState) {
        State oldState = this.state;
        this.state = newState;
        if (listener != null && oldState != newState) {
            listener.onStateChanged(oldState, newState);
        }
    }

    private void closeFileOutput() {
        if (fileOutputStream != null) {
            try { fileOutputStream.close(); } catch (IOException ignored) {}
            fileOutputStream = null;
        }
    }
}
