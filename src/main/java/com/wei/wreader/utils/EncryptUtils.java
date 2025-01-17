package com.wei.wreader.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * <strong>加密工具对象</strong>
 *
 * @author weizhanjie
 */
public class EncryptUtils {

    /**
     * MD2加密
     *
     * @param data 待加密字符串
     * @return 加密后字符串
     * @throws Exception 异常
     */
    public static String md2(String data)  {
        try {
            MessageDigest encrypt = MessageDigest.getInstance("MD2");
            encrypt.reset();
            encrypt.update(data.getBytes("UTF-8"));

            return byteToHex(encrypt.digest());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * MD5加密
     *
     * @param data 待加密字符串
     * @return 加密后字符串
     * @throws Exception 异常
     */
    public static String md5(String data)  {
        try {
            MessageDigest encrypt = MessageDigest.getInstance("MD5");
            encrypt.reset();
            encrypt.update(data.getBytes("UTF-8"));

            return byteToHex(encrypt.digest());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA-1加密
     *
     * @param data 待加密字符串
     * @return 加密后字符串
     * @throws Exception 异常
     */
    public static String sha1(String data) throws Exception {
        try {
            MessageDigest encrypt = MessageDigest.getInstance("SHA-1");
            encrypt.reset();
            encrypt.update(data.getBytes("UTF-8"));
            return byteToHex(encrypt.digest());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA-256加密
     *
     * @param data 待加密字符串
     * @return 加密后字符串
     * @throws Exception 异常
     */
    public static String sha256(String data) throws Exception {
        try {
            MessageDigest encrypt = MessageDigest.getInstance("SHA-256");
            encrypt.reset();
            encrypt.update(data.getBytes("UTF-8"));
            return byteToHex(encrypt.digest());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA-384加密
     *
     * @param data 待加密字符串
     * @return 加密后字符串
     * @throws Exception 异常
     */
    public static String sha384(String data) throws Exception {
        try {
            MessageDigest encrypt = MessageDigest.getInstance("SHA-384");
            encrypt.reset();
            encrypt.update(data.getBytes("UTF-8"));
            return byteToHex(encrypt.digest());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA-512加密
     *
     * @param data 待加密字符串
     * @return 加密后字符串
     * @throws Exception 异常
     */
    public static String sha512(String data) throws Exception {
        try {
            MessageDigest encrypt = MessageDigest.getInstance("SHA-512");
            encrypt.reset();
            encrypt.update(data.getBytes("UTF-8"));
            return byteToHex(encrypt.digest());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 二进制转字符串
     *
     * @param hash 二进制
     * @return 字符串
     */
    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}