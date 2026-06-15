package com.wei.wreader.tts.mimo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wei.wreader.tts.mimo.enums.AudioFormat;
import com.wei.wreader.tts.mimo.enums.Voice;
import com.wei.wreader.tts.mimo.enums.VoiceStyle;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * MiMo TTS v2.5 HTTP 客户端
 */
public class MimoTTSClient {

    private final MimoTTSConfig config;
    private final ObjectMapper objectMapper;

    public MimoTTSClient(MimoTTSConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public MimoTTSClient(String apiKey) {
        this(new MimoTTSConfig.Builder(apiKey).build());
    }

    // ==================== 非流式调用 ====================

    public MimoTTSResponse synthesize(MimoTTSRequest request) throws IOException, MimoTTSException {
        if (request.isStream()) {
            throw new MimoTTSException("Request has stream=true, use stream player instead");
        }
        String response = sendPostRequest(MimoTTSConfig.DEFAULT_SUFFIX_URL, request.toMap());
        return parseResponse(response);
    }

    public MimoTTSResponse synthesize(String text) throws IOException, MimoTTSException {
        MimoTTSRequest request = MimoTTSRequest.of(text)
                .voice(config.getDefaultVoice())
                .format(config.getDefaultFormat())
                .build();
        return synthesize(request);
    }

    public MimoTTSResponse synthesize(String text, Voice voice) throws IOException, MimoTTSException {
        MimoTTSRequest request = MimoTTSRequest.of(text)
                .voice(voice)
                .format(config.getDefaultFormat())
                .build();
        return synthesize(request);
    }

    public MimoTTSResponse synthesize(String text, VoiceStyle style) throws IOException, MimoTTSException {
        String styledText = style.wrapText(text);
        MimoTTSRequest request = MimoTTSRequest.of(styledText)
                .voice(config.getDefaultVoice())
                .format(config.getDefaultFormat())
                .build();
        return synthesize(request);
    }

    public MimoTTSResponse synthesize(String text, Voice voice, VoiceStyle style) throws IOException, MimoTTSException {
        String styledText = style.wrapText(text);
        MimoTTSRequest request = MimoTTSRequest.of(styledText)
                .voice(voice)
                .format(config.getDefaultFormat())
                .build();
        return synthesize(request);
    }

    public void synthesizeToFile(String text, String filePath) throws IOException, MimoTTSException {
        MimoTTSResponse response = synthesize(text);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(response.getAudioData());
        }
    }

    // ==================== 内部方法 ====================

    private String sendPostRequest(String endpoint, Object requestBody) throws IOException, MimoTTSException {
        URL url = new URL(config.getBaseUrl() + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

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
                throw new MimoTTSException("HTTP error: " + responseCode, responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    private MimoTTSResponse parseResponse(String responseBody) throws MimoTTSException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String id = root.path("id").asText();
            String model = root.path("model").asText();

            JsonNode audioNode = root.path("choices").get(0).path("message").path("audio");
            String base64Audio = audioNode.path("data").asText();
            if (base64Audio == null || base64Audio.isEmpty()) {
                throw new MimoTTSException("No audio data in response");
            }
            byte[] audioData = Base64.getDecoder().decode(base64Audio);

            MimoTTSResponse.Usage usage = null;
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                usage = new MimoTTSResponse.Usage(
                        usageNode.path("prompt_tokens").asInt(0),
                        usageNode.path("completion_tokens").asInt(0),
                        usageNode.path("total_tokens").asInt(0)
                );
            }

            return new MimoTTSResponse.Builder()
                    .id(id).model(model).audioData(audioData).usage(usage)
                    .build();
        } catch (IOException e) {
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

    public MimoTTSConfig getConfig() {
        return config;
    }
}
