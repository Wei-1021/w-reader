package com.wei.wreader.utils.tts.mimo.v2;

/**
 * 流式调用回调接口
 */
public abstract class  StreamCallback {

    /**
     * 接收到音频数据块时调用
     *
     * @param audioChunk 音频数据块（Base64 解码后的字节数组）
     * @param chunkIndex 当前块的索引（从 0 开始）
     */
    public void onAudioChunk(byte[] audioChunk, int chunkIndex) {};

    /**
     * 流式调用完成时调用
     */
    public void onComplete() {};

    /**
     * 发生错误时调用
     *
     * @param e 异常信息
     */
    public void onError(Exception e) {};
}
