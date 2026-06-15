package com.wei.wreader.util.comm;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES加密解密
 *
 * @author weizhanjie
 */
public class AESDecryption {

    public static final String START_TRANS_STR = "st###";
    public static final String END_TRANS_STR = "###ed";

    /**
     * 使用AES加密算法（CBC模式、无填充）对明文字符串进行加密，并将加密结果转换为Base64编码的字符串
     *
     * @param plainText          要加密的明文字符串
     * @param key                加密密钥，长度需符合AES要求（16、24或32字节，这里示例为16字节）
     * @param transformation     加密算法转换字符串，格式如 "AES/CBC/NoPadding"
     * @param iv                 初始向量，长度需符合要求（和密钥长度相同，这里示例为16字节）
     * @return Base64编码的加密字符串
     * @throws Exception 如果加密过程中出现任何异常（如密钥长度错误、加密失败等）则抛出异常
     */
    public static String aesBase64Encode(String plainText, String key, String transformation, String iv) throws Exception {
        // 1. 创建AES密钥对象
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");

        // 2. 创建初始向量对象
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());

        // 3. 创建Cipher实例，用于加密
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        // 4. 执行加密操作，得到加密后的字节数组
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

        // 5. 将加密后的字节数组进行Base64编码，转换为字符串并返回
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 将Base64编码的AES加密（CBC模式、无填充）字符串解密为明文
     *
     * @param encryptedBase64Str 经过Base64编码的加密字符串
     * @param key                加密密钥，长度需符合AES要求（16、24或32字节，这里示例为16字节）
     * @param transformation     加密算法转换字符串，格式如 "AES/CBC/NoPadding"
     * @param iv                 初始向量，长度需符合要求（和密钥长度相同，这里示例为16字节）
     * @param transStr           转换规则: json字符串，key代表被替换的内容，value代表目标结果；<br>
     *                           当Key="st###"时，代表要在开头插入内容；当Key="###ed"时，代表要在结尾插入内容
     * @return 解密后的明文字符串
     * @throws Exception 如果加密过程中出现任何异常（如密钥长度错误、解密失败等）则抛出异常
     */
    public static String aesBase64DecodeToTransStr(String encryptedBase64Str,
                                                   String key,
                                                   String transformation,
                                                   String iv,
                                                   String transStr) throws Exception {
        StringBuilder str = new StringBuilder(aesBase64DecodeToString(encryptedBase64Str, key, transformation, iv));
        // 提取{和}之间的内容
        String transStrContent = transStr.substring(transStr.indexOf("{") + 1, transStr.indexOf("}"));
        String[] transStrItems = transStrContent.split(",");
        for (String transStrItem : transStrItems) {
            String[] transStrItemObjs = transStrItem.split(":");
            String transStrKey = transStrItemObjs[0].trim();
            String transStrValue = transStrItemObjs[1].trim();

            // 去除开头和结尾的双引号
            int ketLen = transStrKey.length();
            if (transStrKey.startsWith("\"")) {
                transStrKey = transStrKey.substring(1, ketLen - 1);
            }
            if (transStrKey.endsWith("\"")) {
                transStrKey = transStrKey.substring(0, ketLen - 1);
            }

            int valLen = transStrValue.length();
            if (transStrValue.startsWith("\"")) {
                transStrValue = transStrValue.substring(1, valLen - 1);
            }
            if (transStrValue.endsWith("\"")) {
                transStrValue = transStrValue.substring(0, valLen - 1);
            }


            if (transStrKey.equals(START_TRANS_STR)) {
                str.insert(0, transStrKey);
            }

            if (transStrKey.equals(END_TRANS_STR)) {
                str.append(transStrKey);
            }

            // 替换
            str = new StringBuilder(str.toString().replace(transStrKey, transStrValue));
        }
        return str.toString();
    }

    /**
     * 将Base64编码的AES加密（CBC模式、无填充）字符串解密为明文
     *
     * @param encryptedBase64Str 经过Base64编码的加密字符串
     * @param key                加密密钥，长度需符合AES要求（16、24或32字节，这里示例为16字节）
     * @param transformation     加密算法转换字符串，格式如 "AES/CBC/NoPadding"
     * @param iv                 初始向量，长度需符合要求（和密钥长度相同，这里示例为16字节）
     * @return 解密后的明文字符串
     * @throws Exception 如果加密过程中出现任何异常（如密钥长度错误、解密失败等）则抛出异常
     */
    public static String aesBase64DecodeToString(String encryptedBase64Str, String key, String transformation, String iv) throws Exception {
        // 1. 对Base64编码的加密字符串进行解码，得到字节数组
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64Str);

        // 2. 创建AES密钥对象
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");

        // 3. 创建初始向量对象
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());

        // 4. 创建Cipher实例，用于解密
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        // 5. 执行解密操作，得到解密后的字节数组
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // 6. 将解密后的字节数组转换为字符串并返回
        return new String(decryptedBytes);
    }
}