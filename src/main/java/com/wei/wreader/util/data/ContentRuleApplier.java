package com.wei.wreader.util.data;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本内容规则解析与执行工具类
 * <p>
 * 规则格式：s/正则表达式/替换内容/标志
 * 多条规则用 ||| 分隔
 * <p>
 * 标志说明：
 * g - 全局替换（替换所有匹配项）
 * i - 忽略大小写
 */
public class ContentRuleApplier {

    /**
     * 对文本应用内容替换规则
     *
     * @param text      原始文本
     * @param rulesStr  规则字符串（多条用 ||| 分隔）
     * @return 替换后的文本
     */
    public static String applyRules(String text, String rulesStr) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(rulesStr)) {
            return text;
        }

        List<Rule> rules = parseRules(rulesStr);
        if (rules.isEmpty()) {
            return text;
        }

        String result = text;
        for (Rule rule : rules) {
            result = applySingleRule(result, rule);
        }
        return result;
    }

    /**
     * 对文本逐行应用内容替换规则（行以 &lt;br&gt; 分隔）
     *
     * @param text      原始文本（行以 &lt;br&gt; 分隔）
     * @param rulesStr  规则字符串（多条用 ||| 分隔）
     * @return 替换后的文本
     */
    public static String applyRulesPerLine(String text, String rulesStr) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(rulesStr)) {
            return text;
        }

        List<Rule> rules = parseRules(rulesStr);
        if (rules.isEmpty()) {
            return text;
        }

        String[] lines = text.split("<br>");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (Rule rule : rules) {
                line = applySingleRule(line, rule);
            }
            result.append(line);
            if (i < lines.length - 1) {
                result.append("<br>");
            }
        }
        return result.toString();
    }

    /**
     * 解析规则字符串为规则列表
     *
     * @param rulesStr 规则字符串（多条用换行或 ||| 分隔）
     * @return 规则列表
     */
    public static List<Rule> parseRules(String rulesStr) {
        List<Rule> rules = new ArrayList<>();
        if (StringUtils.isBlank(rulesStr)) {
            return rules;
        }

        // 先按换行分割，再按 ||| 分割
        String[] parts = rulesStr.split("\\r?\\n|\\|\\|\\|");
        for (String part : parts) {
            String trimmed = part.trim();
            if (StringUtils.isBlank(trimmed)) {
                continue;
            }
            Rule rule = parseSingleRule(trimmed);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    /**
     * 解析单条规则
     * 格式：s/regex/replacement/flags 或 s|regex|replacement|flags
     *
     * @param ruleStr 规则字符串
     * @return 解析后的规则，解析失败返回 null
     */
    private static Rule parseSingleRule(String ruleStr) {
        if (ruleStr.length() < 4 || ruleStr.charAt(0) != 's') {
            return null;
        }

        char delimiter = ruleStr.charAt(1);
        // 查找第二个分隔符的位置
        int secondDelim = findUnescapedDelimiter(ruleStr, delimiter, 2);
        if (secondDelim < 0) {
            return null;
        }

        // 查找第三个分隔符的位置
        int thirdDelim = findUnescapedDelimiter(ruleStr, delimiter, secondDelim + 1);

        String regex;
        String replacement;
        String flags;

        if (thirdDelim < 0) {
            // 没有第三个分隔符，replacement 为空，flags 为空
            regex = ruleStr.substring(2, secondDelim);
            replacement = "";
            flags = "";
        } else {
            regex = ruleStr.substring(2, secondDelim);
            replacement = ruleStr.substring(secondDelim + 1, thirdDelim);
            flags = ruleStr.substring(thirdDelim + 1);
        }

        // 处理转义的分隔符
        regex = regex.replace("\\" + delimiter, String.valueOf(delimiter));
        replacement = replacement.replace("\\" + delimiter, String.valueOf(delimiter));

        // 解析标志
        boolean global = flags.contains("g");
        boolean caseInsensitive = flags.contains("i");

        try {
            int patternFlags = 0;
            if (caseInsensitive) {
                patternFlags |= Pattern.CASE_INSENSITIVE;
            }
            Pattern pattern = Pattern.compile(regex, patternFlags);
            return new Rule(pattern, replacement, global);
        } catch (Exception e) {
            // 正则表达式解析失败，跳过该规则
            return null;
        }
    }

    /**
     * 查找未转义的分隔符位置
     *
     * @param str      源字符串
     * @param delim    分隔符
     * @param fromIdx  起始位置
     * @return 分隔符位置，未找到返回 -1
     */
    private static int findUnescapedDelimiter(String str, char delim, int fromIdx) {
        for (int i = fromIdx; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                i++; // 跳过转义字符
                continue;
            }
            if (c == delim) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 对单条文本应用单条规则
     *
     * @param text 文本
     * @param rule 规则
     * @return 替换后的文本
     */
    private static String applySingleRule(String text, Rule rule) {
        Matcher matcher = rule.pattern.matcher(text);
        if (rule.global) {
            return matcher.replaceAll(rule.replacement);
        } else {
            return matcher.replaceFirst(rule.replacement);
        }
    }

    /**
     * 单条替换规则
     */
    public static class Rule {
        private final Pattern pattern;
        private final String replacement;
        private final boolean global;

        public Rule(Pattern pattern, String replacement, boolean global) {
            this.pattern = pattern;
            this.replacement = replacement;
            this.global = global;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String getReplacement() {
            return replacement;
        }

        public boolean isGlobal() {
            return global;
        }
    }
}
