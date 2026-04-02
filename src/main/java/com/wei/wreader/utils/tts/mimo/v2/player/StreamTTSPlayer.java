package com.wei.wreader.utils.tts.mimo.v2.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wei.wreader.utils.tts.mimo.v2.StreamCallback;
import com.wei.wreader.utils.tts.mimo.v2.TTSConfig;
import com.wei.wreader.utils.tts.mimo.v2.TTSRequest;
import com.wei.wreader.utils.tts.mimo.v2.enums.AudioFormat;
import com.wei.wreader.utils.tts.mimo.v2.exception.TTSException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流式 TTS 播放控制器
 *
 * 特点：
 * 1. 非阻塞 —— start() 立即返回，播放在后台进行
 * 2. 可控制 —— stop() 随时中断
 * 3. 边收边播 —— 音频块到达即播放
 * 4. 可选保存 —— 同时保存到文件
 */
public class StreamTTSPlayer {

    // 状态枚举
    public enum State {
        IDLE,       // 空闲
        PLAYING,    // 正在播放
        STOPPED,    // 已停止
        COMPLETED,  // 正常完成
        ERROR       // 出错
    }

    private final TTSConfig config;
    private final ObjectMapper objectMapper;

    private volatile State state = State.IDLE;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong chunkCount = new AtomicLong(0);

    private Thread networkThread;
    private Thread playerThread;
    private PCMStreamPlayer pcmPlayer;

    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private static final byte[] END_MARKER = new byte[0]; // 结束标记

    private FileOutputStream fileOutputStream; // 可选的文件输出

    private PlayerListener listener;

    /**
     * 播放器监听器
     */
    public interface PlayerListener {
        /** 开始播放 */
        default void onStarted() {}
        /** 收到音频块并播放 */
        default void onChunkPlayed(byte[] data, long totalBytes, long chunkCount) {}
        /** 播放完成 */
        default void onCompleted(long totalBytes, long durationMs) {}
        /** 播放出错 */
        default void onError(Exception e) {}
        /** 状态变更 */
        default void onStateChanged(State oldState, State newState) {}
    }

    public StreamTTSPlayer(TTSConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 启动播放 ====================

    /**
     * 启动流式播放（立即返回，不阻塞）
     */
    public void start(String text) throws TTSException {
        start(text, null, null, null, null);
    }
    /**
     * 启动流式播放（立即返回，不阻塞）
     */
    public void start(String text, StreamCallback callback) throws TTSException {
        start(text, null, null, null, callback);
    }

    /**
     * 启动流式播放（带风格）
     */
    public void start(String text, String style) throws TTSException {
        start(text, style, null, null, null);
    }

    /**
     * 启动流式播放（带风格 + 保存文件）
     */
    public void start(String text, String style, String saveFilePath) throws TTSException {
        start(text, style, saveFilePath, null, null);
    }

    /**
     * 启动流式播放（完整参数）
     */
    public void start(String text, String style, String saveFilePath, PlayerListener listener) throws TTSException {
        start(text, style, saveFilePath, listener, null);
    }
    /**
     * 启动流式播放（完整参数）
     */
    public void start(String text, String style, String saveFilePath, PlayerListener listener, StreamCallback callback)
            throws TTSException {

        if (running.get()) {
            throw new TTSException("Player is already running, stop it first");
        }

        this.listener = listener;
        this.totalBytes.set(0);
        this.chunkCount.set(0);
        this.audioQueue.clear();

        // 构建请求
        TTSRequest.Builder builder = TTSRequest.of(text)
                .voice(config.getDefaultVoice())
                .format(AudioFormat.PCM16);

        if (style != null && !style.isEmpty()) {
            builder.customStyle(style);
        }

        TTSRequest request = builder.build();

        // 打开文件输出（如果需要）
        if (saveFilePath != null && !saveFilePath.isEmpty()) {
            try {
                fileOutputStream = new FileOutputStream(saveFilePath);
            } catch (IOException e) {
                throw new TTSException("Failed to open save file: " + saveFilePath, e);
            }
        }

        // 启动
        running.set(true);
        setState(State.PLAYING);

        // 启动播放线程
        startPlayerThread();

        // 启动网络线程
        startNetworkThread(request);
    }

    /**
     * 使用 Request 对象启动
     */
    public void start(TTSRequest request, PlayerListener listener) throws TTSException {
        if (running.get()) {
            throw new TTSException("Player is already running, stop it first");
        }

        this.listener = listener;
        this.totalBytes.set(0);
        this.chunkCount.set(0);
        this.audioQueue.clear();

        running.set(true);
        setState(State.PLAYING);

        startPlayerThread();
        startNetworkThread(request);
    }

    // ==================== 控制方法 ====================

    /**
     * 停止播放（立即返回）
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        running.set(false);

        // 中断网络线程
        if (networkThread != null) {
            networkThread.interrupt();
        }

        // 发送结束标记唤醒播放线程
        audioQueue.clear();
        audioQueue.offer(END_MARKER);

        // 停止 PCM 播放器
        if (pcmPlayer != null) {
            pcmPlayer.stop();
            pcmPlayer = null;
        }

        // 关闭文件输出
        closeFileOutput();

        setState(State.STOPPED);
    }

    /**
     * 等待播放完成（可选，需要时调用）
     */
    public void awaitCompletion() throws InterruptedException {
        if (playerThread != null) {
            playerThread.join();
        }
    }

    /**
     * 等待播放完成（带超时）
     */
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
    public void running() {
        running.set(true);
    }

    // ==================== 内部方法 ====================

    /**
     * 启动播放线程
     */
    private void startPlayerThread() {
        playerThread = new Thread(() -> {
            try {
                pcmPlayer = new PCMStreamPlayer();

                if (listener != null) {
                    listener.onStarted();
                }

                while (running.get()) {
                    // 从队列取音频块，最多等 100ms
                    byte[] audioChunk = audioQueue.poll();

                    if (audioChunk == null) {
                        // 队列为空，短暂休眠后继续
                        Thread.sleep(100);
                        continue;
                    }

                    // 结束标记
                    if (audioChunk == END_MARKER) {
                        break;
                    }

                    // 播放
                    if (pcmPlayer != null && pcmPlayer.isPlaying()) {
                        pcmPlayer.write(audioChunk, 0, audioChunk.length);
                    }

                    // 写入文件
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.write(audioChunk);
                        } catch (IOException e) {
                            // 文件写入失败不影响播放
                            e.printStackTrace();
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
                if (listener != null) {
                    listener.onError(e);
                }
                setState(State.ERROR);
            } finally {
                // 停止播放器
                if (pcmPlayer != null) {
                    pcmPlayer.stop();
                    pcmPlayer = null;
                }

                closeFileOutput();

                if (state == State.PLAYING) {
                    setState(State.COMPLETED);
                    if (listener != null) {
                        listener.onCompleted(totalBytes.get(), 0);
                    }
                }

                running.set(false);
            }
        }, "TTS-Player");

        playerThread.setDaemon(true);
        playerThread.start();
    }

    /**
     * 启动网络线程
     */
    private void startNetworkThread(TTSRequest request) {
        networkThread = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Map<String, Object> requestBody = request.toMap();
                requestBody.put("stream", true);
                requestBody.put("temperature", config.getTemperature());
                requestBody.put("top_p", config.getTopP());

//                URL url = new URL(config.getBaseUrl() + TTSConfig.DEFAULT_SUFFIX_URL);
//                connection = (HttpURLConnection) url.openConnection();

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
//                    String errorBody = readStream(connection.getErrorStream());
//                    JsonNode errorNode = objectMapper.readTree(errorBody);
//                    String message = errorNode.path("error").path("message").asText("Unknown error");
//                    throw new TTSException(message, responseCode);
                }

                // 解析 SSE 流
                parseSSEStream(connection.getInputStream());

            } catch (InterruptedException e) {
                // 被中断，正常退出
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
                setState(State.ERROR);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                // 无论成功失败，发送结束标记
                audioQueue.offer(END_MARKER);
            }
        }, "TTS-Network");

        networkThread.setDaemon(true);
        networkThread.start();
    }

    /**
     * SSE 流解析
     */
    private void parseSSEStream(InputStream inputStream) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try {
            String line;
            int sequenceIndex = 0; // 用于调试，记录解析的序号

            int i = 0;
            while (running.get() && (line = reader.readLine()) != null) {
                i++;
                line = line.trim();

                System.out.println(i + " SSE: " + line);

                // 跳过空行（SSE 事件分隔符）
                if (line.isEmpty()) {
                    continue;
                }

                // 只处理 data: 开头的行
                if (!line.startsWith("data: ")) {
                    continue;
                }

                String data = line.substring(6).trim();

                // 结束标记
                if ("[DONE]".equals(data)) {
                    return;
                }

                // 解析 JSON
                try {
                    JsonNode node = objectMapper.readTree(data);

                    // 提取音频数据
                    JsonNode choicesNode = node.path("choices");
                    if (choicesNode.isMissingNode() || !choicesNode.isArray() || choicesNode.isEmpty()) {
                        continue;
                    }

                    JsonNode messageNode = choicesNode.get(0).path("delta");
                    if (messageNode.isMissingNode()) {
                        continue;
                    }

                    JsonNode audioNode = messageNode.path("audio");
                    if (audioNode.isMissingNode()) {
                        continue;
                    }

                    JsonNode dataNode = audioNode.path("data");
                    if (dataNode.isMissingNode() || dataNode.isNull()) {
                        continue;
                    }

                    String base64Audio = dataNode.asText();
                    if (base64Audio == null || base64Audio.isEmpty()) {
                        continue;
                    }

                    // 解码并放入队列
                    byte[] audioChunk = Base64.getDecoder().decode(base64Audio);
                    if (audioChunk.length > 0) {
                        System.out.println(i + " Audio chunk size: " + audioChunk.length);
                        audioQueue.offer(audioChunk);
                        sequenceIndex++;
                    }

                } catch (Exception e) {
                    // 解析失败，记录日志但继续处理后续数据
                    System.err.println("[TTS] Failed to parse SSE data at sequence "
                            + sequenceIndex + ": " + e.getMessage());
                    // 不跳过，继续处理下一行
                }
            }
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * 启动网络线程
     */
    public void startNetworkThread(TTSRequest request, StreamCallback callback) {
        networkThread = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Map<String, Object> requestBody = request.toMap();
                requestBody.put("stream", true);
                requestBody.put("temperature", config.getTemperature());
                requestBody.put("top_p", config.getTopP());

//                URL url = new URL(config.getBaseUrl() + TTSConfig.DEFAULT_SUFFIX_URL);
//                connection = (HttpURLConnection) url.openConnection();

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
//                    String errorBody = readStream(connection.getErrorStream());
//                    JsonNode errorNode = objectMapper.readTree(errorBody);
//                    String message = errorNode.path("error").path("message").asText("Unknown error");
//                    throw new TTSException(message, responseCode);
                }

                // 解析 SSE 流
                parseSSEStream(connection.getInputStream(), callback);

            } catch (InterruptedException e) {
                // 被中断，正常退出
                if (callback != null) {
                    callback.onError(e);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
                if (callback != null) {
                    callback.onError(e);
                }
                setState(State.ERROR);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                // 无论成功失败，发送结束标记
                audioQueue.offer(END_MARKER);
            }
        }, "TTS-Network");

        networkThread.setDaemon(true);
        networkThread.start();
    }

    /**
     * SSE 流解析
     */
    private void parseSSEStream(InputStream inputStream, StreamCallback callback) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try {
            String line;
            int sequenceIndex = 0; // 用于调试，记录解析的序号

            while (running.get() && (line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过空行（SSE 事件分隔符）
                if (line.isEmpty()) {
                    continue;
                }

                // 只处理 data: 开头的行
                if (!line.startsWith("data: ")) {
                    continue;
                }

                String data = line.substring(6).trim();

                // 结束标记
                if ("[DONE]".equals(data)) {
                    if (callback != null) {
                        callback.onComplete();
                    }
                    return;
                }

                // 解析 JSON
                try {
                    JsonNode node = objectMapper.readTree(data);

                    // 提取音频数据
                    JsonNode choicesNode = node.path("choices");
                    if (choicesNode.isMissingNode() || !choicesNode.isArray() || choicesNode.isEmpty()) {
                        continue;
                    }

                    JsonNode messageNode = choicesNode.get(0).path("delta");
                    if (messageNode.isMissingNode()) {
                        continue;
                    }

                    JsonNode audioNode = messageNode.path("audio");
                    if (audioNode.isMissingNode()) {
                        continue;
                    }

                    JsonNode dataNode = audioNode.path("data");
                    if (dataNode.isMissingNode() || dataNode.isNull()) {
                        continue;
                    }

                    String base64Audio = dataNode.asText();
                    if (base64Audio == null || base64Audio.isEmpty()) {
                        continue;
                    }

                    // 解码并放入队列
                    byte[] audioChunk = Base64.getDecoder().decode(base64Audio);
                    if (audioChunk.length > 0) {
                        audioQueue.offer(audioChunk);
                        sequenceIndex++;
                        if (callback != null) {
                            callback.onAudioChunk(audioChunk, sequenceIndex);
                        }
                    }

                } catch (Exception e) {
                    // 解析失败，记录日志但继续处理后续数据
                    System.err.println("[TTS] Failed to parse SSE data at sequence "
                            + sequenceIndex + ": " + e.getMessage());
                    // 不跳过，继续处理下一行
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            }

            if (callback != null) {
                callback.onComplete();
            }
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
                if (callback != null) {
                    callback.onError(ignored);
                }
            }
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
            try {
                fileOutputStream.close();
            } catch (IOException ignored) {}
            fileOutputStream = null;
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
