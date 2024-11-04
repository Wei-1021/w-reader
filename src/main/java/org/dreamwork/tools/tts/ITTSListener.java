package org.dreamwork.tools.tts;

import java.nio.file.Path;

public interface ITTSListener {
    /**
     * 当开始合成一段文本时触发
     * @param text 即将被合成的文本
     */
    default void started (String text) {}

    /**
     * 当一段文本合成成功后触发
     * @param text 被成功合成的文本
     */
    default void finished (String text) {}

    /**
     * 进入空闲模式时触发
     */
    default void idle () {}

    /**
     * 但一段合成的语音文件被存盘后触发
     * @param text 原始文本
     * @param path 合成后的语音文件。 mp3 格式
     */
    default void voiceSaved (String text, Path path) {}
}