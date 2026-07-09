package com.wei.wreader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.llm.LLMClient;
import com.wei.wreader.llm.LLMClient.Conversation;
import com.wei.wreader.service.WebsiteExplorer.*;
import com.wei.wreader.util.CustomSiteUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 生成书源规则编排器 - 每一步都将页面发给 LLM 分析
 */
public class SiteRuleGenerator {
    private static final Logger LOG = Logger.getInstance(SiteRuleGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Project project;

    public SiteRuleGenerator(Project project) {
        this.project = project;
    }

    // ==================== 上下文 ====================

    /** 每一步的分析结果 */
    private static class StepContext {
        WebsiteExplorer explorer;
        LLMClient llmClient;
        Conversation conversation; // 共享对话上下文，提升缓存命中率
        String baseUrl;
        String siteName;
        SearchInfo searchInfo;
        String searchAnalysis;
        String firstBookUrl;
        String bookAnalysis;
        String chapterAnalysis;
    }

    // ==================== 主流程 ====================

    public void generate(String websiteUrl, String llmBaseUrl, String llmApiKey, String llmModel,
                         Consumer<String> progressCallback, Consumer<String> successCallback,
                         Consumer<String> errorCallback) {
        try {
            StepContext ctx = new StepContext();
            ctx.explorer = new WebsiteExplorer();
            ctx.llmClient = new LLMClient(llmBaseUrl, llmApiKey, llmModel);
            ctx.conversation = ctx.llmClient.createConversation(buildSystemPrompt());
            ctx.baseUrl = ctx.explorer.normalizeBaseUrl(websiteUrl);

            // Step 1: 首页
            if (!step1_home(ctx, progressCallback, errorCallback)) return;

            // Step 2: 搜索
            step2_search(ctx, progressCallback);

            // Step 3: 书籍详情页
            step3_book(ctx, progressCallback);

            // Step 4: 章节内容页
            step4_chapter(ctx, progressCallback);

            // Step 5: 组装规则
            step5_assembleRule(ctx, progressCallback, successCallback, errorCallback);

        } catch (Exception e) {
            LOG.error("Site rule generation failed", e);
            errorCallback.accept("生成失败: " + e.getMessage());
        }
    }

    // ==================== Step 1: 首页分析 ====================

    private boolean step1_home(StepContext ctx, Consumer<String> progress, Consumer<String> error) {
        progress.accept("正在获取并分析首页...");
        System.out.println("[Step 1] 正在获取首页: " + ctx.baseUrl);

        FetchedPage homePage = ctx.explorer.fetchAndClean(ctx.baseUrl);
        System.out.println("[Step 1] 首页响应: HTTP " + homePage.statusCode + " | isJson=" + homePage.isJson
                + " | rawBody=" + len(homePage.rawBody) + "字节 | cleanedHtml=" + len(homePage.cleanedHtml) + "字节");
        if (homePage.errorMsg != null) System.out.println("[Step 1] 首页错误: " + homePage.errorMsg);

        if (homePage.statusCode >= 400 || homePage.cleanedHtml == null) {
            error.accept("无法访问首页: " + or(homePage.errorMsg, "HTTP " + homePage.statusCode));
            return false;
        }

        // 搜索发现
        Document homeDoc = safeParse(homePage.rawBody, ctx.baseUrl);
        ctx.searchInfo = ctx.explorer.discoverSearch(homeDoc, ctx.baseUrl);
        System.out.println("[Step 1] 搜索发现: 方式=" + ctx.searchInfo.detectedFrom
                + " | URL=" + ctx.searchInfo.urlTemplate + " | method=" + ctx.searchInfo.method + " | param=" + ctx.searchInfo.paramName);

        // LLM 分析首页
        progress.accept("AI 正在分析首页结构...");
        System.out.println("[Step 1] 正在调用LLM分析首页HTML (" + len(homePage.cleanedHtml) + "字节)...");
        String analysis = llmSend(ctx,
                "分析这个小说网站首页的HTML，找出页面中书籍列表/推荐书籍的容器结构。\n\n"
                        + "请用JSON回答（不要markdown代码块）：\n"
                        + "{\"siteName\":\"网站名称\",\"bookListSelector\":\"书籍列表容器CSS选择器\",\"bookLinkSelector\":\"链接选择器(相对)\",\"bookTitleSelector\":\"标题选择器(相对)\"}\n\n"
                        + "如果首页没有书籍列表，bookListSelector填null。\n\nHTML:\n" + homePage.cleanedHtml);

        JsonNode json = parseJson(analysis);
        ctx.siteName = getJsonString(json, "siteName", "");
        System.out.println("[Step 1] 解析结果: siteName=" + ctx.siteName
                + " | bookListSelector=" + getJsonString(json, "bookListSelector", "null"));
        return true;
    }

    // ==================== Step 2: 搜索结果 ====================

    private void step2_search(StepContext ctx, Consumer<String> progress) {
        progress.accept("正在执行搜索并分析结果...");
        String searchUrl = ctx.searchInfo != null ? ctx.searchInfo.urlTemplate.replace("${key}", "完美") : null;
        System.out.println("[Step 2] 正在执行搜索: " + searchUrl);

        FetchedPage searchPage = null;
        if (ctx.searchInfo != null && ctx.searchInfo.urlTemplate != null) {
            searchPage = ctx.explorer.executeSearch(ctx.searchInfo, "完美");
        }
        logPage("[Step 2]", "搜索", searchPage);

        if (searchPage == null || searchPage.statusCode != 200) {
            System.out.println("[Step 2] 搜索失败或无结果，跳过");
            return;
        }

        String content = searchPage.isJson ? searchPage.rawBody : searchPage.cleanedHtml;
        if (content == null || content.length() <= 50) {
            System.out.println("[Step 2] 搜索结果内容过短，跳过");
            return;
        }

        progress.accept("AI 正在分析搜索结果页面...");
        ctx.searchAnalysis = llmSend(ctx,
                "分析这个小说搜索结果页面，找出搜索结果列表的结构。\n\n"
                        + "搜索URL: " + searchPage.url + "\n响应类型: " + (searchPage.isJson ? "JSON" : "HTML") + "\n\n"
                        + "请用JSON回答（不要markdown代码块）：\n"
                        + "{\"resultListSelector\":\"结果列表CSS选择器\",\"linkSelector\":\"链接选择器(相对)\",\"titleSelector\":\"标题选择器(相对)\",\"firstBookUrl\":\"第一个结果URL\",\"jsonPath\":\"JSON响应的JsonPath\"}\n\n"
                        + "响应内容:\n" + content);

        JsonNode json = parseJson(ctx.searchAnalysis);
        ctx.firstBookUrl = getJsonString(json, "firstBookUrl", null);
        System.out.println("[Step 2] 解析结果: resultListSelector=" + getJsonString(json, "resultListSelector", "null")
                + " | firstBookUrl=" + ctx.firstBookUrl);
    }

    // ==================== Step 3: 书籍详情页 ====================

    private void step3_book(StepContext ctx, Consumer<String> progress) {
        progress.accept("正在获取并分析书籍详情页...");

        // 确定书籍URL
        String bookUrl = null;
        if (ctx.firstBookUrl != null && !ctx.firstBookUrl.equals("null") && !ctx.firstBookUrl.isEmpty()) {
            bookUrl = ctx.explorer.resolveUrl(ctx.baseUrl, ctx.firstBookUrl);
            System.out.println("[Step 3] 书籍URL(来自搜索): " + bookUrl);
        }
        if (bookUrl == null) {
            System.out.println("[Step 3] 未找到书籍链接，跳过");
            return;
        }

        System.out.println("[Step 3] 正在获取书籍详情页: " + bookUrl);
        FetchedPage bookPage = ctx.explorer.fetchAndClean(bookUrl);
        logPage("[Step 3]", "书籍页", bookPage);

        if (bookPage == null || bookPage.statusCode != 200 || bookPage.cleanedHtml == null) {
            System.out.println("[Step 3] 书籍页获取失败，跳过");
            return;
        }

        // 检测是否有"查看全部章节"链接，获取完整目录页
        String fullChapterListHtml = null;
        String fullChapterListUrl = findFullChapterListLink(bookPage.cleanedHtml, bookUrl);
        if (fullChapterListUrl != null) {
            System.out.println("[Step 3] 发现完整目录页链接: " + fullChapterListUrl);
            FetchedPage fullListPage = ctx.explorer.fetchAndClean(fullChapterListUrl);
            if (fullListPage != null && fullListPage.statusCode == 200 && fullListPage.cleanedHtml != null) {
                fullChapterListHtml = fullListPage.cleanedHtml;
                System.out.println("[Step 3] 完整目录页获取成功: " + fullChapterListHtml.length() + "字节");
            }
        }

        progress.accept("AI 正在分析书籍详情页...");

        // 构造 prompt：如果找到了完整目录页，一起发给 LLM
        String prompt;
        if (fullChapterListHtml != null) {
            prompt = "分析这个小说书籍详情页面和完整章节目录页，找出书籍信息和章节结构。\n\n"
                    + "注意：书籍详情页可能只显示部分章节，完整目录在另一个页面中。\n\n"
                    + "请用JSON回答（不要markdown代码块）：\n"
                    + "{\n"
                    + "  \"chapterListSelector\": \"章节目录CSS选择器（优先从完整目录页中提取）\",\n"
                    + "  \"chapterLinkSelector\": \"章节链接选择器(相对)\",\n"
                    + "  \"bookTitleSelector\": \"书名选择器\",\n"
                    + "  \"authorSelector\": \"作者选择器\",\n"
                    + "  \"firstChapterUrl\": \"第一个章节URL\",\n"
                    + "  \"totalChapters\": \"估计章节数\",\n"
                    + "  \"fullChapterListUrl\": \"" + fullChapterListUrl + "\",\n"
                    + "  \"fullChapterListSelector\": \"完整目录页中章节列表的CSS选择器\"\n"
                    + "}\n\n"
                    + "## 书籍详情页HTML:\n" + bookPage.cleanedHtml
                    + "\n\n## 完整章节目录页HTML:\n" + fullChapterListHtml;
        } else {
            prompt = "分析这个小说书籍详情页面，找出章节目录和书籍信息的结构。\n\n"
                    + "请用JSON回答（不要markdown代码块）：\n"
                    + "{\"chapterListSelector\":\"章节目录CSS选择器\",\"chapterLinkSelector\":\"章节链接选择器(相对)\",\"bookTitleSelector\":\"书名选择器\",\"authorSelector\":\"作者选择器\",\"firstChapterUrl\":\"第一个章节URL\",\"totalChapters\":\"估计章节数\"}\n\n"
                    + "HTML:\n" + bookPage.cleanedHtml;
        }

        ctx.bookAnalysis = llmSend(ctx, prompt);
        System.out.println("[Step 3] LLM书籍页分析结果:\n" + ctx.bookAnalysis);
    }

    /**
     * 从书籍详情页中查找"查看全部章节"/"完整目录"类型的链接
     */
    private String findFullChapterListLink(String html, String bookUrl) {
        if (html == null) return null;
        Document doc = Jsoup.parse(html, bookUrl);

        // 匹配包含"查看所有章节"/"全部章节"/"完整目录"/"更多章节"等文字的链接
        String[] keywords = {"查看所有章节", "全部章节", "完整目录", "更多章节", "章节目录", "全部目录"};
        for (Element a : doc.select("a[href]")) {
            String text = a.text();
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    String href = a.attr("href");
                    if (!href.isEmpty() && !href.equals("#")) {
                        return resolveUrl(bookUrl, href);
                    }
                }
            }
        }

        // 兜底：查找 href 中包含 chapters/catalog 且文字较短的链接
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            String text = a.text();
            if ((href.contains("chapter") || href.contains("catalog") || href.contains("mulu"))
                    && text.length() < 20 && !text.isEmpty()) {
                return resolveUrl(bookUrl, href);
            }
        }

        return null;
    }

    // ==================== Step 4: 章节内容页 ====================

    private void step4_chapter(StepContext ctx, Consumer<String> progress) {
        progress.accept("正在获取并分析章节内容页...");

        if (ctx.bookAnalysis == null) {
            System.out.println("[Step 4] 无书籍页分析结果，跳过");
            return;
        }

        JsonNode bookJson = parseJson(ctx.bookAnalysis);
        String firstChapter = getJsonString(bookJson, "firstChapterUrl", null);
        if (firstChapter == null || firstChapter.equals("null") || firstChapter.isEmpty()) {
            System.out.println("[Step 4] LLM未返回firstChapterUrl，跳过");
            return;
        }

        String chapterUrl = ctx.explorer.resolveUrl(ctx.baseUrl, firstChapter);
        System.out.println("[Step 4] 章节URL: " + chapterUrl);

        System.out.println("[Step 4] 正在获取章节内容页: " + chapterUrl);
        FetchedPage chapterPage = ctx.explorer.fetchAndClean(chapterUrl);
        logPage("[Step 4]", "章节页", chapterPage);

        if (chapterPage == null || chapterPage.statusCode != 200 || chapterPage.cleanedHtml == null) {
            System.out.println("[Step 4] 章节页获取失败，跳过");
            return;
        }

        // 检测是否有 JS 动态加载内容的脚本，并尝试获取
        String tokenJsContent = fetchTokenScripts(chapterPage.cleanedHtml, chapterUrl);
        String extraContext = "";
        if (tokenJsContent != null) {
            extraContext = "\n\n## 该页面的内容加载脚本（token/API信息）:\n" + tokenJsContent;
            System.out.println("[Step 4] 获取到token脚本 (" + tokenJsContent.length() + "字节)");
        }

        progress.accept("AI 正在分析章节内容页...");
        ctx.chapterAnalysis = llmSend(ctx,
                "分析这个小说章节内容页面，判断内容是如何加载的，并找出正文内容的结构。\n\n"
                        + "## 分析要点\n"
                        + "1. 如果页面中有 `<div class=\"content\">` 等容器但内容为空，说明内容是通过JS动态加载的\n"
                        + "2. 如果页面中有 `document.writeln` 或 `eval` 等调用，说明内容是编码后通过JS解码写入的\n"
                        + "3. 如果页面中有 `chapter.js.php` 或类似的内容加载脚本，说明需要先请求该脚本获取token，再通过API获取内容\n"
                        + "4. 如果页面中有 `fetch()` 或 `XMLHttpRequest` 调用，说明是通过API获取内容\n\n"
                        + "## 请用JSON回答（不要markdown代码块）\n"
                        + "{\n"
                        + "  \"contentLoadMethod\": \"html/js-decode/js-api/js-fetch/unknown\",\n"
                        + "  \"contentSelector\": \"正文CSS选择器（如果内容在HTML中）\",\n"
                        + "  \"contentDescription\": \"如果内容不在HTML中，描述内容是如何加载的（如：需要先请求chapter.js.php获取token，然后通过fetch调用API获取内容）\",\n"
                        + "  \"apiUrl\": \"如果能找到API端点，填写完整URL（含参数模式）\",\n"
                        + "  \"apiMethod\": \"GET或POST\",\n"
                        + "  \"apiParams\": \"API参数说明\",\n"
                        + "  \"encodedContent\": \"如果页面中有编码内容（如base64），提取出来\",\n"
                        + "  \"titleSelector\": \"标题选择器\",\n"
                        + "  \"hasPagination\": false,\n"
                        + "  \"nextPageUrl\": \"下一页URL\"\n"
                        + "}\n\n"
                        + "## 章节页面HTML\n" + chapterPage.cleanedHtml
                        + extraContext);
        System.out.println("[Step 4] LLM章节页分析结果:\n" + ctx.chapterAnalysis);
    }

    // ==================== Step 5: 组装最终规则 ====================

    private void step5_assembleRule(StepContext ctx, Consumer<String> progress,
                                     Consumer<String> success, Consumer<String> error) {
        progress.accept("正在生成最终书源规则...");
        System.out.println("[Step 5] 汇总: siteName=" + ctx.siteName
                + " | search=" + (ctx.searchAnalysis != null ? "有" : "无")
                + " | book=" + (ctx.bookAnalysis != null ? "有" : "无")
                + " | chapter=" + (ctx.chapterAnalysis != null ? "有" : "无"));

        String finalPrompt = buildFinalRulePrompt(ctx);
        System.out.println("[Step 5] 正在调用LLM生成最终规则 (prompt " + finalPrompt.length() + "字节, 对话轮数=" + ctx.conversation.getTurnCount() + ")...");
        String ruleResponse = llmSend(ctx, finalPrompt);
        System.out.println("[Step 5] LLM响应 (" + len(ruleResponse) + "字节): "
                + (ruleResponse != null ? ruleResponse.substring(0, Math.min(500, ruleResponse.length())) : "null"));

        // 提取 JSON
        progress.accept("正在解析并校验规则...");
        String jsonStr = extractJsonArray(ruleResponse);
        System.out.println("[Step 5] 提取到JSON: " + (jsonStr != null ? jsonStr.length() + "字节" : "null"));
        if (jsonStr == null) {
            error.accept("AI未能生成有效的JSON格式，请重试");
            return;
        }

        // 校验
        String finalJson = jsonStr;
        boolean[] validated = {false};
        String[] validationError = {null};
        try {
            CustomSiteUtil.getInstance(project).parseCustomSiteRule(jsonStr,
                    r -> validated[0] = true, e -> validationError[0] = "校验失败");
        } catch (Exception e) {
            validationError[0] = e.getMessage();
        }
        System.out.println("[Step 5] 校验结果: validated=" + validated[0] + " | error=" + validationError[0]);

        // 自纠正
        if (!validated[0] && validationError[0] != null) {
            progress.accept("校验失败，正在自动修正...");
            try {
                String fixPrompt = "JSON校验错误:\n" + validationError[0] + "\n\n请修正以下JSON，只输出修正后的JSON数组:\n" + jsonStr;
                String fixedResponse = llmSend(ctx, fixPrompt);
                String fixedJson = extractJsonArray(fixedResponse);
                if (fixedJson != null) {
                    finalJson = fixedJson;
                    try {
                        CustomSiteUtil.getInstance(project).parseCustomSiteRule(fixedJson, r -> validated[0] = true, e -> {});
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                LOG.warn("Self-correction failed", e);
            }
        }

        progress.accept("生成完成" + (validated[0] ? "（已通过校验）" : "（校验未通过，请手动检查）"));
        success.accept(finalJson);
    }

    // ==================== Prompt ====================

    private String buildSystemPrompt() {
        String ruleDoc = loadResource("md/custom-rule-info.md");
        return "你是一个专业的书源规则生成专家。根据提供的各页面分析结果，生成W-Reader插件的SiteBean JSON规则。\n\n"
                + "## 规则文档\n\n" + ruleDoc + "\n\n"
                + "## 输出要求\n\n"
                + "1. 输出一个包含且仅包含一个SiteBean对象的JSON数组\n"
                + "2. CSS选择器必须是有效的Jsoup语法\n"
                + "3. hasHtml: HTML页面填true，JSON API填false\n"
                + "4. id填域名，name填网站名，baseUrl含协议\n"
                + "5. 空字段填\"\"，不要null\n"
                + "6. bookListUrlElement和bookListTitleElement相对于每个列表项\n"
                + "7. 只输出JSON数组，无其他文字\n";
    }

    private String buildFinalRulePrompt(StepContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据以下各步骤的页面分析结果，生成SiteBean书源规则。\n\n");

        sb.append("## 基本信息\n");
        sb.append("- baseUrl: ").append(ctx.baseUrl).append("\n");
        sb.append("- 网站名称: ").append(or(ctx.siteName, "未知")).append("\n\n");

        sb.append("## 搜索规则\n");
        if (ctx.searchInfo != null) {
            sb.append("- 搜索URL模板: ").append(ctx.searchInfo.urlTemplate).append("\n");
            sb.append("- 搜索方法: ").append(ctx.searchInfo.method).append("\n");
            sb.append("- 参数名: ").append(ctx.searchInfo.paramName).append("\n");
            sb.append("- 发现方式: ").append(ctx.searchInfo.detectedFrom).append("\n");
        }
        if (ctx.searchAnalysis != null) sb.append("- AI分析:\n").append(ctx.searchAnalysis).append("\n");
        sb.append("\n");

        sb.append("## 书籍详情页分析\n").append(or(ctx.bookAnalysis, "未能获取")).append("\n\n");
        sb.append("## 章节内容页分析\n").append(or(ctx.chapterAnalysis, "未能获取")).append("\n\n");

        sb.append("## 示例规则（参考格式）\n").append(loadResource("json/default-site-rule.json"));
        sb.append("\n\n请根据以上分析结果生成SiteBean JSON规则。");
        return sb.toString();
    }

    // ==================== LLM ====================

    /**
     * 通过共享对话发送消息，提升缓存命中率
     */
    private String llmSend(StepContext ctx, String prompt) {
        System.out.println("[LLM] 对话第" + (ctx.conversation.getTurnCount() + 1) + "轮 (prompt " + prompt.length() + "字节)...");
        try {
            String response = ctx.conversation.send(prompt);
            System.out.println("[LLM] 响应 (" + len(response) + "字节): "
                    + (response != null ? response.substring(0, Math.min(300, response.length())) : "null"));
            return response;
        } catch (LLMClient.LLMException e) {
            System.out.println("[LLM] 调用失败: " + e.getUserFriendlyMessage());
            LOG.warn("LLM call failed", e);
            return null;
        }
    }

    // ==================== 工具方法 ====================

    private void logPage(String tag, String name, FetchedPage page) {
        if (page == null) {
            System.out.println(tag + " " + name + " 未获取");
            return;
        }
        System.out.println(tag + " " + name + "响应: HTTP " + page.statusCode
                + " | isJson=" + page.isJson + " | url=" + page.url
                + " | body=" + len(page.rawBody) + "字节 | cleaned=" + len(page.cleanedHtml) + "字节");
        if (page.errorMsg != null) System.out.println(tag + " " + name + "错误: " + page.errorMsg);
    }

    /**
     * 从章节页面中检测并获取内容加载相关的 JS 脚本（如 chapter.js.php 等 token 脚本）
     */
    private String fetchTokenScripts(String html, String pageUrl) {
        if (html == null) return null;
        StringBuilder result = new StringBuilder();

        // 匹配可能的内容加载脚本：chapter.js, content.js, read.js 等
        Pattern scriptPattern = Pattern.compile("<script[^>]*src=['\"]([^'\"]*(?:chapter|content|read|token|yuedu)[^'\"]*\\.js[^'\"]*?)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = scriptPattern.matcher(html);

        while (matcher.find()) {
            String scriptSrc = matcher.group(1);
            String scriptUrl = resolveUrl(pageUrl, scriptSrc);
            if (scriptUrl == null) continue;

            System.out.println("[Step 4] 发现内容加载脚本: " + scriptUrl);
            try {
                org.apache.http.client.methods.HttpGet request = new org.apache.http.client.methods.HttpGet(scriptUrl);
                request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                request.setHeader("Referer", pageUrl);
                request.setHeader("Accept-Encoding", "identity");
                try (org.apache.http.impl.client.CloseableHttpClient client = org.apache.http.impl.client.HttpClients.createDefault();
                     org.apache.http.client.methods.CloseableHttpResponse response = client.execute(request)) {
                    if (response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null) {
                        String content = org.apache.http.util.EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
                        // 限制长度
                        if (content.length() > 3000) content = content.substring(0, 3000) + "... (truncated)";
                        result.append("### 脚本: ").append(scriptUrl).append("\n```\n").append(content).append("\n```\n\n");
                    }
                }
            } catch (Exception e) {
                System.out.println("[Step 4] 获取脚本失败: " + scriptUrl + " - " + e.getMessage());
            }
        }

        // 也检测内联 script 中的 document.writeln / eval 模式
        Pattern inlinePattern = Pattern.compile("<script[^>]*>([\\s\\S]*?(?:document\\.writeln|eval|qsbs|base64)[\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
        Matcher inlineMatcher = inlinePattern.matcher(html);
        while (inlineMatcher.find()) {
            String scriptContent = inlineMatcher.group(1).trim();
            if (scriptContent.length() > 10 && scriptContent.length() < 2000) {
                result.append("### 内联脚本 (编码内容):\n```\n").append(scriptContent).append("\n```\n\n");
            }
        }

        return result.length() > 0 ? result.toString() : null;
    }

    private Document safeParse(String html, String baseUrl) {
        try { return Jsoup.parse(html != null ? html : "", baseUrl); }
        catch (Exception e) { return Document.createShell(baseUrl); }
    }

    private JsonNode parseJson(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.startsWith("```")) text = text.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        try { return objectMapper.readTree(text); }
        catch (Exception e) { LOG.warn("JSON parse failed", e); return null; }
    }

    private String getJsonString(JsonNode node, String field, String def) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return def;
        String val = node.get(field).asText();
        return (val.isEmpty() || val.equals("null")) ? def : val;
    }

    private String extractJsonArray(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```").matcher(text);
        if (m.find()) { String inner = m.group(1).trim(); if (inner.startsWith("[")) return inner; }
        Matcher m2 = Pattern.compile("\\[[\\s\\S]*\\]").matcher(text);
        if (m2.find()) return m2.group();
        text = text.trim();
        return text.startsWith("[") ? text : null;
    }

    private String loadResource(String path) {
        try (InputStream is = SiteRuleGenerator.class.getClassLoader().getResourceAsStream(path)) {
            return is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { return ""; }
    }

    private int len(String s) { return s != null ? s.length() : 0; }
    private String or(String s, String def) { return s != null && !s.isEmpty() ? s : def; }

    private String resolveUrl(String baseUrl, String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) return null;
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) return relativeUrl;
        if (relativeUrl.startsWith("//")) return "https:" + relativeUrl;
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL resolved = new java.net.URL(base, relativeUrl);
            return resolved.toString();
        } catch (Exception e) {
            return relativeUrl.startsWith("/") ? baseUrl + relativeUrl : baseUrl + "/" + relativeUrl;
        }
    }
}
