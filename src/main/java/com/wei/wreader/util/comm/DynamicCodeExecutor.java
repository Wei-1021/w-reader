package com.wei.wreader.util.comm;

import org.codehaus.janino.SimpleCompiler;

import java.io.StringReader;
import java.lang.reflect.Method;

/**
 * <strong>动态代码执行器</strong>
 *
 * <pre>
 * 格式:
 * {@code
 *     <java>
 *         <package_import>导入的包</package_import>
 *         <code>要执行的代码</code>
 *     </java>
 * }
 * </pre>
 *
 * @author weizhanjie
 */
public class DynamicCodeExecutor {

    /**
     * 执行动态代码
     *
     * @param code           动态代码
     * @param methodName     要执行的方法名
     * @param parameterTypes 方法参数类型数组
     * @param parameters     方法参数数组
     * @return 方法执行结果
     * @throws Exception 执行异常
     */
    public static Object executeMethod(String code, String methodName, Class<?>[] parameterTypes, Object[] parameters) throws Exception {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be blank.");
        }

        if (!code.contains("<java>") && !code.contains("</java>")) {
            throw new IllegalArgumentException("Code must contain <java> and </java>.");
        }

        // 从给定的配置字符串中提取出包导入语句
        String packageImport = extractPackageImport(code);
        // 从给定的配置字符串中提取出实际代码内容
        String codeCall = extractCodeCall(code);

        String classTemplate = packageImport +
                "public class DynamicCode {\n" +
                "    public DynamicCode() {}\n" +
                "    " + codeCall + "\n" +
                "}";

        // 加载指定的类加载器
        ClassLoader classLoader = DynamicCodeExecutor.class.getClassLoader();
        // 创建一个SimpleCompiler实例，并设置类加载器
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.setParentClassLoader(classLoader);
        compiler.cook(new StringReader(classTemplate));

        // 加载并运行生成的类
        Class<?> clazz = compiler.getClassLoader().loadClass("DynamicCode");
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // 调用指定的方法
        Method method = clazz.getMethod(methodName, parameterTypes);
        return method.invoke(instance, parameters);
    }

    /**
     * 从给定的配置字符串中提取出 {@code <code>} 和 {@code </code>} 标签之间的实际内容
     *
     * @param configStr 包含代码配置的完整字符串
     * @return {@code <code>} 和 {@code </code>} 标签之间的字符串
     */
    private static String extractCodeCall(String configStr) {
        return configStr.substring(configStr.indexOf("<code>") + "<code>".length(), configStr.indexOf("</code>"));
    }

    /**
     * 从给定的配置字符串中提取出 {@code <package_import>} 和 {@code </package_import>} 标签的内容
     *
     * @param configStr
     */
    private static String extractPackageImport(String configStr) {
        if (!configStr.contains("<package_import>") || !configStr.contains("</package_import>")) {
            return "";
        }
        return configStr.substring(configStr.indexOf("<package_import>") + "<package_import>".length(), configStr.indexOf("</package_import>"));
    }
}




