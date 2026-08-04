package com.wei.wreader.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.llm.LLMClient.ToolDefinition;
import com.wei.wreader.service.WebsiteExplorer;
import com.wei.wreader.service.WebsiteExplorer.FetchedPage;
import com.wei.wreader.service.WebsiteExplorer.SearchInfo;
import com.wei.wreader.util.CustomSiteUtil;
import com.jayway.jsonpath.JsonPath;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 书源规则生成 Agent 工具集
 *
 * @author weizhanjie
 */
public class SiteRuleTools {
    private static final Logger LOG = Logger.getInstance(SiteRuleTools.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_HTML_LENGTH = 80000;
    private static final int MAX_ELEMENT_TEXT_LENGTH = 25000;

    private final Project project;
    private final WebsiteExplorer explorer;

    /** 缓存搜索发现结果，避免重复发现 */
    private SearchInfo cachedSearchInfo;
    /** 缓存 baseUrl */
    private String baseUrl;

    public SiteRuleTools(Project project) {
        this.project = project;
        this.explorer = new WebsiteExplorer();
    }

    /**
     * 获取所有工具定义
     */
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(new ToolDefinition(
                "fetch_page",
                "获取指定URL的网页内容。返回清理后的HTML（移除了script/style等非内容标签），以及页面基本信息（状态码、是否JSON、标题等）。用于分析网站页面结构。",
                """
                {
                  "type": "object",
                  "properties": {
                    "url": {
                      "type": "string",
                      "description": "要获取的完整URL（必须包含http/https协议）"
                    }
                  },
                  "required": ["url"]
                }
                """
        ));

        tools.add(new ToolDefinition(
                "search_website",
                "在目标网站搜索书籍。自动发现搜索机制（表单/JS/API），执行搜索并返回结果页面。需要先调用fetch_page获取首页后再使用。返回搜索结果页面的HTML或JSON。",
                """
                {
                  "type": "object",
                  "properties": {
                    "keyword": {
                      "type": "string",
                      "description": "搜索关键词，建议使用常见书名如'完美世界'、'斗破苍穹'等"
                    }
                  },
                  "required": ["keyword"]
                }
                """
        ));

        tools.add(new ToolDefinition(
                "extract_html_elements",
                "从HTML中使用CSS选择器提取元素。返回匹配元素的标签名、属性、文本内容。用于验证CSS选择器是否正确匹配到预期内容。",
                """
                {
                  "type": "object",
                  "properties": {
                    "html": {
                      "type": "string",
                      "description": "HTML内容"
                    },
                    "css_selector": {
                      "type": "string",
                      "description": "Jsoup CSS选择器，如 '#content', '.chapter-list a', 'dl dd a' 等"
                    },
                    "max_results": {
                      "type": "integer",
                      "description": "最大返回结果数，默认20",
                      "default": 20
                    }
                  },
                  "required": ["html", "css_selector"]
                }
                """
        ));

        tools.add(new ToolDefinition(
                "parse_json_path",
                "从JSON响应中使用JsonPath表达式提取数据。用于分析API返回的JSON数据结构。",
                """
                {
                  "type": "object",
                  "properties": {
                    "json_text": {
                      "type": "string",
                      "description": "JSON文本内容"
                    },
                    "json_path": {
                      "type": "string",
                      "description": "JsonPath表达式，如 '$.data.list[*].title', '$.chapters[0].url' 等"
                    }
                  },
                  "required": ["json_text", "json_path"]
                }
                """
        ));

        tools.add(new ToolDefinition(
                "validate_site_rule",
                "校验生成的SiteBean JSON规则是否合法。返回校验结果和错误信息。请在生成最终规则前调用此工具验证。",
                """
                {
                  "type": "object",
                  "properties": {
                    "json_rule": {
                      "type": "string",
                      "description": "SiteBean JSON规则字符串（JSON数组格式）"
                    }
                  },
                  "required": ["json_rule"]
                }
                """
        ));

        tools.add(new ToolDefinition(
                "complete_rule",
                "提交最终生成的书源规则。调用此工具表示规则生成完成，传入校验通过的SiteBean JSON。这是生成流程的最后一步。",
                """
                {
                  "type": "object",
                  "properties": {
                    "json_rule": {
                      "type": "string",
                      "description": "最终的SiteBean JSON规则（JSON数组格式，必须已通过validate_site_rule校验）"
                    }
                  },
                  "required": ["json_rule"]
                }
                """
        ));

        return tools;
    }

    /**
     * 根据工具名执行对应的工具
     */
    public String executeTool(String toolName, JsonNode arguments) throws Exception {
        return switch (toolName) {
            case "fetch_page" -> executeFetchPage(arguments);
            case "search_website" -> executeSearchWebsite(arguments);
            case "extract_html_elements" -> executeExtractHtmlElements(arguments);
            case "parse_json_path" -> executeParseJsonPath(arguments);
            case "validate_site_rule" -> executeValidateSiteRule(arguments);
            case "complete_rule" -> executeCompleteRule(arguments);
            default -> "未知工具: " + toolName;
        };
    }

    // ==================== 工具实现 ====================

    private String executeFetchPage(JsonNode args) {
        String url = args.path("url").asText();
        if (url.isEmpty()) return "错误: url 参数不能为空";

        // 首次调用时记录 baseUrl
        if (baseUrl == null) {
            baseUrl = explorer.normalizeBaseUrl(url);
        }

        FetchedPage page = explorer.fetchAndClean(url);

        StringBuilder result = new StringBuilder();
        result.append("状态: HTTP ").append(page.statusCode).append("\n");
        result.append("类型: ").append(page.isJson ? "JSON" : "HTML").append("\n");
        result.append("URL: ").append(page.url).append("\n");

        if (page.errorMsg != null) {
            result.append("错误: ").append(page.errorMsg).append("\n");
        }

        if (page.rawBody != null) {
            result.append("原始大小: ").append(page.rawBody.length()).append(" 字节\n");
        }

        // 返回内容
        String content = page.isJson ? page.rawBody : page.cleanedHtml;
        if (content != null && !content.isEmpty()) {
            if (content.length() > MAX_HTML_LENGTH) {
                content = content.substring(0, MAX_HTML_LENGTH) + "\n... (已截断，共" + content.length() + "字节)";
            }
            result.append("\n--- 页面内容 ---\n");
            result.append(content);
        } else if (page.rawBody != null) {
            // cleanedHtml 可能为空但 rawBody 不为空
            String raw = page.rawBody;
            if (raw.length() > MAX_HTML_LENGTH) {
                raw = raw.substring(0, MAX_HTML_LENGTH) + "\n... (已截断)";
            }
            result.append("\n--- 原始内容 ---\n");
            result.append(raw);
        }

        return result.toString();
    }

    private String executeSearchWebsite(JsonNode args) {
        String keyword = args.path("keyword").asText();
        if (keyword.isEmpty()) return "错误: keyword 参数不能为空";
        if (baseUrl == null) return "错误: 请先调用 fetch_page 获取网站首页";

        // 发现搜索机制（缓存）
        if (cachedSearchInfo == null) {
            FetchedPage homePage = explorer.fetchAndClean(baseUrl);
            if (homePage.statusCode != 200 || homePage.rawBody == null) {
                return "错误: 无法获取首页 (HTTP " + homePage.statusCode + ")";
            }
            Document homeDoc = Jsoup.parse(homePage.rawBody, baseUrl);
            cachedSearchInfo = explorer.discoverSearch(homeDoc, baseUrl);
        }

        // 执行搜索
        FetchedPage searchPage = explorer.executeSearch(cachedSearchInfo, keyword);
        if (searchPage == null) {
            return "错误: 搜索执行失败";
        }

        StringBuilder result = new StringBuilder();
        result.append("搜索URL: ").append(searchPage.url).append("\n");
        result.append("状态: HTTP ").append(searchPage.statusCode).append("\n");
        result.append("类型: ").append(searchPage.isJson ? "JSON" : "HTML").append("\n");
        result.append("发现方式: ").append(cachedSearchInfo.detectedFrom).append("\n");
        result.append("参数名: ").append(cachedSearchInfo.paramName).append("\n");

        String content = searchPage.isJson ? searchPage.rawBody : searchPage.cleanedHtml;
        if (content != null && !content.isEmpty()) {
            if (content.length() > MAX_HTML_LENGTH) {
                content = content.substring(0, MAX_HTML_LENGTH) + "\n... (已截断)";
            }
            result.append("\n--- 搜索结果 ---\n");
            result.append(content);
        }

        return result.toString();
    }

    private String executeExtractHtmlElements(JsonNode args) {
        String html = args.path("html").asText();
        String cssSelector = args.path("css_selector").asText();
        int maxResults = args.path("max_results").isInt() ? args.path("max_results").asInt() : 20;

        if (html.isEmpty()) return "错误: html 参数不能为空";
        if (cssSelector.isEmpty()) return "错误: css_selector 参数不能为空";

        try {
            Document doc = Jsoup.parse(html, baseUrl != null ? baseUrl : "https://example.com");
            Elements elements = doc.select(cssSelector);

            if (elements.isEmpty()) {
                return "选择器 '" + cssSelector + "' 未匹配到任何元素。\n"
                        + "页面中可用的主要元素: " + summarizePageStructure(doc);
            }

            StringBuilder result = new StringBuilder();
            result.append("选择器: ").append(cssSelector).append("\n");
            result.append("匹配数量: ").append(elements.size()).append("\n\n");

            int count = 0;
            for (Element el : elements) {
                if (count >= maxResults) break;
                result.append("[").append(count + 1).append("] ");
                result.append("<").append(el.tagName());

                // 输出关键属性
                String id = el.id();
                String className = el.className();
                if (!id.isEmpty()) result.append(" id=\"").append(id).append("\"");
                if (!className.isEmpty()) result.append(" class=\"").append(className).append("\"");

                String href = el.attr("href");
                if (!href.isEmpty()) result.append(" href=\"").append(href).append("\"");

                result.append(">");
                String text = el.text();
                if (text.length() > 200) text = text.substring(0, 200) + "...";
                result.append(" ").append(text);
                result.append("\n");
                count++;
            }

            return result.toString();
        } catch (Exception e) {
            return "CSS选择器解析失败: " + e.getMessage();
        }
    }

    private String executeParseJsonPath(JsonNode args) {
        String jsonText = args.path("json_text").asText();
        String jsonPathExpr = args.path("json_path").asText();

        if (jsonText.isEmpty()) return "错误: json_text 参数不能为空";
        if (jsonPathExpr.isEmpty()) return "错误: json_path 参数不能为空";

        try {
            Object result = JsonPath.read(jsonText, jsonPathExpr);
            if (result == null) {
                return "JsonPath '" + jsonPathExpr + "' 返回 null";
            }
            String resultStr = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);
            if (resultStr.length() > MAX_ELEMENT_TEXT_LENGTH) {
                resultStr = resultStr.substring(0, MAX_ELEMENT_TEXT_LENGTH) + "\n... (已截断)";
            }
            return "JsonPath: " + jsonPathExpr + "\n结果:\n" + resultStr;
        } catch (Exception e) {
            return "JsonPath 解析失败: " + e.getMessage();
        }
    }

    private String executeValidateSiteRule(JsonNode args) {
        String jsonRule = args.path("json_rule").asText();
        if (jsonRule.isEmpty()) return "错误: json_rule 参数不能为空";

        try {
            boolean[] validated = {false};

            CustomSiteUtil.getInstance(project).parseCustomSiteRule(jsonRule,
                    r -> validated[0] = true,
                    null);

            return "校验通过 ✓ SiteBean JSON 格式正确。";
        } catch (IllegalArgumentException e) {
            return "校验失败 ✗\n" + e.getMessage();
        } catch (Exception e) {
            return "校验异常: " + e.getMessage();
        }
    }

    private String executeCompleteRule(JsonNode args) {
        String jsonRule = args.path("json_rule").asText();
        if (jsonRule.isEmpty()) return "错误: json_rule 参数不能为空";

        // 存储最终结果供外部获取
        this.finalRule = jsonRule;
        return "规则已提交。生成流程完成。";
    }

    /** 存储最终生成的规则 */
    private String finalRule;

    public String getFinalRule() {
        return finalRule;
    }

    // ==================== 辅助方法 ====================

    /**
     * 概述页面结构（供选择器未匹配时给出提示）
     */
    private String summarizePageStructure(Document doc) {
        StringBuilder sb = new StringBuilder();
        // 列出主要容器
        for (String tag : new String[]{"div", "ul", "ol", "table", "section", "article", "main"}) {
            Elements els = doc.select(tag + "[id], " + tag + "[class]");
            for (Element el : els) {
                if (sb.length() > 500) break;
                String id = el.id();
                String cls = el.className();
                String desc = "<" + tag;
                if (!id.isEmpty()) desc += " id=\"" + id + "\"";
                if (!cls.isEmpty()) desc += " class=\"" + cls + "\"";
                desc += "> (" + el.children().size() + "个子元素)";
                sb.append(desc).append("\n");
            }
            if (sb.length() > 500) break;
        }
        return sb.length() > 0 ? "\n" + sb.toString() : "无法概述";
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public WebsiteExplorer getExplorer() {
        return explorer;
    }
}
