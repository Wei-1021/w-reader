package com.wei.wreader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.llm.LLMClient;
import com.wei.wreader.service.WebsiteExplorer.*;
import com.wei.wreader.util.CustomSiteUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
        String analysis = llmAnalyze(ctx.llmClient,
                "分析这个小说网站首页的HTML，找出页面中书籍列表/推荐书籍的容器结构。\n\n"
                        + "请用JSON回答（不要markdown代码块）：\n"
                        + "{\"siteName\":\"网站名称\",\"bookListSelector\":\"书籍列表容器CSS选择器\",\"bookLinkSelector\":\"链接选择器(相对)\",\"bookTitleSelector\":\"标题选择器(相对)\"}\n\n"
                        + "如果首页没有书籍列表，bookListSelector填null。\n\nHTML:\n" + homePage.cleanedHtml,
                "你是一个HTML分析专家。只输出JSON，不要其他文字。");

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
        ctx.searchAnalysis = llmAnalyze(ctx.llmClient,
                "分析这个小说搜索结果页面，找出搜索结果列表的结构。\n\n"
                        + "搜索URL: " + searchPage.url + "\n响应类型: " + (searchPage.isJson ? "JSON" : "HTML") + "\n\n"
                        + "请用JSON回答（不要markdown代码块）：\n"
                        + "{\"resultListSelector\":\"结果列表CSS选择器\",\"linkSelector\":\"链接选择器(相对)\",\"titleSelector\":\"标题选择器(相对)\",\"firstBookUrl\":\"第一个结果URL\",\"jsonPath\":\"JSON响应的JsonPath\"}\n\n"
                        + "响应内容:\n" + content,
                "你是一个HTML/JSON分析专家。只输出JSON，不要其他文字。");

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
            Document homeDoc = safeParse(null, ctx.baseUrl); // 需要重新获取，但这里用 firstBookUrl 兜底
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

        progress.accept("AI 正在分析书籍详情页...");
        ctx.bookAnalysis = llmAnalyze(ctx.llmClient,
                "分析这个小说书籍详情页面，找出章节目录和书籍信息的结构。\n\n"
                        + "请用JSON回答（不要markdown代码块）：\n"
                        + "{\"chapterListSelector\":\"章节目录CSS选择器\",\"chapterLinkSelector\":\"章节链接选择器(相对)\",\"bookTitleSelector\":\"书名选择器\",\"authorSelector\":\"作者选择器\",\"firstChapterUrl\":\"第一个章节URL\",\"totalChapters\":\"估计章节数\"}\n\n"
                        + "HTML:\n" + bookPage.cleanedHtml,
                "你是一个HTML分析专家。只输出JSON，不要其他文字。");
        System.out.println("[Step 3] LLM书籍页分析结果:\n" + ctx.bookAnalysis);
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

        // 需要 bookUrl 来 resolve，从 step3 的 bookAnalysis 中无法直接获取 bookUrl
        // 但 firstChapter 通常是相对路径，需要 baseUrl
        String chapterUrl = ctx.explorer.resolveUrl(ctx.baseUrl, firstChapter);
        System.out.println("[Step 4] 章节URL: " + chapterUrl);

        System.out.println("[Step 4] 正在获取章节内容页: " + chapterUrl);
        FetchedPage chapterPage = ctx.explorer.fetchAndClean(chapterUrl);
        logPage("[Step 4]", "章节页", chapterPage);

        if (chapterPage == null || chapterPage.statusCode != 200 || chapterPage.cleanedHtml == null) {
            System.out.println("[Step 4] 章节页获取失败，跳过");
            return;
        }

        progress.accept("AI 正在分析章节内容页...");
        ctx.chapterAnalysis = llmAnalyze(ctx.llmClient,
                "分析这个小说章节内容页面，找出正文内容和分页的结构。\n\n"
                        + "若页面返回内容不是正常文字，而是被编码过的字符（包括但不限于如<script>document.writeln(qsbs.bb('PHA+55S3PC9wPg=='));</script>等形式的内容）时，请将页面内容显示出来，\n"
                        + "请用JSON回答（不要markdown代码块）：\n"
                        + "{\"contentSelector\":\"正文CSS选择器\", contentCoding:\"被编码过的内容\",\"titleSelector\":\"标题选择器\",\"hasPagination\":true或false,\"nextPageSelector\":\"下一页选择器\",\"nextPageUrl\":\"下一页URL\"}\n\n"
                        + "HTML:\n" + chapterPage.cleanedHtml,
                "你是一个HTML分析专家。只输出JSON，不要其他文字。");
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
        System.out.println("[Step 5] 正在调用LLM生成最终规则 (prompt " + finalPrompt.length() + "字节)...");
        String ruleResponse = safeCompletion(ctx.llmClient, buildSystemPrompt(), finalPrompt);
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
                String fixedResponse = safeCompletion(ctx.llmClient, buildSystemPrompt(), fixPrompt);
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

    private String llmAnalyze(LLMClient client, String prompt, String systemPrompt) {
        System.out.println("[LLM] 调用分析接口 (prompt " + prompt.length() + "字节)...");
        String response = safeCompletion(client, systemPrompt, prompt);
        System.out.println("[LLM] 分析响应 (" + len(response) + "字节): "
                + (response != null ? response.substring(0, Math.min(300, response.length())) : "null"));
        return response;
    }

    private String safeCompletion(LLMClient client, String system, String user) {
        try {
            return client.chatCompletion(system, user);
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
}
