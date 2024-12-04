package com.wei.wreader.utils.tts;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
import org.apache.commons.lang3.StringUtils;
import org.dreamwork.tools.tts.ITTSListener;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class EdgeTTS {

    /**
     * 音频流开始传输标记
     */
    private static final String TURN_START = "turn.start";
    /**
     * 音频流结束传输标记
     */
    private static final String TURN_END = "turn.end";
    private static final String WS_URL = "wss://eastasia.api.speech.microsoft.com/cognitiveservices/websocket/v1";
    private static final String WS_ORIGIN = "https://speech.microsoft.com";
    private static final String WS_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.44";
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_WS_CONNECTIONS = 5;
    private final ExecutorService executor;
    /**
     * TTS Listener
     */
    private volatile ITTSListener listener;

    /**
     * 音色
     */
    private VoiceRole voiceRole;
    /**
     * 风格
     */
    private String style;
    /**
     * 音量
     */
    private String volume;
    /**
     * 语速
     */
    private String rate;
    private static WebSocketClient client;
    private static AdvancedPlayer player;
    private final PipedInputStream pis;
    private final PipedOutputStream pos;
    /**
     * 文本队列
     */
    private final Queue<String> textQueue = new LinkedList<>();
    /**
     * 用于存储合成音频数据的 CompletableFuture
     */
    private CompletableFuture<List<byte[]>> result;
    /**
     * 用于存储所有合成音频数据的字节数组队列
     */
    BlockingQueue<byte[]> allAudioData = new LinkedBlockingQueue<>();
    /**
     * 当前正在合成的文本
     */
    private String currentContent;
    /**
     * 当前文字是否已经发送
     */
    private boolean isCurrentContentSend = false;
    private int connectionCount = 0;
    /**
     * 正在合成
     */
    private boolean synthesising = false;
    /**
     * 是否正在播放
     */
    private boolean isPlaying = false;
    /**
     * 是否为当前对话的最后一条消息
     */
    private boolean isTempLastMsg = false;
    /**
     * 是否销毁
     */
    private boolean isDispose = false;


    private static EdgeTTS instance;

    /**
     * 单例
     */
    public static EdgeTTS getInstance() throws IOException {
        if (instance == null) {
            instance = new EdgeTTS();
        }
        return instance;
    }

    public EdgeTTS() throws IOException {
        pos = new PipedOutputStream();
        pis = new PipedInputStream(pos);
        executor = Executors.newFixedThreadPool(3);
    }

    /**
     * 获取音量
     *
     * @return
     */
    public String getVolume() {
        return volume;
    }

    /**
     * 设置音量
     *
     * @param volume
     */
    public EdgeTTS setVolume(String volume) {
        this.volume = volume;
        return this;
    }

    /**
     * 获取音色
     *
     * @return
     */
    public VoiceRole getVoiceRole() {
        return voiceRole;
    }

    /**
     * 设置音色
     *
     * @param voiceRole
     */
    public EdgeTTS setVoiceRole(VoiceRole voiceRole) {
        this.voiceRole = voiceRole;
        return this;
    }

    /**
     * 获取语音风格
     * @return
     */
    public String getStyle() {
        return style;
    }

    /**
     * 根据VoiceStyle的name属性设置语音风格
     * @param name
     * @return
     */
    public EdgeTTS setStyleName(String name) {
        this.style = VoiceStyle.getByName(name).value;
        return this;
    }

    /**
     * 根据VoiceStyle的value属性设置语音风格
     * @param value
     * @return
     */
    public EdgeTTS setStyleValue(String value) {
        this.style = VoiceStyle.getByValue(value).value;
        return this;
    }

    /**
     * 获取语速
     *
     * @return
     */
    public String getRate() {
        return rate;
    }

    /**
     * 设置语速
     *
     * @param rate
     */
    public EdgeTTS setRate(String rate) {
        this.rate = rate;
        return this;
    }

    public void setListener(ITTSListener listener) {
        this.listener = listener;
    }

    /**
     * 合成给定的文本并返回音频数据
     *
     * @param text 要合成的文本
     * @return 包含合成音频数据的字节数组列表的 CompletableFuture
     */
    public void synthesize(String text) {
        // 将文本按最大长度切分并加入队列
        splitTextIntoQueue(text);
        // 文字队列末尾添加上一个临时消息，用于处理最后一部分文字合成的问题
        textQueue.offer("<break time=\"1500ms\"/>");
        // 发送SSML消息
        synthesising = true;
        isTempLastMsg = true;
    }

    /**
     * 开始合成
     */
    public void start() {
        executor.submit(this::sendSSMLMsg);
        executor.submit(this::copyByteToOut);
        executor.submit(this::play);
        executor.shutdown();
    }

    /**
     * 将给定的文本分割成多个部分，并将这些部分加入到队列中
     *
     * @param text 要分割的文本
     */
    private void splitTextIntoQueue(String text) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_TEXT_LENGTH, text.length());
            // 尝试在标点或空格处断句
            if (end < text.length()) {
                int lastPunctuation = findLastPunctuation(text.substring(start, end));
                if (lastPunctuation != -1) {
                    end = start + lastPunctuation + 1;
                }
            }
            String handlerText = textSSMLHandler(text.substring(start, end));
            textQueue.offer(handlerText);
            start = end;
        }
    }

    /**
     * 文本处理，对文本进行特殊处理；如：省略号后增加停顿，以实现更自然的播放
     *
     * @param text
     */
    private String textSSMLHandler(String text) {
        return replaceEllipsis(text);
    }

    /**
     * 替换中文省略号或至少三个英文句号为停顿
     * @param input
     * @return
     */
    private static String replaceEllipsis(String input) {
        // 使用正则表达式匹配中文省略号或至少三个英文句号
        return input.replaceAll("[…]{1,}|\\.{3,}", "<break time=\"800ms\"/>");
    }

    /**
     * 在给定的字符串中找到最后一个标点符号的位置
     *
     * @param text 要搜索的字符串
     * @return 最后一个标点符号的位置，如果没有找到标点符号，则返回-1
     */
    private int findLastPunctuation(String text) {
        int lastIndex = -1;
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '。' || c == '!' || c == '！' || c == '?' || c == '？' || c == ',' || c == '，' || c == ' ') {
                lastIndex = i;
                break;
            }
        }
        return lastIndex;
    }

    /**
     * 发送配置消息
     */
    private void sendVoiceFormat() {
        String audioConfig = VoiceFormat.asJson(VoiceFormat.audio_24khz_48kbitrate_mono_mp3);
        client.send(audioConfig);
    }

    /**
     * 发送SSML消息
     */
    private void sendSSMLMsg() {
        // 当文本队列不为空且不为销毁状态，循环发送消息
        while (!textQueue.isEmpty() && !isDispose) {
            // 如果上一个消息接收完毕，则从队列中取出下一个消息发送，否则等待
            if (isTempLastMsg) {
                // 上一个消息成功发送或者还未发送过消息，则从队列中取出下一个消息，否则继续发送上条消息
                if (isCurrentContentSend || StringUtils.isBlank(currentContent)) {
                    currentContent = textQueue.poll();
                    isCurrentContentSend = false;
                }

                if (StringUtils.isNotBlank(currentContent)) {
                    // 发送SSML消息
                    SSMLPayload ssmlPayload = new SSMLPayload(voiceRole, rate, volume, style);
                    ssmlPayload.content = currentContent;
                    String messageSSML = ssmlPayload.toString();
                    processQueue();
                    isCurrentContentSend = true;
                    client.send(messageSSML);
                    isTempLastMsg = false;
                }
            }

            // 循环间隔100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理文本队列，通过 WebSocket 连接发送文本到 Microsoft Edge 的 TTS 服务进行语音合成，并将合成的音频数据以字节数组的形式返回
     */
    private void processQueue() {
        if (isDispose) {
            return;
        }

        if (connectionCount >= MAX_WS_CONNECTIONS) {
            // 重置连接计数并重新创建WebSocket连接
            connectionCount = 0;
            if (client != null) {
                client.close(CloseFrame.NORMAL, "bye!");
                client = null;
            }
        }

        if (client == null) {
            Map<String, String> header = new HashMap<>();
            header.put("User-Agent", WS_UA);
            header.put("Origin", WS_ORIGIN);

            String requestId = UUID.randomUUID().toString().replace("-", "");
            String wsUrl = WS_URL + "?X-ConnectionId=" + requestId;

            try {
                client = new WebSocketClient(URI.create(wsUrl), header) {

                    @Override
                    public void onOpen(ServerHandshake handshake) {
                    }

                    @Override
                    public void onMessage(String message) {
                        // 当接收到文本数据中包含turn.start时，代表音频流开始传输
                        if (message.contains(TURN_START)) {
                            // 开始合成一段文本，触发监听器
                            if (listener != null) {
                                listener.started(currentContent);
                            }
                        }
                        // 当接收到文本数据中包含turn.end时，代表音频流传输结束
                        else if (message.contains(TURN_END)) {
                            // 一段文本合成完成
                            // 一段解码结束
                            isTempLastMsg = true;
                        }
                    }

                    @Override
                    public void onMessage(ByteBuffer data) {
                        // 处理数据
                        String line;
                        while (!(line = readLine(data)).isEmpty()) {
                            if ("Path:audio".equals(line.trim())) {
                                break;
                            }
                        }

                        // 获取实际剩余的数据长度
                        int remaining = data.remaining();
                        byte[] datas = new byte[remaining];
                        // 将 ByteBuffer 中的数据复制到 byte 数组中
                        data.get(datas);
                        allAudioData.offer(datas);
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        connectionCount = 0;
                        client = null;
                        isTempLastMsg = true;
                        if (code != CloseFrame.NORMAL) {
                            isCurrentContentSend = false;
                            if (textQueue.isEmpty()) {
                                textQueue.offer(currentContent);
                            }
                        } else {
                            synthesising = false;
                        }
                    }

                    @Override
                    public void onError(Exception ex) {
                        ex.printStackTrace();
                    }
                };
                client.connectBlocking();
                connectionCount++;
                // 发送配置消息
                sendVoiceFormat();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 从 ByteBuffer 中读取一行数据
     * @param buffer
     * @return
     */
    private String readLine(ByteBuffer buffer) {
        byte[] target = new byte[128];

        int index = 0, remains = buffer.remaining();
        if (remains == 0) {
            return "";
        }
        while (index < remains) {
            if (index >= target.length) {
                byte[] temp = new byte[target.length << 1];
                System.arraycopy(target, 0, temp, 0, target.length);
                target = temp;
            }
            target[index] = buffer.get();
            if (target[index++] == '\n') {
                break;
            }
        }
        return new String(target, 0, index, StandardCharsets.UTF_8);
    }

    /**
     * 线程从音频数据队列中读取数据，并将其写入到输出流中
     */
    private void copyByteToOut() {
        try {
            while (!allAudioData.isEmpty() || !textQueue.isEmpty()) {
                byte[] audioData = allAudioData.take();
                if (isPlaying && (allAudioData == null || allAudioData.isEmpty() || audioData.length == 0)) {
                    continue;
                }

                pos.write(audioData);
                pos.flush();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 播放音频
     */
    private void play() {
        try {
            if (isDispose) {
                return;
            }

            player = new AdvancedPlayer(pis);
            player.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackStarted(PlaybackEvent evt) {
                    isPlaying = true;
                }

                @Override
                public void playbackFinished(PlaybackEvent evt) {
//                    isPlaying = false;
                }
            });
            player.play();
        } catch (JavaLayerException e) {
            e.printStackTrace();
        } finally {
            stopPlaying();
        }
    }

    public void stopPlaying() {
        isPlaying = false;
    }

    public void dispose() {
        try {
            isDispose = true;

            if (client != null) {
                client.close(CloseFrame.NORMAL, "bye");
            }
            client = null;

            if (player != null) {
                player.stop();
            }

            if (!textQueue.isEmpty()) {
                textQueue.clear();
            }

            if (!allAudioData.isEmpty()) {
                allAudioData.clear();
            }

            pis.close();
            pos.close();
            executor.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
