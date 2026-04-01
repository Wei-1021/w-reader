package com.wei.wreader.utils.tts.mimo.v2;


import com.wei.wreader.utils.tts.mimo.v2.enums.VoiceStyle;
import com.wei.wreader.utils.tts.mimo.v2.player.StreamTTSPlayer;

import java.util.Scanner;

public class TTSExample {

    public static void main(String[] args) {
        MiMoTTSClient client = new MiMoTTSClient("your-api-key");

        try {
            // ========== 示例1：最简单的播放（非阻塞） ==========
            System.out.println("示例1：播放后继续执行其他逻辑");
            StreamTTSPlayer player1 = client.play("你好，我是MiMo，很高兴为你服务！");

            // 播放已在后台进行，主线程可以做其他事情
            System.out.println("播放已启动，主线程继续...");
            System.out.println("当前状态: " + player1.getState());

            // 模拟主线程做其他工作
            for (int i = 0; i < 5; i++) {
                Thread.sleep(500);
                System.out.println("主线程工作中... 状态: " + player1.getState()
                        + ", 已播放: " + player1.getTotalBytes() + " bytes");
            }

            // 等待播放完成
            player1.awaitCompletion();
            System.out.println("播放完成，总字节数: " + player1.getTotalBytes());

            // ========== 示例2：带监听器的播放 ==========
            System.out.println("\n示例2：带监听器");
            StreamTTSPlayer player2 = client.createStreamPlayer();
            player2.start("哎呀妈呀，这天儿也忒冷了吧！",
                    VoiceStyle.NORTHEAST_DIALECT.getValue(),
                    null,
                    new StreamTTSPlayer.PlayerListener() {
                    @Override
                    public void onStarted() {
                        System.out.println("播放开始");
                    }

                    @Override
                    public void onChunkPlayed(byte[] data, long totalBytes, long chunkCount) {
                        System.out.print(".");
                    }

                    @Override
                    public void onCompleted(long totalBytes, long durationMs) {
                        System.out.println("\n播放完成: " + totalBytes + " bytes");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.err.println("出错: " + e.getMessage());
                    }
                }
            );

            // 主线程不阻塞
            System.out.println("播放2已在后台进行...");
            player2.awaitCompletion();

            // ========== 示例3：播放并保存文件 ==========
            System.out.println("\n示例3：播放并保存");
            StreamTTSPlayer player3 = client.playAndSave(
                "明天就是周五了，真开心！",
                VoiceStyle.HAPPY,
                "output.pcm"
            );

            // 主线程可以做其他事
            System.out.println("播放3已在后台进行，同时保存到 output.pcm...");
            player3.awaitCompletion();
            System.out.println("保存完成");

            // ========== 示例4：手动控制停止 ==========
            System.out.println("\n示例4：手动停止");
            StreamTTSPlayer player4 = client.play(
                "这是一段很长的文本，会在播放过程中被手动停止..." +
                "春天来了，万物复苏，大地一片生机勃勃的景象。" +
                "小草从泥土里探出头来，花儿竞相开放，鸟儿在枝头歌唱。" +
                "春风拂面，温暖而舒适，让人心情愉悦。"
            );

            // 播放 2 秒后停止
            Thread.sleep(2000);
            System.out.println("手动停止播放...");
            player4.stop();
            System.out.println("已停止，状态: " + player4.getState());
            System.out.println("已播放: " + player4.getTotalBytes() + " bytes");

            // ========== 示例5：交互式控制 ==========
            System.out.println("\n示例5：交互式控制（输入 q 停止）");
            StreamTTSPlayer player5 = client.play(
                "这是一段可以随时停止的语音。" +
                "春天来了，万物复苏，大地一片生机勃勃的景象。" +
                "小草从泥土里探出头来，花儿竞相开放，鸟儿在枝头歌唱。" +
                "春风拂面，温暖而舒适，让人心情愉悦。" +
                "夏天到了，阳光明媚，知了在树上不停地叫着。" +
                "孩子们在河边嬉戏，笑声回荡在整个山谷。"
            );

            Scanner scanner = new Scanner(System.in);
            while (player5.isRunning()) {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    if ("q".equalsIgnoreCase(input.trim())) {
                        player5.stop();
                        break;
                    }
                }
                Thread.sleep(100);
            }
            System.out.println("已停止，播放了: " + player5.getChunkCount() + " 块");

            // ========== 示例6：多个播放器并发 ==========
            System.out.println("\n示例6：多个播放器（第二个等第一个完成后再播）");
            StreamTTSPlayer playerA = client.play("第一段语音开始播放。");
            System.out.println("播放器A已启动");

            playerA.awaitCompletion();
            System.out.println("播放器A完成，启动播放器B");

            StreamTTSPlayer playerB = client.play("第二段语音现在播放。");
            playerB.awaitCompletion();
            System.out.println("播放器B完成");

            System.out.println("\n全部示例执行完成！");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
