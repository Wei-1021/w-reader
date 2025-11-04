package com.wei.wreader.utils.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>HTTP请求配置解析器;</p>
 * <p>解析配置，并将结果保存在相应的属性中。</p>
 * <pre style="font-size: 10px;">
 *     <b>配置字符串格式</b>：
 *     {
 *       'url': URL地址,
 *       'method': 请求类型,
 *       'queryParams': {
 *          '参数1': '值1',
 *          '参数2': '值2'
 *       },
 *       'bodyParams': {
 *          '参数1': '值1',
 *          '参数2': '值2'
 *       },
 *       'header':{
 *           '请求头参数1': '值1'
 *       }
 *     }
 *     <b>配置项</b>: <b>url</b>：URL地址;
 *            <b>method</b>：请求类型，默认为GET;
 *            <b>queryParams</b>：查询参数;
 *            <b>bodyParams</b>：请求体参数
 *            <b>header</b>：请求头
 *     <b>示例配置字符串</b>：
 *     {
 *       'url':'http://www.lianjianxsw.com/search',
 *       'method': 'POST',
 *       'queryParams': {
 *          'q': '123'
 *       },
 *       'bodyParams': {
 *          'keyword':'${key}'
 *       },
 *       'header':{
 *           'Content-Type': 'text/html; charset=utf-8'
 *       }
 *     }
 * </pre>
 *
 * @author weizhanjie
 */
public class HttpRequestConfigParser {

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
     * 请求头
     */
    private Map<String, String> header;

    /**
     * 构造函数，接收配置字符串并进行解析
     *
     * @param config 包含HTTP请求配置的字符串
     */
    public HttpRequestConfigParser(String config) {
        parseConfig(config);
    }

    /**
     * 解析配置字符串，并设置相应的属性
     *
     * @param config 包含HTTP请求配置的字符串
     */
    private void parseConfig(String config) {
        // 标记是否找到有效的JSON字符串
        boolean validJson = true;

        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject = gson.fromJson(config, JsonObject.class);
        } catch (Exception e) {
            validJson = false;
        }

        this.url = "";
        this.method = "GET";
        this.queryParams = new HashMap<>();
        this.bodyParams = new HashMap<>();
        this.header = new HashMap<>();

        // 如果没有找到有效的URL，则清空所有字段
        if (validJson) {
            this.url = jsonObject.get("url").getAsString();
            this.method = jsonObject.get("method").getAsString();
            JsonObject queryParamsJson = jsonObject.getAsJsonObject("queryParams");
            if (!queryParamsJson.isEmpty()) {
                this.queryParams = gson.fromJson(queryParamsJson, Map.class);
            }
            JsonObject bodyParamsJson = jsonObject.getAsJsonObject("bodyParams");
            if (!bodyParamsJson.isEmpty()) {
                this.bodyParams = gson.fromJson(bodyParamsJson, Map.class);
            }
            JsonObject headerJson = jsonObject.getAsJsonObject("header");
            if (!headerJson.isEmpty()) {
                this.header = gson.fromJson(headerJson, Map.class);
            }
        } else {
            this.url = config;
            this.method = "GET";
            this.queryParams.clear();
            this.bodyParams.clear();
            this.header.clear();
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
     * 获取请求头
     *
     * @return
     */
    public Map<String, String> getHeader() {
        return header;
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
        sb.append("\nHeader: ");
        for (Map.Entry<String, String> entry : header.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        // 测试配置字符串
//        String invalidConfig = "url=http://www.lianjianxsw.com/search;method=POST;body_params=keyword={key}";
        String invalidConfig =
                """
                                {
                                    'url': 'http://www.lianjianxsw.com/search',
                                    'method': 'POST',
                                    'queryParams': {
                                        'tt': '123456'
                                    },
                                    'bodyParams': {
                                        'keyword': '${key}'
                                    },
                                    'header': {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                }
                        """;

        HttpRequestConfigParser invalidParser = new HttpRequestConfigParser(invalidConfig);
        System.out.println(invalidParser);

        // 获取解析后的值
        System.out.println("Parsed URL: " + invalidParser.getUrl());
        System.out.println("Parsed Method: " + invalidParser.getMethod());
        System.out.println("Parsed Query Params: " + invalidParser.getQueryParams());
        System.out.println("Parsed Body Params: " + invalidParser.getBodyParams());
        System.out.println("Parsed Body Params: " + invalidParser.getHeader());
    }

    public String execute(String chapterUrl, String preContentUrlTemp, String bodyElement) {
        return (bodyElement.contains("next.png") ?
                (preContentUrlTemp == null || preContentUrlTemp.isEmpty() ?
                        incrementPage(chapterUrl) : incrementPage(preContentUrlTemp)) :
                "");
    }

    public String incrementPage(String url) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)(?:_(\\d+))?(\\.html?)$");
        java.util.regex.Matcher m = p.matcher(url);
        if (!m.find()) return url;
        int major = Integer.parseInt(m.group(1)), minor = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        minor++;
        return m.replaceFirst(major + "_" + minor + m.group(3));
    }
}



