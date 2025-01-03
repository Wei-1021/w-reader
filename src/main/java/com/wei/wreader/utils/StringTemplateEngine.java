package com.wei.wreader.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板引擎，用于渲染模板字符串，将大括号中的占位符替换为实际值或计算结果。
 *
 * @author weizhanjie
 * @since 2025/01/03
 */
public class StringTemplateEngine {

    // 定义正则表达式模式，用于匹配大括号中的内容
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /**
     * 渲染模板字符串，将其中的大括号占位符替换为实际值或计算结果。
     *
     * @param template 模板字符串，包含大括号占位符
     * @param params   包含参数名和对应值的映射表
     * @return 替换后的字符串
     */
    public static String render(String template, Map<String, Object> params) {
        // 创建一个Matcher对象来查找所有匹配项
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        // 遍历所有匹配项
        while (matcher.find()) {
            // 获取大括号中的表达式
            String expression = matcher.group(1).trim();
            try {
                // 评估表达式并获取结果
                String value = evaluateExpression(expression, params);
                // 将匹配项替换为评估结果
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid expression: " + expression, e);
            }
        }

        // 添加剩余部分到结果中
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 评估表达式，将占位符替换为实际值，并计算表达式的值。
     *
     * @param expression 表达式字符串
     * @param params     包含参数名和对应值的映射表
     * @return 表达式的计算结果或替换后的字符串
     * @throws Exception 如果表达式无效抛出异常
     */
    private static String evaluateExpression(String expression, Map<String, Object> params) throws Exception {
        // 创建一个Rhino上下文
        Context rhino = Context.enter();
        try {
            // 初始化标准对象作用域
            Scriptable scope = rhino.initStandardObjects();

            // 将参数定义在作用域中
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Number) {
                    // 如果是数字类型，将其作为double存储在作用域中
                    scope.put(key, scope, ((Number) value).doubleValue());
                } else {
                    // 否则，将其作为字符串存储在作用域中
                    scope.put(key, scope, value.toString());
                }
            }

            // 使用Rhino引擎评估表达式
            Object result = rhino.evaluateString(scope, expression, "<cmd>", 1, null);

            // 根据结果类型返回相应的字符串表示
            if (result instanceof Number numberResult) {
                if (numberResult.doubleValue() % 1 == 0) {
                    // 如果结果是整数，返回整数形式
                    return Long.toString(numberResult.longValue());
                } else {
                    // 否则，返回浮点数形式
                    return Double.toString(numberResult.doubleValue());
                }
            } else {
                return Context.toString(result);
            }
        } finally {
            // 退出Rhino上下文
            Context.exit();
        }
    }

    public static void main(String[] args) {
        // 示例模板字符串，包含数值和字符串类型的占位符
        String template = "Page number: {page}, Previous page offset: {(page-1)*10}, Greeting: {greeting}, Integer division: {5/2}";
        // 参数映射表
        Map<String, Object> params = new HashMap<>();
        params.put("key", 42); // 示例参数
        params.put("page", 5);
        params.put("greeting", "Hello, World!");

        // 渲染模板字符串
        String renderedString = render(template, params);
        // 输出结果
        System.out.println(renderedString);
    }
}



