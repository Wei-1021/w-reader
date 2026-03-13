package com.wei.wreader.utils.http;

import com.intellij.openapi.ui.Messages;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Http请求工具类
 *
 * @author weizhanjie
 */
public class HttpUtil {

    public static final String GET = "GET";

    public static final String POST = "POST";

    /**
     * 通用请求
     *
     * @param config 请求配置 or 请求url
     * @return
     */
    public static @NotNull HttpRequestBase commonRequest(String config) {
        HttpRequestConfigParser parser = new HttpRequestConfigParser(config);
        String requestUrl = parser.getUrl();
        String requestMethod = parser.getMethod();
        Map<String, String> queryParam = parser.getQueryParams();
        Map<String, String> bodyParam = parser.getBodyParams();
        Map<String, String> header = parser.getHeader();

        if (POST.equalsIgnoreCase(requestMethod)) {
            // 处理POST请求
            // 处理query请求参数
            if (queryParam != null && !queryParam.isEmpty()) {
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String> entry : queryParam.entrySet()) {
                    queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
                requestUrl += "?" + queryString.substring(0, queryString.length() - 1);
            }
            HttpPost httpPost = new HttpPost(requestUrl);
            // 处理body请求参数
            if (bodyParam != null && !bodyParam.isEmpty()) {
                List<NameValuePair> formParams = new ArrayList<>();
                for (Map.Entry<String, String> entry : bodyParam.entrySet()) {
                    formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }

                try {
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
                    httpPost.setEntity(entity);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            // 处理header
            if (header != null && !header.isEmpty()) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }

            return httpPost;
        } else if (GET.equalsIgnoreCase(requestMethod)) {
            // 处理GET请求
            if (queryParam != null && !queryParam.isEmpty()) {
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String> entry : queryParam.entrySet()) {
                    queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
                requestUrl += "?" + queryString.substring(0, queryString.length() - 1);
            }
            HttpGet httpGet = new HttpGet(requestUrl);
            // 处理header
            if (header != null && !header.isEmpty()) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }
            return httpGet;
        } else {
            throw new RuntimeException("不支持的请求方法");
        }
    }

    /**
     * 请求Html
     * @param url 请求url
     * @param method 请求方法 GET/POST
     * @param queryParams 查询参数
     * @param bodyParams 请求体参数
     * @param headers 请求头
     * @return  html文档对象
     */
    public static Document requestHtml(String url, String method, Map<String, String> queryParams,
                                       Map<String, String> bodyParams, Map<String, String> headers) {
        int maxRetries = 3;  // 最大重试次数
        int retryDelay = 1000;  // 重试延迟毫秒数

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 处理query请求参数
                if (queryParams != null && !queryParams.isEmpty()) {
                    StringBuilder queryString = new StringBuilder();
                    for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                        queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                    }
                    url += "?" + queryString.substring(0, queryString.length() - 1);
                }

                Connection connection = Jsoup.connect(url)
                        .timeout(10000)  // 设置10秒超时
                        .ignoreContentType(true)  // 忽略内容类型检查
                        .ignoreHttpErrors(true);  // 忽略HTTP错误

                // 禁用GZIP压缩以避免EOFException
                connection.header("Accept-Encoding", "identity");

                // 设置请求头
                if (headers != null && !headers.isEmpty()) {
                    connection.headers(headers);
                }

                // 设置请求方法和参数
                if (HttpUtil.POST.equals(method)) {
                    connection.method(Connection.Method.POST);
                    if (bodyParams != null) {
                        for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                            connection.data(entry.getKey(), entry.getValue());
                        }
                    }
                    connection.header("Content-Type", "application/x-www-form-urlencoded");
                } else {
                    connection.method(Connection.Method.GET);
                }

                Connection.Response response = connection.execute();

                // 检查响应状态码
                if (response.statusCode() >= 400) {
                    throw new IOException("HTTP " + response.statusCode() + ": " + response.statusMessage());
                }

                return response.parse();

            } catch (IOException e) {
                // 如果是EOFException或其他网络相关异常，且还有重试机会
                if (e instanceof EOFException ||
                        e.getMessage().contains("Unexpected end of ZLIB input stream") ||
                        e.getMessage().contains("Connection reset") ||
                        e.getMessage().contains("Read timed out")) {

                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(retryDelay * attempt);  // 指数退避
                            System.err.println("网络请求失败，正在进行第 " + (attempt + 1) + " 次重试...");
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                }

                // 如果是最后一次尝试或非网络异常，记录错误并返回null
                e.printStackTrace();
                Messages.showErrorDialog("网络请求异常 (" + attempt + "/" + maxRetries + "): " +
                        e.getMessage() + "\n请检查网络连接或稍后重试！", "提示");
                return null;
            }
        }

        return null;
    }
}



