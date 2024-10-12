package com.wei.wreader.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

public class UrlUtil {

    /**
     * 转化html中href的链接，转化成完整的url
     *
     * @param href
     * @param location
     * @param baseUrl
     * @return
     */
    public static String hrefInvert(String href, String location, String baseUrl) {
        if (!href.contains("http://") && !href.contains("https://")) {
            if (location.startsWith("/")) {
                href = baseUrl + href;
            } else if (location.startsWith("./") ) {
                href = href.replace("./", "");
                href = location + href;
            } else if (location.startsWith("../")) {

            }
        }

        return href;
    }


    /**
     * 根据基础URL和相对路径构建完整的URL。
     *
     * @param baseUrl 基础URL（应包含协议、域名和可能的端口）
     * @param relativePath 相对路径（可以包含`/`、`./`、`../`）
     * @return 完整的URL字符串
     * @throws MalformedURLException 如果基础URL格式不正确
     */
    public static String buildFullURL(String baseUrl, String relativePath) throws MalformedURLException {
        if (relativePath.contains("http://") || relativePath.contains("https://")) {
            return relativePath;
        }

        // 创建一个URL对象
        URL base = new URL(baseUrl);

        // 处理以`/`开头的相对路径，将其视为从根目录开始的路径
        if (relativePath.startsWith("/")) {
            return new URL(base.getProtocol(), base.getHost(), base.getPort(), relativePath).toString();
        }

        // 使用java.nio.file.Paths来处理相对路径
        // 利用normalize方法来处理`./`和`../`
        String normalizedPath = Paths.get(base.getPath(), relativePath).normalize().toString();
        normalizedPath = normalizedPath.replaceAll("\\\\", "/");

        // 构建完整的URL字符串
        // 注意：这里我们假设基础URL的查询字符串和锚点部分（如果有的话）不应该被修改
        // 因此我们只替换了路径部分
        URL fullUrl = new URL(base.getProtocol(), base.getHost(), base.getPort(), normalizedPath);

        // 返回完整的URL字符串
        return fullUrl.toString();
    }

}
