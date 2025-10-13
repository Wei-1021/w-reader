package com.wei.wreader.utils.http;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>HTTP请求配置解析器;</p>
 * <p>解析配置，并将结果保存在相应的属性中。</p>
 * <pre style="font-size: 10px;">
 *     <b>配置字符串格式</b>：url=url;method=请求方式;query_params=参数1=值1&参数2=值2;body_params=参数3=值3&参数4=值4
 *     <b>配置项</b>: <b>url</b>：URL地址;
 *            <b>method</b>：HTTP方法，默认为GET;
 *            <b>query_params</b>：查询参数，格式为参数1=值1&参数2=值2;
 *            <b>body_params</b>：请求体参数，格式为参数3=值3&参数4=值4
 *     <b>示例配置字符串</b>：
 *     url=http://example.com/api/data;method=POST;query_params=param1=value1&param2=value2;body_params=param3=value3&param4=value4
 * </pre>
 *
 * @author weizhanjie
 */
public class HttpRequestConfigParserOld {

    /**
     * 存储URL
     */
    private String url;
    /**
     * 存储HTTP方法，默认为GET
     */
    private String method;
    /**
     * 存储查询参数
     */
    private Map<String, String> queryParams;
    /**
     * 存储请求体参数
     */
    private Map<String, String> bodyParams;

    /**
     * 构造函数，接收配置字符串并进行解析
     *
     * @param config 包含HTTP请求配置的字符串
     */
    public HttpRequestConfigParserOld(String config) {
        parseConfig(config);
    }

    /**
     * 解析配置字符串，并设置相应的属性
     *
     * @param config 包含HTTP请求配置的字符串
     */
    private void parseConfig(String config) {
        this.url = "";
        this.method = "GET";
        this.queryParams = new HashMap<>();
        this.bodyParams = new HashMap<>();

        // 标记是否找到有效的URL
        boolean validConfig = false;

        String[] lines = config.split(";"); // 将配置字符串;分割
        for (String line : lines) {
            if (line.startsWith("url=")) {
                this.url = line.substring(4).trim(); // 提取URL
                validConfig = true; // 设置标志为有效配置
            } else if (validConfig && line.startsWith("method=")) {
                this.method = line.substring(7).trim().toUpperCase(); // 提取并转换为大写的HTTP方法
            } else if (validConfig && line.startsWith("query_params=")) {
                String paramStr = line.substring(13).trim(); // 提取查询参数字符串
                parseParams(paramStr, queryParams); // 解析查询参数
            } else if (validConfig && line.startsWith("body_params=")) {
                String paramStr = line.substring(12).trim(); // 提取请求体参数字符串
                parseParams(paramStr, bodyParams); // 解析请求体参数
            }
        }

        // 如果没有找到有效的URL，则清空所有字段
        if (!validConfig) {
            this.url = config;
            this.method = "GET";
            this.queryParams.clear();
            this.bodyParams.clear();
        }
    }

    /**
     * 解析参数字符串，并存入指定的映射中
     *
     * @param paramStr  参数字符串
     * @param paramsMap 存储参数的映射
     */
    private void parseParams(String paramStr, Map<String, String> paramsMap) {
        String[] keyValuePairs = paramStr.split("&"); // 按"&"分割键值对
        for (String pair : keyValuePairs) {
            int idx = pair.indexOf('='); // 查找"="的位置
            if (idx > 0 && idx < pair.length() - 1) {
                String key = pair.substring(0, idx).trim(); // 提取键
                String value = pair.substring(idx + 1).trim(); // 提取值
                paramsMap.put(key, value); // 将键值对存入映射
            }
        }
    }

    /**
     * 获取URL
     *
     * @return URL字符串
     */
    public String getUrl() {
        return url;
    }

    /**
     * 获取HTTP方法
     *
     * @return HTTP方法字符串
     */
    public String getMethod() {
        return method;
    }

    /**
     * 获取查询参数
     *
     * @return 查询参数映射
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    /**
     * 获取请求体参数
     *
     * @return 请求体参数映射
     */
    public Map<String, String> getBodyParams() {
        return bodyParams;
    }

    /**
     * 返回解析结果的字符串表示形式
     *
     * @return 字符串表示形式
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("URL: ").append(url).append("\n");
        sb.append("Method: ").append(method).append("\n");
        sb.append("Query Params: ");
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        sb.append("\nBody Params: ");
        for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        // 测试配置字符串
        String invalidConfig = "url=http://www.lianjianxsw.com/search;method=POST;body_params=keyword={key}";

        HttpRequestConfigParserOld invalidParser = new HttpRequestConfigParserOld(invalidConfig);
        System.out.println(invalidParser);

        // 获取解析后的值
        System.out.println("Parsed URL: " + invalidParser.getUrl());
        System.out.println("Parsed Method: " + invalidParser.getMethod());
        System.out.println("Parsed Query Params: " + invalidParser.getQueryParams());
        System.out.println("Parsed Body Params: " + invalidParser.getBodyParams());
    }
}



