package com.wei.wreader.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTemplateEngine {

    /**
     * 定义正则表达式模式，用于匹配${表达式}格式的内容
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 渲染模板字符串，将模板中的 ${表达式} 占位符替换为实际值或计算结果
     *
     * @param template  模板字符串，包含 ${表达式} 形式的占位符
     * @param variables 变量映射表，键为变量名，值为对应变量的值，用于替换占位符中的变量或参与计算表达式求值
     * @return 渲染后的字符串，即把占位符都替换为相应值后的字符串
     */
    public static String render(String template, Map<String, Object> variables) {
        // 创建 Matcher 对象，用于在给定的模板字符串中查找与正则表达式匹配的部分
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        // 使用 StringBuffer 来构建最终渲染后的字符串结果，因为 String 是不可变的，频繁修改效率低，而 StringBuffer 可变便于拼接
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            // 获取匹配到的 ${表达式} 中的表达式部分，并去除两端的空白字符
            String expression = matcher.group(1).trim();
            Object value;
            // 判断表达式是否为计算表达式（包含四则运算符号或括号）
            if (isCalculationExpression(expression)) {
                // 如果是计算表达式，则调用 evaluateExpression 方法进行计算求值，传入表达式和变量映射表
                value = evaluateExpression(expression, variables);
            } else {
                // 如果不是计算表达式，尝试从变量映射表中获取对应变量的值
                value = variables.get(expression);
            }
            if (value!= null) {
                // 如果获取到的值不为空，将匹配到的占位符替换为该值的字符串表示形式
                matcher.appendReplacement(result, value.toString());
            } else {
                // 如果获取到的值为空，将匹配到的占位符替换为空字符串
                matcher.appendReplacement(result, "");
            }
        }
        // 将剩余未匹配的模板字符串部分添加到结果中，确保整个模板字符串内容完整
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 判断给定的字符串是否为计算表达式，即是否包含四则运算符号（+、-、*、/）或者括号（(、)）
     *
     * @param expression 要判断的字符串
     * @return 如果包含四则运算符号或括号则返回 true，表示是计算表达式；否则返回 false
     */
    private static boolean isCalculationExpression(String expression) {
        return expression.matches(".*[+\\-*/()].*");
    }

    /**
     * 对给定的包含变量的计算表达式进行求值计算，确保同级运算符按照从左到右的顺序进行运算
     *
     * @param expression 包含变量的计算表达式字符串，例如 "(page - 1) * 10"
     * @param variables  变量映射表，用于获取表达式中变量对应的实际值
     * @return 表达式计算后的结果对象，可以是数值类型（如 Integer、Double 等）
     */
    private static Object evaluateExpression(String expression, Map<String, Object> variables) {
        // 创建操作数栈，用于存放表达式中的操作数（数字或变量对应的值），操作数在计算过程中按照运算规则进出栈
        Stack<Object> operands = new Stack<>();
        // 创建运算符栈，用于存放表达式中的运算符（如 +、-、*、/ 等），运算符根据优先级等规则进出栈参与运算
        Stack<Character> operators = new Stack<>();

        int index = 0;
        while (index < expression.length()) {
            char ch = expression.charAt(index);
            // 如果字符是数字或者小数点，说明可能是数字的一部分，开始提取完整的数字
            if (Character.isDigit(ch) || ch == '.') {
                StringBuilder numBuilder = new StringBuilder();
                while (index < expression.length() && (Character.isDigit(expression.charAt(index)) || expression.charAt(index) == '.')) {
                    numBuilder.append(expression.charAt(index));
                    index++;
                }
                try {
                    // 先尝试将提取出来的数字字符串解析为 Double 类型，并压入操作数栈
                    operands.push(Double.parseDouble(numBuilder.toString()));
                } catch (NumberFormatException e) {
                    // 如果解析 Double 类型失败（可能是整数格式的字符串），则尝试解析为 Integer 类型并压入操作数栈
                    operands.push(Integer.parseInt(numBuilder.toString()));
                }
                index--;
            }
            // 如果字符是字母，认为可能是变量名的开始，提取完整的变量名
            else if (Character.isLetter(ch)) {
                StringBuilder varBuilder = new StringBuilder();
                while (index < expression.length() && Character.isLetter(expression.charAt(index))) {
                    varBuilder.append(expression.charAt(index));
                    index++;
                }
                String variableName = varBuilder.toString();
                Object variableValue = variables.get(variableName);
                if (variableValue == null) {
                    // 如果变量在变量映射表中不存在，抛出异常提示变量未定义
                    throw new IllegalArgumentException("变量 " + variableName + " 未在变量映射中定义");
                }
                // 将变量对应的值压入操作数栈
                operands.push(variableValue);
                index--;
            }
            // 如果字符是左括号 (，将其压入运算符栈
            else if (ch == '(') {
                operators.push(ch);
            }
            // 如果字符是右括号 )，按照运算规则，从运算符栈和操作数栈中弹出相应的运算符和操作数进行计算，直到遇到左括号
            else if (ch == ')') {
                while (operators.peek()!= '(') {
                    operands.push(applyOperator(operators.pop(), operands.pop(), operands.pop()));
                }
                operators.pop();
            }
            // 如果字符是四则运算符号（+、-、*、/），根据运算符优先级规则处理
            else if (isOperator(ch)) {
                // 处理同级运算符从左到右的顺序，根据运算符优先级分组判断
                while (!operators.isEmpty() && (
                        (isAddSubOperator(operators.peek()) && isAddSubOperator(ch)) ||
                                (isMulDivOperator(operators.peek()) && isMulDivOperator(ch))
                )) {
                    operands.push(applyOperator(operators.pop(), operands.pop(), operands.pop()));
                }
                operators.push(ch);
            }
            index++;
        }

        // 当表达式扫描完后，如果运算符栈中还有剩余运算符，继续按照顺序弹出运算符和操作数进行计算
        while (!operators.isEmpty()) {
            operands.push(applyOperator(operators.pop(), operands.pop(), operands.pop()));
        }

        // 最后操作数栈中只剩下一个元素，就是表达式的最终计算结果，将其弹出并返回
        return operands.pop();
    }

    /**
     * 判断给定的字符是否为四则运算符号（+、-、*、/）
     *
     * @param ch 要判断的字符
     * @return 如果是四则运算符号则返回 true，否则返回 false
     */
    private static boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/';
    }

    /**
     * 判断运算符是否为加法或减法运算符（+、-）
     *
     * @param ch 要判断的字符
     * @return 如果是加法或减法运算符则返回 true，否则返回 false
     */
    private static boolean isAddSubOperator(char ch) {
        return ch == '+' || ch == '-';
    }

    /**
     * 判断运算符是否为乘法或除法运算符（*、/）
     *
     * @param ch 要判断的字符
     * @return 如果是乘法或除法运算符则返回 true，否则返回 false
     */
    private static boolean isMulDivOperator(char ch) {
        return ch == '*' || ch == '/';
    }

    /**
     * 判断运算符 op1 的优先级是否高于运算符 op2，按照数学运算规则，* 和 / 的优先级高于 + 和 -
     *
     * @param op1 第一个运算符
     * @param op2 第二个运算符
     * @return 如果 op1 的优先级高于 op2，则返回 true；否则返回 false
     */
    private static boolean hasPrecedence(char op1, char op2) {
        return (op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-');
    }

    /**
     * 根据给定的运算符对两个操作数进行相应的四则运算
     *
     * @param operator 四则运算符号（+、-、*、/）
     * @param b        第二个操作数
     * @param a        第一个操作数
     * @return 运算后的结果，类型为 double，会根据具体运算进行相应的计算返回
     */
    @SuppressWarnings("unchecked")
    private static Object applyOperator(char operator, Object b, Object a) {
        // 首先判断操作数是否都为 Number 类型（Number 是 Integer、Double 等数值类型的父类），只有是数值类型才能进行运算
        if (a instanceof Number && b instanceof Number) {
            // 将操作数 a 转换为 double 类型的值，确保操作数顺序正确，先出栈的作为第一个操作数
            double numA = ((Number) a).doubleValue();
            // 将操作数 b 转换为 double 类型的值，后出栈的作为第二个操作数
            double numB = ((Number) b).doubleValue();
            return switch (operator) {
                case '+' -> computeIntegerOrDouble(BigDecimalUtil.add(numA, numB).doubleValue());
                case '-' -> computeIntegerOrDouble(BigDecimalUtil.subtract(numA, numB).doubleValue());
                case '*' -> computeIntegerOrDouble(BigDecimalUtil.multi(numA, numB).doubleValue());
                case '/' -> computeIntegerOrDouble(BigDecimalUtil.divide(numA, numB).doubleValue());
                default -> 0;
            };
        }
        // 如果操作数类型不满足要求，抛出异常提示操作数类型不匹配，无法进行运算
        throw new IllegalArgumentException("操作数类型不匹配，无法进行运算");
    }

    /**
     * 计算给定的值为整数或浮点数
     */
    private static Object computeIntegerOrDouble(Object value) {
        if (!(value instanceof Number numberResult)) {
            return value;
        }

        if (numberResult.doubleValue() % 1 == 0) {
            // 如果结果是整数，返回整数形式
            return numberResult.longValue();
        } else {
            // 否则，返回浮点数形式
            return numberResult.doubleValue();
        }
    }

    public static void main(String[] args) {
        // 定义一个包含计算表达式占位符的模板字符串，包含同级运算符连续出现的情况
        String template = "示例：${(page - 1) * 10}，变量替换示例：${name}，普通计算替换示例：${ 5 - 2 + 2.3}, ${ 5 / 2.5}, " +
                "${5 - 2 + 2.3 * 4 / 2}, ${5 - 2 + 2.3 - 1}";

        // 创建变量映射表，用于存放变量名和对应的值
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");
        variables.put("nam2e", "John");
        variables.put("page", 5);

        // 调用 render 方法进行模板渲染，将渲染后的结果存储在 rendered 变量中
        String rendered = render(template, variables);
        // 打印输出渲染后的字符串，展示模板引擎的功能
        System.out.println(rendered);
    }
}