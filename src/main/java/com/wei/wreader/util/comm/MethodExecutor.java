package com.wei.wreader.util.comm;

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

}