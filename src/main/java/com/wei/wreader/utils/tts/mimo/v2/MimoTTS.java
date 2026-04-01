package com.wei.wreader.utils.tts.mimo.v2;

import com.wei.wreader.utils.tts.edge.SSMLPayload;
import com.wei.wreader.utils.tts.edge.VoiceFormat;
import com.wei.wreader.utils.tts.edge.VoiceRole;
import com.wei.wreader.utils.tts.edge.VoiceStyle;
import com.wei.wreader.utils.tts.edge.listener.ITTSListener;
import com.wei.wreader.utils.tts.mimo.v2.enums.AudioFormat;
import com.wei.wreader.utils.tts.mimo.v2.player.StreamTTSPlayer;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MimoTTS {

    private final ExecutorService executor;

    /**
     * 风格
     */
    private String style;
    private final StreamTTSPlayer ttsPlayer;
    private final TTSConfig config;
    private static AdvancedPlayer player;
    private final PipedInputStream pis;
    private final PipedOutputStream pos;
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

    private static MimoTTS instance;

    public MimoTTS(TTSConfig config) throws IOException {
        this.pos = new PipedOutputStream();
        this.pis = new PipedInputStream(pos);
        this.executor = Executors.newFixedThreadPool(3);
        this.config = config;
        this.ttsPlayer = new StreamTTSPlayer(config);
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
    public MimoTTS setStyleName(String name) {
        this.style = VoiceStyle.getByName(name).value;
        return this;
    }

    /**
     * 合成给定的文本并返回音频数据
     *
     * @param text 要合成的文本
     * @return 包含合成音频数据的字节数组列表的 CompletableFuture
     */
    public void synthesize(String text) {
        this.currentContent = text;
        // 发送SSML消息
        synthesising = true;
        isTempLastMsg = true;
    }

    /**
     * 开始合成
     */
    public void start() {
        executor.submit(() -> processQueue(currentContent));
        executor.submit(this::copyByteToOut);
        executor.submit(this::play);
        executor.shutdown();
    }

    /**
     * 处理文本队列，通过 WebSocket 连接发送文本到 Microsoft Edge 的 TTS 服务进行语音合成，并将合成的音频数据以字节数组的形式返回
     */
    private void processQueue(String text) {
        if (isDispose) {
            return;
        }


        synthesising = true;

        // 构建请求
        TTSRequest.Builder builder = TTSRequest.of(text)
                .voice(config.getDefaultVoice())
                .format(AudioFormat.PCM16);
        if (style != null && !style.isEmpty()) {
            builder.customStyle(style);
        }
        TTSRequest request = builder.build();

        ttsPlayer.running();
        ttsPlayer.startNetworkThread(request, new StreamCallback() {
            @Override
            public void onAudioChunk(byte[] audioChunk, int chunkIndex) {
                if (audioChunk == null || audioChunk.length == 0) {
                    return;
                }

                allAudioData.offer(audioChunk);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                synthesising = false;
            }

            @Override
            public void onError(Exception e) {
                super.onError(e);
                synthesising = false;
            }
        });

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
            while (!allAudioData.isEmpty() || synthesising) {
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

            if (player != null) {
                player.stop();
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
