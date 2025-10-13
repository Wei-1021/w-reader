package com.wei.wreader.utils.comm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>方法执行器</p>
 * <p>格式: {@code <java>完整类名.方法名(参数列表)</java>}</p>
 * <p>示例：{@code <java>com.example.MyClass.myMethod(123, abc)</java>}</p>
 * <p>方法的String型参数头尾两侧不能有引号，容易引起错误，如"abc" -> abc;</p>
 * <p>参数与参数之间用逗号+空格分割，防止与参数中内容里的逗号发生冲突</p>
 *
 * @author weizhanjie
 */
public class MethodExecutor {

    /**
     * 根据给定的配置字符串执行其中指定的Java方法
     *
     * @param configStr 包含方法调用配置的字符串，格式如 "<java>完整类名.方法名(参数列表)</java>"
     * @return 方法执行的结果，如果方法返回类型为void，则返回null
     * @throws Exception 如果在解析配置字符串、加载类、获取方法或执行方法过程中出现任何异常则抛出异常
     */
    public static Object executeMethod(String configStr) throws Exception {
        // 1. 提取标签内的方法调用部分
        String methodCallStr = extractMethodCall(configStr);
        // 2. 解析出类名、方法名和参数
        String className = parseClassName(methodCallStr);
        String methodName = parseMethodName(methodCallStr);
        List<String> paramStrs = parseParameters(methodCallStr);

        // 3. 使用反射加载类
        Class<?> clazz = Class.forName(className);

        // 4. 获取方法对象（尝试匹配参数类型获取对应的方法）
        Method method = getMethod(clazz, methodName, paramStrs);

        // 5. 准备实际参数（根据参数类型进行转换）
        Object[] actualParams = prepareParameters(paramStrs, method.getParameterTypes());

        // 6. 调用方法并返回结果
        return method.invoke(null, actualParams);
    }

    /**
     * 从给定的配置字符串中提取出 {@code <java>} 和 {@code </java>} 标签之间的实际方法调用内容
     *
     * @param configStr 包含方法调用配置的完整字符串
     * @return {@code <java>} 和 {@code </java>} 标签之间的方法调用字符串
     */
    private static String extractMethodCall(String configStr) {
        return configStr.substring(configStr.indexOf("<java>") + "<java>".length(), configStr.indexOf("</java>"));
    }

    /**
     * 从给定的配置字符串中提取出 {@code </java>} 标签之后的实际方法调用内容
     *
     * @param configStr 包含方法调用配置的完整字符串
     * @return {@code </java>} 标签之后的内容
     */
    private static String extractMethodCallAfter(String configStr) {
        return configStr.substring(configStr.indexOf("</java>") + "</java>".length());
    }

    /**
     * 通过查找方法调用字符串中最后一个. 的位置，提取出其前面的部分作为要加载的类名
     *
     * @param methodCallStr 包含类名、方法名及参数的方法调用字符串
     * @return 解析出的类名
     */
    private static String parseClassName(String methodCallStr) {
        int lastDotIndex = methodCallStr.lastIndexOf(".");
        return methodCallStr.substring(0, lastDotIndex);
    }

    /**
     * 从方法调用字符串中，在最后一个. 之后到 ( 之前的内容确定为方法名
     *
     * @param methodCallStr 包含类名、方法名及参数的方法调用字符串
     * @return 解析出的方法名
     */
    private static String parseMethodName(String methodCallStr) {
        int lastDotIndex = methodCallStr.lastIndexOf(".");
        return methodCallStr.substring(lastDotIndex + 1, methodCallStr.indexOf("("));
    }

    /**
     * 获取方法调用字符串中 ( 和 ) 之间的内容，按照逗号, 分割成字符串数组，再将每个元素去除空格后放入列表中，
     * 得到参数的字符串表示列表，用于后续解析参数类型和准备实际参数
     *
     * @param methodCallStr 包含类名、方法名及参数的方法调用字符串
     * @return 参数的字符串表示列表
     */
    private static List<String> parseParameters(String methodCallStr) {
        List<String> paramList = new ArrayList<>();
        String paramStr = methodCallStr.substring(methodCallStr.indexOf("(") + 1, methodCallStr.indexOf(")"));
        if (!paramStr.isEmpty()) {
            String[] paramArr = paramStr.split(", ");
            for (String s : paramArr) {
                paramList.add(s.trim());
            }
        }
        return paramList;
    }

    /**
     * 首先尝试通过 getMethod 直接获取指定名称且参数类型完全匹配的方法（根据解析得到的参数类型数组），
     * 如果获取失败（比如存在方法重载且参数类型不完全匹配但实际是兼容的情况，像可变参数方法等），
     * 则遍历类中所有的 public 方法，查找名称相同且参数类型兼容（通过 isCompatibleParamTypes 方法判断，
     * 即实际参数类型是否可以赋值给声明的参数类型）的方法并返回，如果最终都没找到合适的方法则抛出 NoSuchMethodException 异常
     *
     * @param clazz      要查找方法的类对象
     * @param methodName 要查找的方法名称
     * @param paramStrs  参数的字符串表示列表
     * @return 匹配的 Method 对象
     * @throws Exception 如果没有找到合适的方法则抛出异常
     */
    private static Method getMethod(Class<?> clazz, String methodName, List<String> paramStrs) throws Exception {
        Class<?>[] paramTypes = getParamTypes(paramStrs);
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            // 如果直接获取方法失败，尝试查找参数类型兼容的方法（比如存在可变参数情况等）
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) && isCompatibleParamTypes(m.getParameterTypes(), paramTypes)) {
                    return m;
                }
            }
            throw e;
        }
    }

    /**
     * 根据参数的字符串表示列表，逐个调用 getParamType 方法来确定每个参数对应的Java类型，
     * 然后构建并返回参数类型数组，用于在获取方法和准备参数时使用
     *
     * @param paramStrs 参数的字符串表示列表
     * @return 参数类型数组
     */
    private static Class<?>[] getParamTypes(List<String> paramStrs) {
        Class<?>[] paramTypes = new Class<?>[paramStrs.size()];
        for (int i = 0; i < paramStrs.size(); i++) {
            String paramStr = paramStrs.get(i);
            paramTypes[i] = getParamType(paramStr);
        }
        return paramTypes;
    }

    /**
     * 根据参数字符串的内容简单判断其可能对应的Java基本数据类型或 String 类型，例如全数字字符串判断为 Integer 类，
     * 包含小数点的数字字符串判断为 Double 类，"true" 或 "false" 判断为 Boolean 类，其他情况则认为是 String 类，
     * 用于确定参数在方法调用时的实际类型
     *
     * @param paramStr 参数的字符串表示
     * @return 参数对应的Java类型
     */
    private static Class<?> getParamType(String paramStr) {
        if (paramStr.matches("\\d+")) {
            return Integer.class;
        } else if (paramStr.matches("\\d+\\.\\d+")) {
            return Double.class;
        } else if ("true".equalsIgnoreCase(paramStr) || "false".equalsIgnoreCase(paramStr)) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    /**
     * 用于判断给定的声明的参数类型数组和实际解析得到的参数类型数组是否兼容，即实际参数类型是否可以赋值给声明的参数类型，
     * 通过遍历数组比较每个位置的类型是否满足 isAssignableFrom 关系来判断，如果数组长度不一致或者有类型不兼容的情况则返回 false，
     * 否则返回 true
     *
     * @param declaredParamTypes 方法声明的参数类型数组
     * @param actualParamTypes   实际解析得到的参数类型数组
     * @return 表示参数类型是否兼容的布尔值
     */
    private static boolean isCompatibleParamTypes(Class<?>[] declaredParamTypes, Class<?>[] actualParamTypes) {
        if (declaredParamTypes.length != actualParamTypes.length) {
            return false;
        }
        for (int i = 0; i < declaredParamTypes.length; i++) {
            if (!declaredParamTypes[i].isAssignableFrom(actualParamTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据参数的字符串表示列表和解析得到的参数类型数组，逐个调用 convertParam 方法将参数字符串转换为对应类型的实际参数对象，
     * 构建并返回参数对象数组，用于最终调用方法时传入实际参数
     *
     * @param paramStrs  参数的字符串表示列表
     * @param paramTypes 解析得到的参数类型数组
     * @return 转换后的实际参数对象数组
     * @throws Exception 如果参数转换过程中出现异常则抛出异常
     */
    private static Object[] prepareParameters(List<String> paramStrs, Class<?>[] paramTypes) throws Exception {
        Object[] params = new Object[paramStrs.size()];
        for (int i = 0; i < paramStrs.size(); i++) {
            String paramStr = paramStrs.get(i);
            params[i] = convertParam(paramStr, paramTypes[i]);
        }
        return params;
    }

    /**
     * 根据参数的目标类型，将参数字符串进行相应的转换操作，比如将字符串转换为 Integer、Double 或 Boolean 等类型的值（通过对应的 parseXxx 方法），
     * 如果是 String 类型则直接返回参数字符串本身，以此实现将参数字符串转换为方法调用所需的实际参数类型
     *
     * @param paramStr  参数的字符串表示
     * @param paramType 参数对应的目标类型
     * @return 转换后的参数对象
     * @throws Exception 如果参数转换过程中出现异常则抛出异常
     */
    private static Object convertParam(String paramStr, Class<?> paramType) throws Exception {
        if (paramType == Integer.class) {
            return Integer.parseInt(paramStr);
        } else if (paramType == Double.class) {
            return Double.parseDouble(paramStr);
        } else if (paramType == Boolean.class) {
            return Boolean.parseBoolean(paramStr);
        } else {
            return paramStr;
        }
    }

    public static void main(String[] args) {
        String encryptedBase64Str = "I1jW/HTOt9XwAU/TVdODqqJqAqGKTA76T+rH2/y9ux2TS6N25beL+/3ahC4mlxAnVAWQauO/Aq4CNTp" +
                "PhXmL+yEdD+JeZZrjDLnXDgMUNr56l1nTQuWTzQ5BqJphT7+KtaZ2zSulSQXzvzDoGUQSdOaroJH7jI2BZb/REA9Q+4xSVRBSQm" +
                "0Jut4kr7x+SDTpbMFdaDtQsZt09tIjfQaWhbvgTYVsZqcU2SI75h7iV83DWi1D2ByIOWVMjC91tO6Jj49Tdl1LTYKA9kSmAwOla" +
                "pqQftN7xLszRFxniIdvf/g08FHpvsF7BQYpKgY+T8vQirx+jU7K0vVKdFcGAqUfnDcW6DCl138EG9nbVRMdpBo+Q4ClqEfLupS6" +
                "aAsUaMK/JqcNA2/eq5VYSDDnAwHrqi8RHiWb7l5e0SN0ZOcu11EfQusQX4kUyjQANN5rNqLy6l9OEnyTwlpf24kEQA9hnLAsyd8" +
                "Hovf/YcMutkx3ceCe/jPXFCwq/C0MG1lG4DQYHeaUINjTRf7qzM05Ww0vqWLrPO1Uj301yjFD8ebReDdiJnkY51FGuNMlrbFDXj" +
                "FBgYLJTee/3YRqUeRyfIH66FM7yDjxjesGrdcMykyAc5zymMeBdkeZsErkekjFVkJPJtwBusPIQ4TNMo/N8vKsOVM7t++WsyqzR3" +
                "ZHDU7jb+rdSEym95aisCKPlQu6QwzgDWaNlUlP2stLlh1T0KDg+zc0nUerZ5n4AftKDv6latcRODvwkbS1LA54WPwHIMRqXYbuWK" +
                "uVsUWNbAnu+CZNmp9Ahe0h+4qZkp0wTVlBChusopJcyfpemBdLTWxzvUI6HEKibDuuO2E6ugH59XdWdn90OAhf0s7WLl/BrdVuZe" +
                "1taORWcLBZuVX/oJ9uFhc61qZKIiyIzpyUODkdG6uDjxTpxHFuLFeBs9VdwW+eLifR2V41523GXSp64TDnwZuQqPPKsNnqHhv+aK" +
                "xwnsW7dbarPZiVCbjvpzUzdc+BPJ4x+a6lIrK++2XhhneLCa96qLzalwEXOclGA+ZCQUSCuMk38sAJG8EeqXFOF6Wjk5QnPx4Ag8" +
                "BVOAdrY+y8okb5ztbc4TdKmVjzbMfSGEF6Df8o79agqZdfVM+wBK7z4WWBT3ERCDNCTuRbc0tCtoiCUabDUtF2Gufi47EI8+QV7+" +
                "iadPpRmgwndl1BVnYgl5oVm0a6DuLC7Rh7EsgLerSM8p6oZKxoNf7owzIYwNhPYXfZCYU4wKUOVrYe1571E3uocn9RRIy8K4WxBt" +
                "AeX3W4nBh3+RRaGkpMap0ny6ub7omr/M4BF0jkQp2EIPqk/b69EDUWDVl+ex/DqNRWLF1EvXemyRx+v2ob87zFnzdqJ2vOIJa/Yf" +
                "Z4wbaddcaleDgAZGsLc83g6VSWiu2Yls3APuKm6Ap8y/rOkr8Fh/xtUOoTm5th6OWDLy1fv+9HSjlPE0xo96MoBAYQZ1nRvZiE2zW" +
                "lf+c/XpHk45blCLBVEmqrqcNWs+SUCPCh/ynor97xRIbmPoIIARlrSJ6SPoiyGotFAmrRHsBkBF19wTO6i40FIyFxxCU8Owz94QPF" +
                "aSeyD9IxdDKWc8kppWtong/7fYY6VP3J5iUQTWginDLQpsj+qaVgNt9fjb3gCw6sqPLL/oHq2Qh3n9v5QyIHc7ro4lm/BlnADrtkaM" +
                "N0AmGUd2Hpi/FJ3t/rbRU7uGH8mtTZoPkJ+FkIAMuwM9/S6uN2IpNk/KxG90EhLztK4uVolzoy0Er/s/od3vpktbjfN6BkUdOqMpiT" +
                "CUbG3wybVswngcDysh9HUwC5ERhhJ/VfW7Rb50tuh8/k/xA+bch86xietLaogoxmyj+WAzS1ooFmd80S+Qr5GJ0qMrXw0szHxxhqEw" +
                "JwqQlEenUIwNU15B+Bw+SU5PGjaheerIoRtb/Y5xjTcNf1tOQxwfTPfdaW0DZFQ3UUm0x3d/7ZVubi/PMy7AqQOgkA817BA/VEMfqX" +
                "ewXuVvB+cebth4ysXeH0KqVRXioI8pKn6ao3w2AXSdygtOBciM3A3O5At18Bp1qoCCwet6OfW8VLGZ5l9ymGg5dVfXDZptkW2SeHg0" +
                "fsTa3+L24CmTXFJgWqyqOfhIK+IWcM9SL7LpDIfHAXhYJUDYK8Vz+uQtH9uGkzEb63ln2fAY1nd7rjcJQzF0/HgPnlgbIpYV3/USZs" +
                "pMUfRairRn+HrlqduZKB1hMaF6mVY1TdhdUzFP+82wHk2zfxUcM96itYzf9L1IQwsFITJFjdP6mBfzPB0DasXqdWNj01xMbD1+0h0w" +
                "e9TJHohoVKEXF50t568hsv5tHsU3vYZaucgdlPzksfO1XqKrPAxSl+3BfPdiREQlOXG3LZEtAy6+NkVYtGLdxEtvK2hsKBVQ0Ncwxg" +
                "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";

        String configStr = "<java>com.wei.wreader.utils.comm.AESDecryption.aesBase64DecodeToTransStr(" + encryptedBase64Str +
                ",6CE93717FBEA3E4F,AES/CBC/NoPadding,6CE93717FBEA3E4F,{\"###$$$\": \"<br>\"})</java>";

        String conf2 = "<java>String s1 = \"123456\";</java>";

        try {
            Object result = MethodExecutor.executeMethod(configStr);
            System.out.println("执行结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}