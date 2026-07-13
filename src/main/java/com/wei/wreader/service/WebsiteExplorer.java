package com.wei.wreader.service;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网站链路探索器 - 负责页面获取和 HTML 清理，不负责选择器分析（由 LLM 完成）
 */
public class WebsiteExplorer {
    private static final Logger LOG = Logger.getInstance(WebsiteExplorer.class);

    // ==================== 数据结构 ====================

    public static class FetchedPage {
        public String url;
        public String method;
        public int statusCode;
        public boolean isJson;
        public String rawBody;         // 原始响应
        public String cleanedBodyHtml; // 原始响应
        public String cleanedHtml;     // 清理后的 HTML（给 LLM 分析用）
        public String errorMsg;
    }

    public static class SearchInfo {
        public String urlTemplate;   // 含 ${key} 占位符
        public String method;        // GET / POST
        public String paramName;     // 搜索参数名
        public String detectedFrom;  // form / js-file / schema.org / pattern
        public String apiUrl;        // JS 中发现的 API 端点（如 /api/search）
        public String apiMethod;     // API 请求方法
        public Map<String, String> apiExtraParams; // API 额外参数
    }

    // ==================== 页面获取 ====================

    /**
     * 获取页面并清理 HTML
     */
    public FetchedPage fetchAndClean(String url) {
        return fetchAndClean(url, "GET", null);
    }

    public FetchedPage fetchAndClean(String url, String method, Map<String, String> bodyParams) {
        FetchedPage page = new FetchedPage();
        page.url = url;
        page.method = method;

        try {
            HttpRequestBase request;
            if ("POST".equals(method) && bodyParams != null) {
                HttpPost post = new HttpPost(url);
                StringBuilder formBody = new StringBuilder();
                for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                    if (formBody.length() > 0) formBody.append("&");
                    formBody.append(entry.getKey()).append("=").append(entry.getValue());
                }
                post.setEntity(new StringEntity(formBody.toString(), StandardCharsets.UTF_8));
                post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request = post;
            } else {
                request = new HttpGet(url);
            }

            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            request.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            request.setHeader("Accept-Encoding", "identity");

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(request)) {

                page.statusCode = response.getStatusLine().getStatusCode();
                if (response.getEntity() != null) {
                    String contentType = response.getEntity().getContentType() != null
                            ? response.getEntity().getContentType().getValue() : "";
                    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    page.rawBody = body;
                    page.isJson = isJsonResponse(contentType, body);

                    if (!page.isJson && body.length() > 100) {
//                        page.cleanedBodyHtml = cleanHtml(body, url);
                        page.cleanedHtml = cleanHtml(body, url);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to fetch URL: " + url, e);
            page.statusCode = -1;
            page.errorMsg = e.getMessage();
        }

        // 检测反爬保护
        if (page.rawBody != null) {
            String lower = page.rawBody.toLowerCase();
            if (lower.contains("just a moment") || lower.contains("cloudflare")
                    || lower.contains("cf-chl") || lower.contains("challenge-platform")) {
                page.errorMsg = "Cloudflare 反爬保护，需要浏览器验证";
            } else if (lower.contains("access denied") || lower.contains("403 forbidden")) {
                page.errorMsg = "访问被拒绝 (403)";
            } else if (lower.contains("captcha") || lower.contains("verify you are human")) {
                page.errorMsg = "验证码保护";
            }
        }

        return page;
    }

    /**
     * 清理 HTML：移除 script/style/comment，保留结构，限制长度
     */
    private String cleanHtml(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);

        // 移除 script、style、noscript、svg、link、meta 等非内容标签
        doc.select("style, noscript, svg, link[rel=stylesheet], meta").remove();
//        doc.select("script, style, noscript, svg, link[rel=stylesheet], meta, head comment").remove();

        // 移除 HTML 注释
        doc.select("*").forEach(el -> {
            // Jsoup 不直接支持注释移除，但上面的处理已经足够
        });

        String cleaned = doc.html();

        // 获取 body 内容
//        Element body = doc.body();
//        if (body == null) return "";

//        String cleaned = body.html();

        // 移除多余空白
        cleaned = cleaned.replaceAll(">\\s+<", "><");
        cleaned = cleaned.replaceAll("\\s{2,}", " ");

        // 限制长度：保留前 25000 字符
        if (cleaned.length() > 25000) {
            cleaned = cleaned.substring(0, 25000) + "\n... (HTML truncated)";
        }

        return cleaned;
    }

    // ==================== 搜索发现 ====================

    /**
     * 从首页发现搜索机制（只做 URL 发现，不做内容分析）
     */
    public SearchInfo discoverSearch(Document homeDoc, String baseUrl) {
        SearchInfo info = new SearchInfo();
        info.method = "GET";

        // 优先级1: HTML form
        for (Element form : homeDoc.select("form")) {
            String action = form.attr("action");
            if (action.isEmpty() || action.equals("#")) continue;
            Element input = form.selectFirst("input[type=search],input[type=text],input[name*=key],input[name*=q],input[name*=wd],input[name*=word],input[name*=search]");
            if (input != null) {
                return buildSearchInfo(action, form.attr("method"), input.attr("name"), baseUrl, "form");
            }
        }

        // 优先级2: 外部 JS 文件中的 form + API 端点
        Elements scriptSrcs = homeDoc.select("script[src]");
        for (Element script : scriptSrcs) {
            String src = script.attr("src");
            if (src.isEmpty()) continue;
            String jsUrl = resolveUrl(baseUrl, src);
            String jsContent = fetchJsContent(jsUrl);
            if (jsContent != null) {
                // 先找 form
                SearchInfo jsInfo = extractSearchFromJs(jsContent, baseUrl);
                // 再找 API 端点
                String apiUrl = extractApiEndpoint(jsContent, "search");
                if (apiUrl != null && !apiUrl.startsWith("http")) apiUrl = resolveUrl(baseUrl, apiUrl);
                if (jsInfo != null) {
                    jsInfo.apiUrl = apiUrl;
                    // 如果找到了 API 端点，从 JS 中提取额外参数
                    if (apiUrl != null) {
                        jsInfo.apiMethod = "POST";
                        jsInfo.apiExtraParams = extractApiParams(jsContent);
                    }
                    return jsInfo;
                }
                if (apiUrl != null) {
                    info.urlTemplate = apiUrl;
                    info.paramName = "q";
                    info.method = "POST";
                    info.detectedFrom = "js-api";
                    info.apiUrl = apiUrl;
                    info.apiMethod = "POST";
                    info.apiExtraParams = extractApiParams(jsContent);
                    return info;
                }
            }
        }

        // 优先级3: 内联 JS
        for (Element script : homeDoc.select("script:not([src])")) {
            String jsContent = script.html();
            if (jsContent.contains("action") && (jsContent.contains("search") || jsContent.contains("form"))) {
                SearchInfo jsInfo = extractSearchFromJs(jsContent, baseUrl);
                if (jsInfo != null) return jsInfo;
            }
        }

        // 优先级4: Schema.org SearchAction
        for (Element script : homeDoc.select("script[type=application/ld+json]")) {
            String json = script.html();
            if (json.contains("SearchAction")) {
                Pattern p = Pattern.compile("\"target\"\\s*:\\s*\"([^\"]+\\{[^}]+\\}[^\"]*?)\"");
                Matcher m = p.matcher(json);
                if (m.find()) {
                    info.urlTemplate = m.group(1).replaceAll("\\{[^}]+\\}", "${key}");
                    info.paramName = "q";
                    info.detectedFrom = "schema.org";
                    return info;
                }
            }
        }

        // 优先级5: 页面链接中的搜索页
        for (Element a : homeDoc.select("a[href]")) {
            String href = a.attr("href");
            if (href.contains("/search") && !href.contains(".css") && !href.contains(".js")) {
                info.urlTemplate = resolveUrl(baseUrl, href.contains("?") ? href + "&q=${key}" : href + "?q=${key}");
                info.paramName = "q";
                info.detectedFrom = "page-link";
                return info;
            }
        }

        // 优先级6: 常见模式
        info.urlTemplate = baseUrl + "/search?q=${key}";
        info.paramName = "q";
        info.detectedFrom = "pattern-default";
        return info;
    }

    /**
     * 执行搜索请求 - 优先尝试 API，其次尝试 HTML
     */
    public FetchedPage executeSearch(SearchInfo searchInfo, String keyword) {
        if (searchInfo == null) return null;

        // 优先尝试 API 端点
        if (searchInfo.apiUrl != null) {
            System.out.println("[Search] 尝试API端点: " + searchInfo.apiUrl + " method=" + searchInfo.apiMethod);
            Map<String, String> params = new LinkedHashMap<>();
            params.put(searchInfo.paramName, keyword);
            if (searchInfo.apiExtraParams != null) {
                params.putAll(searchInfo.apiExtraParams);
            }
            FetchedPage apiResult = fetchAndClean(searchInfo.apiUrl, searchInfo.apiMethod != null ? searchInfo.apiMethod : "POST", params);
            if (apiResult != null && apiResult.statusCode == 200 && apiResult.rawBody != null && apiResult.rawBody.length() > 50) {
                apiResult.isJson = true; // API 响应通常是 JSON
                System.out.println("[Search] API响应成功: " + apiResult.rawBody.length() + "字节");
                return apiResult;
            }
            System.out.println("[Search] API响应失败或内容过短，回退到HTML");
        }

        // 回退到 HTML 表单方式
        if (searchInfo.urlTemplate == null) return null;
        String testUrl = searchInfo.urlTemplate.replace("${key}", keyword);
        System.out.println("[Search] 尝试HTML: " + testUrl + " method=" + searchInfo.method);

        if ("POST".equals(searchInfo.method)) {
            Map<String, String> bodyParams = new LinkedHashMap<>();
            bodyParams.put(searchInfo.paramName, keyword);
            return fetchAndClean(testUrl, "POST", bodyParams);
        } else {
            return fetchAndClean(testUrl, "GET", null);
        }
    }

    // ==================== 工具方法 ====================

    private SearchInfo buildSearchInfo(String action, String method, String inputName, String baseUrl, String source) {
        SearchInfo info = new SearchInfo();
        if (inputName == null || inputName.isEmpty()) inputName = "q";
        info.paramName = inputName;
        info.detectedFrom = source;
        info.method = (method != null && !method.isEmpty()) ? method.toUpperCase() : "GET";

        String fullAction = resolveUrl(baseUrl, action);
        if ("GET".equals(info.method)) {
            info.urlTemplate = fullAction.contains("?")
                    ? fullAction + "&" + inputName + "=${key}"
                    : fullAction + "?" + inputName + "=${key}";
        } else {
            info.urlTemplate = fullAction;
        }
        return info;
    }

    private SearchInfo extractSearchFromJs(String jsContent, String baseUrl) {
        // 模式1: window.location.href = '/search.html?q=' + keyword
        Pattern locationPattern = Pattern.compile("(?:window\\.)?location(?:\\.href)?\\s*=\\s*['\"]([^'\"]*search[^'\"]*?)[\\?&]([^='\"]*)=\\$\\{?\\s*(?:encodeURIComponent\\s*\\()?\\s*(\\w+)");
        Matcher locationMatcher = locationPattern.matcher(jsContent);
        while (locationMatcher.find()) {
            String searchPath = locationMatcher.group(1);
            String paramName = locationMatcher.group(2);
            System.out.println("[Search] 发现JS location跳转: " + searchPath + " 参数=" + paramName);
            SearchInfo info = buildSearchInfo(searchPath, "GET", paramName, baseUrl, "js-location");
            return info;
        }

        // 模式2: window.location.href = '/search.html?q=' + keyword (简单模式)
        Pattern locationSimplePattern = Pattern.compile("location(?:\\.href)?\\s*=\\s*['\"]([^'\"]*search[^'\"]*?\\?[^'\"]*)['\"]");
        Matcher locationSimpleMatcher = locationSimplePattern.matcher(jsContent);
        while (locationSimpleMatcher.find()) {
            String url = locationSimpleMatcher.group(1);
            // 从 URL 中提取参数名
            String paramName = "q";
            if (url.contains("keyword=")) paramName = "keyword";
            else if (url.contains("searchkey=")) paramName = "searchkey";
            else if (url.contains("key=")) paramName = "key";
            else if (url.contains("wd=")) paramName = "wd";
            // 提取路径部分（去掉查询参数）
            String path = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
            System.out.println("[Search] 发现JS location跳转(简单): " + path + " 参数=" + paramName);
            return buildSearchInfo(path, "GET", paramName, baseUrl, "js-location");
        }

        // 模式3: action="/search/" form 方式
        Pattern actionPattern = Pattern.compile("action=['\"]([^'\"]+)['\"]");
        Matcher actionMatcher = actionPattern.matcher(jsContent);

        while (actionMatcher.find()) {
            String action = actionMatcher.group(1);
            int pos = actionMatcher.start();
            String context = jsContent.substring(Math.max(0, pos - 300), Math.min(jsContent.length(), pos + 300));
            String lowerContext = context.toLowerCase();

            if (lowerContext.contains("form") || lowerContext.contains("search")
                    || lowerContext.contains("input") || lowerContext.contains("writeln")) {

                String method = "GET";
                Pattern methodPattern = Pattern.compile("method=['\"]([^'\"]+)['\"]");
                Matcher methodMatcher = methodPattern.matcher(context);
                if (methodMatcher.find()) method = methodMatcher.group(1).toUpperCase();

                // 优先查找 input 的 name（实际参数名），而非 form 的 name
                String paramName = null;
                Pattern inputNamePattern = Pattern.compile("<input[^>]*name=['\"]([^'\"]+)['\"]");
                Matcher inputNameMatcher = inputNamePattern.matcher(context);
                while (inputNameMatcher.find()) {
                    String name = inputNameMatcher.group(1);
                    String inputTag = inputNameMatcher.group(0).toLowerCase();
                    if (!inputTag.contains("type=\"hidden\"") && !inputTag.contains("type='hidden'")
                            && !name.equalsIgnoreCase("Submit") && !name.equalsIgnoreCase("action")) {
                        paramName = name;
                        break;
                    }
                }
                if (paramName == null) {
                    Pattern namePattern = Pattern.compile("name=['\"]([^'\"]+)['\"]");
                    Matcher nameMatcher = namePattern.matcher(context);
                    if (nameMatcher.find()) paramName = nameMatcher.group(1);
                }
                if (paramName == null || paramName.isEmpty()) paramName = "q";

                return buildSearchInfo(action, method, paramName, baseUrl, "js-form");
            }
        }
        return null;
    }

    /**
     * 从 JS 中提取搜索相关的 API 端点
     * 匹配模式: $.post('/xxx'), $.ajax({url:'/xxx'}), fetch('/xxx'), XMLHttpRequest.open(), var url = '/xxx' 等
     */
    private String extractApiEndpoint(String jsContent, String keyword) {
        // 1. $.post('/xxx') 或 $.post("/xxx")
        Pattern postPattern = Pattern.compile("\\$\\.post\\s*\\(\\s*['\"]([^'\"]+)['\"]");
        Matcher postMatcher = postPattern.matcher(jsContent);
        while (postMatcher.find()) {
            String endpoint = postMatcher.group(1);
            if (isSearchRelated(endpoint, jsContent, postMatcher.start())) return endpoint;
        }

        // 2. $.ajax({url: '/xxx'}) 或 url: '/xxx' 在 search 上下文中
        Pattern urlPattern = Pattern.compile("url\\s*:\\s*['\"]([^'\"]+)['\"]");
        Matcher urlMatcher = urlPattern.matcher(jsContent);
        while (urlMatcher.find()) {
            String endpoint = urlMatcher.group(1);
            if (isSearchRelated(endpoint, jsContent, urlMatcher.start())) return endpoint;
        }

        // 3. fetch('/xxx')
        Pattern fetchPattern = Pattern.compile("fetch\\s*\\(\\s*['\"]([^'\"]+)['\"]");
        Matcher fetchMatcher = fetchPattern.matcher(jsContent);
        while (fetchMatcher.find()) {
            String endpoint = fetchMatcher.group(1);
            if (isSearchRelated(endpoint, jsContent, fetchMatcher.start())) return endpoint;
        }

        // 4. var url = '/xxx' 或 let/const url = '/xxx' 在 search 上下文中
        Pattern varUrlPattern = Pattern.compile("(?:var|let|const)\\s+\\w*[Uu]rl\\w*\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher varUrlMatcher = varUrlPattern.matcher(jsContent);
        while (varUrlMatcher.find()) {
            String endpoint = varUrlMatcher.group(1);
            if (isSearchRelated(endpoint, jsContent, varUrlMatcher.start())) return endpoint;
        }

        // 5. 兜底：找包含 search 关键词的路径
        Pattern searchPathPattern = Pattern.compile("['\"](/[a-zA-Z0-9_/]*search[a-zA-Z0-9_/]*)['\"]");
        Matcher searchPathMatcher = searchPathPattern.matcher(jsContent);
        while (searchPathMatcher.find()) {
            String endpoint = searchPathMatcher.group(1);
            if (!endpoint.contains(".css") && !endpoint.contains(".js") && !endpoint.contains(".html")) {
                return endpoint;
            }
        }

        return null;
    }

    /**
     * 判断一个 URL 是否与搜索相关（通过上下文和关键词）
     */
    private boolean isSearchRelated(String endpoint, String jsContent, int pos) {
        String lower = endpoint.toLowerCase();
        // URL 本身包含搜索关键词
        if (lower.contains("search") || lower.contains("query") || lower.contains("find")) return true;
        // URL 本身不是静态资源
        if (lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".html")
                || lower.endsWith(".png") || lower.endsWith(".jpg")) return false;
        // 检查上下文（前后 200 字符）是否包含搜索关键词
        int start = Math.max(0, pos - 200);
        int end = Math.min(jsContent.length(), pos + endpoint.length() + 200);
        String context = jsContent.substring(start, end).toLowerCase();
        if (context.contains("search") || context.contains("keyword") || context.contains("bookname")
                || context.contains("book_name") || context.contains("searchkey")) return true;
        return false;
    }

    /**
     * 从 JS 中提取 API 调用时的额外参数（如 sign, token 等静态变量）
     */
    private Map<String, String> extractApiParams(String jsContent) {
        Map<String, String> params = new LinkedHashMap<>();
        // 匹配 var xxx = "value"; 模式中的静态变量
        Pattern varPattern = Pattern.compile("var\\s+(\\w+)\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher varMatcher = varPattern.matcher(jsContent);
        while (varMatcher.find()) {
            String name = varMatcher.group(1);
            String value = varMatcher.group(2);
            // 排除常见的非参数变量
            if (!name.equals("html") && !name.equals("url") && !name.equals("method")
                    && !name.equals("type") && !name.equals("data") && !name.equals("result")
                    && value.length() > 3 && value.length() < 100) {
                params.put(name, value);
            }
        }
        return params.isEmpty() ? null : params;
    }

    private String fetchJsContent(String jsUrl) {
        try {
            HttpGet request = new HttpGet(jsUrl);
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            request.setHeader("Accept-Encoding", "identity");
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null) {
                    String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    return content.length() > 50000 ? content.substring(0, 50000) : content;
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to fetch JS: " + jsUrl, e);
        }
        return null;
    }

    private boolean isJsonResponse(String contentType, String body) {
        if (contentType != null && contentType.contains("application/json")) return true;
        if (body != null) {
            String trimmed = body.trim();
            return (trimmed.startsWith("{") || trimmed.startsWith("["));
        }
        return false;
    }

    public String normalizeBaseUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String resolveUrl(String baseUrl, String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) return null;
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) return relativeUrl;
        if (relativeUrl.startsWith("//")) return "https:" + relativeUrl;
        try {
            URI base = URI.create(baseUrl);
            URL resolved = base.resolve(relativeUrl).toURL();
            return resolved.toString();
        } catch (Exception e) {
            return relativeUrl.startsWith("/") ? baseUrl + relativeUrl : baseUrl + "/" + relativeUrl;
        }
    }
}
