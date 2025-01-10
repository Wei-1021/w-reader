package com.wei.wreader.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal工具类
 *
 * @author weizhanjie
 */
public class BigDecimalUtil {

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @return
     */
    public static BigDecimal divide(float dividend,
                                    float divisor) {
        return divide(new BigDecimal(String.valueOf(dividend)), new BigDecimal(String.valueOf(divisor)));
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @return
     */
    public static BigDecimal divide(double dividend,
                                    double divisor) {
        return divide(new BigDecimal(String.valueOf(dividend)), new BigDecimal(String.valueOf(divisor)));
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @return
     */
    public static BigDecimal divide(int dividend,
                                    int divisor) {
        return divide(new BigDecimal(String.valueOf(dividend)), new BigDecimal(String.valueOf(divisor)));
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @return
     */
    public static BigDecimal divide(String dividend,
                                    String divisor) {
        return divide(new BigDecimal(dividend), new BigDecimal(divisor));
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @return
     */
    public static BigDecimal divide(BigDecimal dividend,
                                    BigDecimal divisor) {
        return divide(dividend, divisor, 2);
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @param accuracy 精度，精确到小数点后几位
     * @return
     */
    public static BigDecimal divide(float dividend,
                                    float divisor,
                                    int accuracy) {
        if (new BigDecimal("0").equals(new BigDecimal(Float.toString(divisor)))) {
            return new BigDecimal("0");
        }
        return new BigDecimal(Float.toString(dividend))
                .divide(new BigDecimal(Float.toString(divisor)), accuracy, RoundingMode.HALF_UP);
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @param accuracy 精度，精确到小数点后几位
     * @return
     */
    public static BigDecimal divide(int dividend,
                                    int divisor,
                                    int accuracy) {
        return divide(dividend, divisor, accuracy, RoundingMode.HALF_UP);
    }
    /**
     * 除法 dividend/divisor--不 四舍五入
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @param accuracy 精度，精确到小数点后几位
     * @return
     */
    public static BigDecimal divide2(int dividend,
                                    int divisor,
                                    int accuracy) {
        return divide(dividend, divisor, accuracy,RoundingMode.DOWN);
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @param accuracy 精度，精确到小数点后几位
     * @return
     */
    public static BigDecimal divide(int dividend,
                                    int divisor,
                                    int accuracy,
                                    RoundingMode roundingMode) {
        if (new BigDecimal("0").equals(new BigDecimal(Integer.toString(divisor)))) {
            return new BigDecimal("0");
        }
        return new BigDecimal(Integer.toString(dividend))
                .divide(new BigDecimal(Integer.toString(divisor)), accuracy, roundingMode);
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @param accuracy 精度，精确到小数点后几位
     * @return
     */
    public static BigDecimal divide(String dividend,
                                    String divisor,
                                    int accuracy) {
        if (new BigDecimal("0").equals(new BigDecimal(divisor))) {
            return new BigDecimal("0");
        }
        return new BigDecimal(dividend).divide(new BigDecimal(divisor), accuracy, RoundingMode.HALF_UP);
    }

    /**
     * 除法 dividend/divisor
     *
     * @param dividend 被除数（分子）
     * @param divisor  除数（分母）
     * @param accuracy 精度，精确到小数点后几位
     * @return
     */
    public static BigDecimal divide(BigDecimal dividend,
                                    BigDecimal divisor,
                                    int accuracy) {
        if (new BigDecimal("0").equals(divisor)) {
            return new BigDecimal("0");
        }
        return dividend.divide(divisor, accuracy, RoundingMode.HALF_UP);
    }

    /**
     * 乘法
     *
     * @param multiplicand
     * @param multiplier
     * @return
     */
    public static BigDecimal multi(float multiplicand,
                                   float multiplier) {
        return new BigDecimal(String.valueOf(multiplicand)).multiply(new BigDecimal(String.valueOf(multiplier)));
    }
    /**
     * 乘法
     *
     * @param multiplicand
     * @param multiplier
     * @return
     */
    public static BigDecimal multi(double multiplicand,
                                   double multiplier) {
        return new BigDecimal(String.valueOf(multiplicand)).multiply(new BigDecimal(String.valueOf(multiplier)));
    }

    /**
     * 乘法
     *
     * @param multiplicand
     * @param multiplier
     * @return
     */
    public static BigDecimal multi(int multiplicand,
                                   int multiplier) {
        return new BigDecimal(String.valueOf(multiplicand)).multiply(new BigDecimal(String.valueOf(multiplier)));
    }

    /**
     * 乘法
     *
     * @param multiplicand
     * @param multiplier
     * @return
     */
    public static BigDecimal multi(String multiplicand,
                                   String multiplier) {
        return new BigDecimal(multiplicand).multiply(new BigDecimal(multiplier));
    }

    /**
     * 乘法
     *
     * @param multiplicand
     * @param multiplier
     * @return
     */
    public static BigDecimal multi(BigDecimal multiplicand,
                                   BigDecimal multiplier) {
        return multiplicand.multiply(multiplier);
    }
    /**
     * 去除小数点的0
     * @param num
     * @return
     */
    public static String getNum(String num){
    	     String result="";
    	     BigDecimal value = new BigDecimal(num);
    	     BigDecimal noZeros = value.stripTrailingZeros();
    	     result = noZeros.toPlainString();
     	     return result;
    }
   
    // ------------------- 加法 -----------------
    public static BigDecimal add(BigDecimal num1, BigDecimal num2) {
        return num1.add(num2);
    }

    public static BigDecimal add(String num1, String num2) {
        return new BigDecimal(num1).add(new BigDecimal(num2));
    }

    public static BigDecimal add(int num1, int num2) {
        return new BigDecimal(String.valueOf(num1)).add(new BigDecimal(String.valueOf(num2)));
    }

    public static BigDecimal add(float num1, float num2) {
        return new BigDecimal(String.valueOf(num1)).add(new BigDecimal(String.valueOf(num2)));
    }

    public static BigDecimal add(double num1, double num2) {
        return new BigDecimal(String.valueOf(num1)).add(new BigDecimal(String.valueOf(num2)));
    }

    // ----------------------- 减法 --------------------------
    public static BigDecimal subtract(BigDecimal num1, BigDecimal num2) {
        return num1.subtract(num2);
    }

    public static BigDecimal subtract(String num1, String num2) {
        return new BigDecimal(num1).subtract(new BigDecimal(num2));
    }

    public static BigDecimal subtract(int num1, int num2) {
        return new BigDecimal(String.valueOf(num1)).subtract(new BigDecimal(String.valueOf(num2)));
    }

    public static BigDecimal subtract(float num1, float num2) {
        return new BigDecimal(String.valueOf(num1)).subtract(new BigDecimal(String.valueOf(num2)));
    }

    public static BigDecimal subtract(double num1, double num2) {
        return new BigDecimal(String.valueOf(num1)).subtract(new BigDecimal(String.valueOf(num2)));
    }

}
