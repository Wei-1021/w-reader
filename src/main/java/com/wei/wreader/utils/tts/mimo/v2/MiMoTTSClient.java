package com.wei.wreader.utils.tts.mimo.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wei.wreader.utils.tts.mimo.v2.enums.Voice;
import com.wei.wreader.utils.tts.mimo.v2.enums.VoiceStyle;
import com.wei.wreader.utils.tts.mimo.v2.exception.TTSException;
import com.wei.wreader.utils.tts.mimo.v2.player.MP3FilePlayer;
import com.wei.wreader.utils.tts.mimo.v2.player.StreamTTSPlayer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * MiMo TTS API 客户端
 */
public class MiMoTTSClient {

    private final TTSConfig config;
    private final ObjectMapper objectMapper;

    public MiMoTTSClient(TTSConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public MiMoTTSClient(String apiKey) {
        this(new TTSConfig.Builder(apiKey).build());
    }

    public static MiMoTTSClient fromEnv() {
        return new MiMoTTSClient(TTSConfig.fromEnv());
    }

    // ==================== 非流式调用 ====================

    public TTSResponse synthesize(TTSRequest request) throws IOException, TTSException {
        if (request.isStream()) {
            throw new TTSException("Request has stream=true, use synthesizeStream() instead");
        }
        String response = sendPostRequest(TTSConfig.DEFAULT_SUFFIX_URL, request.toMap());
        return parseResponse(response);
    }

    public TTSResponse synthesize(String text) throws IOException, TTSException {
        TTSRequest request = TTSRequest.of(text)
                .voice(config.getDefaultVoice())
                .format(config.getDefaultFormat())
                .build();
        return synthesize(request);
    }

    public TTSResponse synthesize(String text, Voice voice) throws IOException, TTSException {
        TTSRequest request = TTSRequest.of(text)
                .voice(voice)
                .format(config.getDefaultFormat())
                .build();
        return synthesize(request);
    }

    public TTSResponse synthesize(String text, VoiceStyle style) throws IOException, TTSException {
        TTSRequest request = TTSRequest.of(text)
                .voice(config.getDefaultVoice())
                .format(config.getDefaultFormat())
                .style(style)
                .build();
        return synthesize(request);
    }

    public TTSResponse synthesize(String text, Voice voice, VoiceStyle style)
            throws IOException, TTSException {
        TTSRequest request = TTSRequest.of(text)
                .voice(voice)
                .format(config.getDefaultFormat())
                .style(style)
                .build();
        return synthesize(request);
    }

    public TTSResponse synthesizeWithCustomStyle(String text, String customStyle)
            throws IOException, TTSException {
        TTSRequest request = TTSRequest.of(text)
                .voice(config.getDefaultVoice())
                .format(config.getDefaultFormat())
                .customStyle(customStyle)
                .build();
        return synthesize(request);
    }

    public TTSResponse synthesizeWithContext(String assistantText, String userText)
            throws IOException, TTSException {
        TTSRequest request = TTSRequest.of(assistantText)
                .addUserMessage(userText)
                .voice(config.getDefaultVoice())
                .format(config.getDefaultFormat())
                .build();
        return synthesize(request);
    }

    public void synthesizeToFile(String text, String filePath) throws IOException, TTSException {
        TTSResponse response = synthesize(text);
        saveToFile(response.getAudioData(), filePath);
    }

    // ==================== 流式播放（非阻塞） ====================

    /**
     * 创建流式播放器（非阻塞，立即返回）
     *
     * 返回 StreamTTSPlayer 对象，调用者可以：
     * - player.start(text) 启动播放
     * - player.stop() 随时停止
     * - player.getState() 查询状态
     * - player.awaitCompletion() 等待完成（可选）
     */
    public StreamTTSPlayer createStreamPlayer() {
        return new StreamTTSPlayer(config);
    }

    /**
     * 便捷方法：创建并启动流式播放（非阻塞）
     */
    public StreamTTSPlayer play(String text) throws TTSException {
        StreamTTSPlayer player = new StreamTTSPlayer(config);
        player.start(text);
        return player;
    }

    /**
     * 便捷方法：带风格的流式播放（非阻塞）
     */
    public StreamTTSPlayer play(String text, VoiceStyle style) throws TTSException {
        StreamTTSPlayer player = new StreamTTSPlayer(config);
        player.start(text, style.getValue());
        return player;
    }

    /**
     * 便捷方法：流式播放并保存文件（非阻塞）
     */
    public StreamTTSPlayer playAndSave(String text, String saveFilePath) throws TTSException {
        StreamTTSPlayer player = new StreamTTSPlayer(config);
        player.start(text, null, saveFilePath);
        return player;
    }

    /**
     * 便捷方法：流式播放 + 风格 + 保存（非阻塞）
     */
    public StreamTTSPlayer playAndSave(String text, VoiceStyle style, String saveFilePath)
            throws TTSException {
        StreamTTSPlayer player = new StreamTTSPlayer(config);
        player.start(text, style.getValue(), saveFilePath);
        return player;
    }

    // ==================== MP3 文件播放 ====================

    public void playMP3File(String filePath) throws Exception {
        MP3FilePlayer player = new MP3FilePlayer();
        player.play(filePath);
    }

    public void playMP3FileAsync(String filePath, MP3FilePlayer.PlaybackCompleteListener listener) {
        MP3FilePlayer player = new MP3FilePlayer();
        player.playAsync(filePath, listener);
    }

    // ==================== 文件操作 ====================

    public void saveToFile(byte[] audioData, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(audioData);
        }
    }

    // ==================== 内部方法 ====================

    private String sendPostRequest(String endpoint, Object requestBody)
            throws IOException, TTSException {
        HttpURLConnection connection = null;
//        URL url = new URL(config.getBaseUrl() + endpoint);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("api-key", config.getApiKey());
            connection.setDoOutput(true);
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readStream(connection.getInputStream());
            } else {
//                String errorBody = readStream(connection.getErrorStream());
//                JsonNode errorNode = objectMapper.readTree(errorBody);
//                String message = errorNode.path("error").path("message").asText("Unknown error");
//                String code = errorNode.path("error").path("code").asText(null);
                throw new TTSException("sendPostRequest TTSException", responseCode, "");
            }

        } finally {
            connection.disconnect();
        }
    }

    private TTSResponse parseResponse(String responseBody) throws TTSException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            String id = root.path("id").asText();
            String model = root.path("model").asText();

            JsonNode audioNode = root.path("choices").get(0)
                    .path("message").path("audio");
            String base64Audio = audioNode.path("data").asText();

            if (base64Audio == null || base64Audio.isEmpty()) {
                throw new TTSException("No audio data in response");
            }

            byte[] audioData = Base64.getDecoder().decode(base64Audio);

            TTSResponse.Usage usage = null;
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                usage = new TTSResponse.Usage(
                        usageNode.path("prompt_tokens").asInt(0),
                        usageNode.path("completion_tokens").asInt(0),
                        usageNode.path("total_tokens").asInt(0)
                );
            }

            return new TTSResponse.Builder()
                    .id(id)
                    .model(model)
                    .audioData(audioData)
                    .usage(usage)
                    .build();

        } catch (IOException e) {
            throw new TTSException("Failed to parse response", e);
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

    public TTSConfig getConfig() {
        return config;
    }
}
