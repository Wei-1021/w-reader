package com.wei.wreader.utils.comm;

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

    public static void main(String[] args) {
        try {
            String encryptedBase64Str = "I1jW/HTOt9XwAU/TVdODqqJqAqGKTA76T+rH2/y9ux2TS6N25beL+/3ahC4mlxAnVAWQauO/Aq4CNTpPhXmL+yEdD+JeZZrjDLnXDgMUNr56l1nTQuWTzQ5BqJphT7+KtaZ2zSulSQXzvzDoGUQSdOaroJH7jI2BZb/REA9Q+4xSVRBSQm0Jut4kr7x+SDTpbMFdaDtQsZt09tIjfQaWhbvgTYVsZqcU2SI75h7iV83DWi1D2ByIOWVMjC91tO6Jj49Tdl1LTYKA9kSmAwOlapqQftN7xLszRFxniIdvf/g08FHpvsF7BQYpKgY+T8vQirx+jU7K0vVKdFcGAqUfnDcW6DCl138EG9nbVRMdpBo+Q4ClqEfLupS6aAsUaMK/JqcNA2/eq5VYSDDnAwHrqi8RHiWb7l5e0SN0ZOcu11EfQusQX4kUyjQANN5rNqLy6l9OEnyTwlpf24kEQA9hnLAsyd8Hovf/YcMutkx3ceCe/jPXFCwq/C0MG1lG4DQYHeaUINjTRf7qzM05Ww0vqWLrPO1Uj301yjFD8ebReDdiJnkY51FGuNMlrbFDXjFBgYLJTee/3YRqUeRyfIH66FM7yDjxjesGrdcMykyAc5zymMeBdkeZsErkekjFVkJPJtwBusPIQ4TNMo/N8vKsOVM7t++WsyqzR3ZHDU7jb+rdSEym95aisCKPlQu6QwzgDWaNlUlP2stLlh1T0KDg+zc0nUerZ5n4AftKDv6latcRODvwkbS1LA54WPwHIMRqXYbuWKuVsUWNbAnu+CZNmp9Ahe0h+4qZkp0wTVlBChusopJcyfpemBdLTWxzvUI6HEKibDuuO2E6ugH59XdWdn90OAhf0s7WLl/BrdVuZe1taORWcLBZuVX/oJ9uFhc61qZKIiyIzpyUODkdG6uDjxTpxHFuLFeBs9VdwW+eLifR2V41523GXSp64TDnwZuQqPPKsNnqHhv+aKxwnsW7dbarPZiVCbjvpzUzdc+BPJ4x+a6lIrK++2XhhneLCa96qLzalwEXOclGA+ZCQUSCuMk38sAJG8EeqXFOF6Wjk5QnPx4Ag8BVOAdrY+y8okb5ztbc4TdKmVjzbMfSGEF6Df8o79agqZdfVM+wBK7z4WWBT3ERCDNCTuRbc0tCtoiCUabDUtF2Gufi47EI8+QV7+iadPpRmgwndl1BVnYgl5oVm0a6DuLC7Rh7EsgLerSM8p6oZKxoNf7owzIYwNhPYXfZCYU4wKUOVrYe1571E3uocn9RRIy8K4WxBtAeX3W4nBh3+RRaGkpMap0ny6ub7omr/M4BF0jkQp2EIPqk/b69EDUWDVl+ex/DqNRWLF1EvXemyRx+v2ob87zFnzdqJ2vOIJa/YfZ4wbaddcaleDgAZGsLc83g6VSWiu2Yls3APuKm6Ap8y/rOkr8Fh/xtUOoTm5th6OWDLy1fv+9HSjlPE0xo96MoBAYQZ1nRvZiE2zWlf+c/XpHk45blCLBVEmqrqcNWs+SUCPCh/ynor97xRIbmPoIIARlrSJ6SPoiyGotFAmrRHsBkBF19wTO6i40FIyFxxCU8Owz94QPFaSeyD9IxdDKWc8kppWtong/7fYY6VP3J5iUQTWginDLQpsj+qaVgNt9fjb3gCw6sqPLL/oHq2Qh3n9v5QyIHc7ro4lm/BlnADrtkaMN0AmGUd2Hpi/FJ3t/rbRU7uGH8mtTZoPkJ+FkIAMuwM9/S6uN2IpNk/KxG90EhLztK4uVolzoy0Er/s/od3vpktbjfN6BkUdOqMpiTCUbG3wybVswngcDysh9HUwC5ERhhJ/VfW7Rb50tuh8/k/xA+bch86xietLaogoxmyj+WAzS1ooFmd80S+Qr5GJ0qMrXw0szHxxhqEwJwqQlEenUIwNU15B+Bw+SU5PGjaheerIoRtb/Y5xjTcNf1tOQxwfTPfdaW0DZFQ3UUm0x3d/7ZVubi/PMy7AqQOgkA817BA/VEMfqXewXuVvB+cebth4ysXeH0KqVRXioI8pKn6ao3w2AXSdygtOBciM3A3O5At18Bp1qoCCwet6OfW8VLGZ5l9ymGg5dVfXDZptkW2SeHg0fsTa3+L24CmTXFJgWqyqOfhIK+IWcM9SL7LpDIfHAXhYJUDYK8Vz+uQtH9uGkzEb63ln2fAY1nd7rjcJQzF0/HgPnlgbIpYV3/USZs" +
                    "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" +
                    "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" +
                    "/VpPk4YQXPkFMEsT7ZUVREGHP9D10RbdXS/kQnzTORONBtuW/0QPf582PCaCnDr+3ZdmU4xpW1VfF7+T38gamcBVwPNCv0488lGTlFrPl7ji9eXV4cCa5M/UJTn7rdrJgCvJOAMRLLEaW4eTmrEckZNmAZsi0Yt4iFBtlLDa/Fw6vXu9B/6oHEE/N9Ty+ho4RLz/kAbMBUPeUpuPt2By24RuCR+oLpfe1EWT1jyN/QeHfwDcPvmz+qg8LdWoF7Hf6CZmUKwfHJMuLM75yM170wsqnSzevFd3IgGpdfl5NQiJMqd/dxKJTSiyk9sAKRFI47M8zP/C1A5qYlikR6tFUvulkXbnzFdRvTiRR+4ph+XXTMnuIGtj6oxoJYmlgPgJv7sd/CE2ZrMLXNiS61tHuO5am8uVbSmxSrBNyt1Ymn/cXQ7O3MCQgb7AY2OkafWmq1vJLQDmwBhYoWbLK3S36HVLcYXrCwikyaWNZqPLcBn4J87yjmuOj+LKxJvHhM/eS1qpg6QJJrT61w8mxdVk34wJpMZ2xDHKU0rSIaEFvdbJxNcbNX4yU0ygdnv9CI5xauajxbSXtK6xDH9zNuntFkJBROQrf6wLsRxAZn/QLlJ+TE5RPwArUxJkTWAr6XfvtU2NzuAhHE0jAQw4aB0zTcsWSRvlytff5Ihsg7nN2kZz+sk3pTdVueSZmKvHHlwMusihO3epTJRz0pxdSIECajtkvXLBtZIWix2J+/1WA7nxzeNDNXzl6417d1v5qG+mgJw00J7yw9gfqKalMcSgYhMQznsP3mqaCNxVH0m6ScMxJ4GeYYwbpEH5NSkzUEfglnsBArn9hB15h9ASQQKfEB6Gu3733f4J4aq3cXa4QwL1fpBPP72a3hi5CYB4d5ZN43lALmDVWiu7nV+pmYhpHJaFHfKgFrp3WEpZFhfhN7uo329Pl7mGeKt/4MTf7JFHPInoPVzx7mz25MbVV/YHnLW6gYjOszDI8AB0H63HK9Ra0JCfTKQ0RXCU533lGsro+JVzQ9U40qx3tnIUmY06QYcFObcpQJL+ryhCo3kLLLYe7flMVzeFP2sHdqv7vyiN4QxRU2CJCSkMEfnDTSyYZbkIb1uGV8n29eIqk7DcxX6yebZEDnYjKf3I3N6c9heXRLNXkY0QXMeWxaORubXNABssUTJauMZs86hRleNCbUnmbrjYLGI+sTaZnlvwQjY7eqgi6gQI+Ypg1WDXWh9iVo+Vm76ZjwZ7U6v9ZMI2lEl2dmo95Ks+L979m9ONuqfrULw8mfTtDjoQcZcVhcO12LNCWLR0DrvBHqQwt1f6rs2OpOeuxRY/mc2UiARUXOghme6N34x+/tlaCzKfuAN7dkJeAeXAI7/4f3QQht+Zf13Kd1UhiZAVaGdjyseIAQDXKUfV6tiwjXrMhrAuCDoQZhMO9Q8YkdepQ2TlhtOUPkOIo2ECd2xIEytGLb55kDiw6zfiiHQCry6MJQwdIZnPUccFiSPBahrXh40BKDWKBSKskd4VGb03RzQR/qxM0NDx+iXRejZ2AowNyeazkoQltXGaRPPA3LVqQ33WanUlpVF1T5m66ARzL3/DXZvDzaYGVbEnFfiFJAYHUq1Ob6nR5pL/9S3R15Ge6i/U9EdDEzRxaDp2l6EQ4Ht09dPfD7r1QwI699Yz1lVXzoGMD9kKx1X6QDY8kvkHpbNg8nDzOiO+vBHqt/+h35G5TW1v2lTzm7MMIMhcDprsy3fxQ65IFkXb6E6KDM21y1e28dICV1BhupEZT4BWJVMEobT6VRWg==";

            String key = "6CE93717FBEA3E4F";
            String transformation = "AES/CBC/NoPadding";
            String iv = "6CE93717FBEA3E4F";
            String decryptedText = aesBase64DecodeToString(encryptedBase64Str, key, transformation, iv);
            System.out.println("解密后的明文: " + decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}