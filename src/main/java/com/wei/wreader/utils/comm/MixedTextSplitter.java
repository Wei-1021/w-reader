package com.wei.wreader.utils.comm;

import java.util.ArrayList;
import java.util.List;

public class MixedTextSplitter {

    public static List<String> splitText(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty() || maxLength <= 0) {
            return result;
        }

        // 将文本按空格分割为tokens（单词或连续非空格字符）
        String[] tokens = text.split("\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String token : tokens) {
            // 判断是否为英文单词（仅字母组成）
            boolean isEnglishWord = token.matches("^[a-zA-Z]+$");
            
            if (isEnglishWord) {
                // 处理英文单词：保持完整
                handleEnglishWord(token, currentChunk, result, maxLength);
            } else {
                // 处理非英文文本：逐字符分割
                handleNonEnglishText(token, currentChunk, result, maxLength);
            }
        }
        
        // 添加最后剩余的段落
        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString());
        }
        
        return result;
    }

    private static void handleEnglishWord(String word, 
                                          StringBuilder currentChunk, 
                                          List<String> result,
                                          int maxLength) {
        // 英文单词必须保持完整
        int newLength = currentChunk.length() + (currentChunk.length() > 0 ? 1 : 0) + word.length();
        if (newLength > maxLength) {
            // 当前段落无法容纳，先存入结果
            if (currentChunk.length() > 0) {
                result.add(currentChunk.toString());
                currentChunk.setLength(0);
            }
            // 检查单词是否超过最大长度
            if (word.length() > maxLength) {
                // 单词过长需要特殊处理（如截断或单独成段）
                // 这里选择单独成段，即使超过maxLength
                result.add(word);
            } else {
                currentChunk.append(word);
            }
        } else {
            if (currentChunk.length() > 0) {
                currentChunk.append(' ');
            }
            currentChunk.append(word);
        }
    }

    private static void handleNonEnglishText(String text, 
                                             StringBuilder currentChunk, 
                                             List<String> result,
                                             int maxLength) {
        // 非英文文本逐字符处理
        for (char c : text.toCharArray()) {
            if (currentChunk.length() >= maxLength) {
                // 当前段落已满，存入结果并重置
                result.add(currentChunk.toString());
                currentChunk.setLength(0);
            }
            currentChunk.append(c);
        }
    }

    public static void main(String[] args) {
        // 测试混合文本
        String mixedText = "This is a sample English sentence, 这是一个需要被分割成更小部分的样例中文句子,This is a sample English" +
                " sentence that we want to split into smaller chunks without breaking words.";

        List<String> result = splitText(mixedText, 30);
        
        System.out.println("Mixed Text Split:");
        result.forEach(s -> System.out.println("[" + s + "]"));
    }
}