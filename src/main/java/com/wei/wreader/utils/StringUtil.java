package com.wei.wreader.utils;

import com.jayway.jsonpath.JsonPath;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    /** 全角空格符号 */
    public static final String FULL_WIDTH_SPACE = "　";

    /**
     * 匹配 <body> 标签内容的正则表达式
     */
    public static final String PATTERN = "<body[^>]*>(.*?)</body>";

    /**
     * 去除字符按两端的空格（全角和半角）
     * @param str
     * @return
     */
    public static String trim(String str) {
        str = str.trim();
        // 这里判断是不是全角空格
        while (str.startsWith(FULL_WIDTH_SPACE)) {
            str = str.substring(1).trim();
        }
        while (str.endsWith(FULL_WIDTH_SPACE)) {
            str = str.substring(0, str.length() - 1).trim();
        }

        return str;
    }

    /**
     * 将字符串按照指定的最大字符数分割成多个子字符串
     *
     * @param str      原始字符串
     * @param maxChars 单行最大字符数
     * @return 分割后的字符串数组
     */
    public static List<String> splitStringByMaxCharList(String str, int maxChars) {
        return List.of(splitStringByMaxChars(str, maxChars));
    }

    /**
     * 将字符串按照指定的最大字符数分割成多个子字符串
     *
     * @param str      原始字符串
     * @param maxChars 单行最大字符数base64Decode
     * @return 分割后的字符串数组
     */
    public static String[] splitStringByMaxChars(String str, int maxChars) {
        if (str == null || str.isEmpty() || maxChars <= 0) {
            return new String[]{};
        }

        int length = str.length();
        int parts = length / maxChars; // 计算大致的分割数量
        if (length % maxChars != 0) {
            parts++; // 如果有余数，则分割数量加1
        }

        String[] result = new String[parts];

        for (int i = 0, start = 0; i < parts; i++) {
            int end = Math.min(start + maxChars, length);
            result[i] = str.substring(start, end);
            start = end;
        }

        return result;
    }


    public static List<String> splitCompleteString(String input, int maxLength) {
        List<String> result = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return result;
        }
        String[] words = input.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.isEmpty()) {
                // 如果当前行是空的，直接添加单词
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxLength) {
                // 如果添加当前单词和一个空格后不超过最大长度，添加单词
                currentLine.append(" ").append(word);
            } else {
                // 超过最大长度，将当前行添加到结果列表，并开始新的一行
                result.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }
        // 添加最后一行
        if (!currentLine.isEmpty()) {
            result.add(currentLine.toString());
        }
        return result;
    }

    /**
     * BASE64解码
     * @param content 待解码的Base64字符串
     * @param isConvert 是否进行转换
     */
    public static String base64Decode(String content, Boolean isConvert) {
        try {
            if (isConvert) {
                content = convertString(content);
            }
            return new String(Base64.getDecoder().decode(content), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String base64Encode(String content, Boolean isConvert) {
        if (isConvert) {
            content = convertString(content);
        }
        return Base64.getEncoder().encodeToString(content.getBytes());
    }

    /**
     * 将输入字符串中的字母进行特定的转换
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    public static String convertString(String input) {
        StringBuilder result = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                int charCode = (int) (double) ((int) c / 97);
                int i = (Character.toLowerCase(c) - 83) % 26;
                int kValue = i != 0 ? i : 26;

                char convertedChar = (char) (kValue + (charCode == 0 ? 64 : 96));
                result.append(convertedChar);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static Object jsonPathRead(String jsonString, String path) {
        return JsonPath.read(jsonString, path);
    }

    /**
     * 提取HTML中的body标签内容
     * @param html
     * @return
     */
    public static String extractBodyContent(String html) {
        Pattern r = Pattern.compile(PATTERN, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = r.matcher(html);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * 替换HTML中的img标签的src属性为base64编码
     * @param html
     * @param imageMap
     * @return
     */
    public static String replaceImgSrcWithBase64(String html,
                                                 Map<String, String> imageMap) {
        // 解析 HTML 字符串
        Document doc = Jsoup.parse(html);
        // 获取所有 <img> 标签
        Elements imgElements = doc.select("img");

        // 遍历每个 <img> 标签
        for (Element img : imgElements) {
            String src = img.attr("src");
            // 检查 src 属性是否存在于 Map 中
            if (imageMap.containsKey(src)) {
                String base64Image = imageMap.get(src);
                // 替换 <img> 标签的 src 属性
                img.attr("src", base64Image); // 假设图片类型为 JPEG，可根据实际情况修改
            }
        }

        // 返回修改后的 HTML 字符串
        return doc.html();
    }

    /**
     * 替换HTML中的img标签的src属性为base64编码
     * @param html
     * @param imageMap
     * @return
     */
    public static String replaceImgSrcWithBase64regex(String html, Map<String, String> imageMap) {
        // 定义匹配 <img> 标签及其 src 属性的正则表达式
        String regex = "<img\\s+[^>]*src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        StringBuilder result = new StringBuilder();
        int lastIndex = 0;

        // 遍历所有匹配的 <img> 标签
        while (matcher.find()) {
            // 将匹配到的 <img> 标签之前的部分添加到结果中
            result.append(html, lastIndex, matcher.start());
            // 获取 <img> 标签的 src 属性值
            String src = matcher.group(1);
            if (imageMap.containsKey(src)) {
                String base64Image = imageMap.get(src);
                // 构建替换后的 <img> 标签
                String newImgTag = matcher.group().replace(src, base64Image);
                result.append(newImgTag);
            } else {
                // 如果 src 属性不在 Map 中，保留原 <img> 标签
                result.append(matcher.group());
            }
            lastIndex = matcher.end();
        }
        // 添加最后一个匹配的 <img> 标签之后的部分
        result.append(html.substring(lastIndex));

        return result.toString();
    }

    /**
     * 替换 HTML 或 SVG 中的图片链接
     * @param html
     * @param imageMap
     * @return
     */
    public static String replaceImageLinks(String html, Map<String, String> imageMap) {
        // 匹配 <img> 标签的正则表达式
        String imgRegex = "<img\\s+[^>]*src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
        html = replaceWithImg(html, imgRegex, imageMap);

        // 匹配 SVG 中 <image> 标签的正则表达式
        String svgImageRegex = "<image\\s+[^>]*xlink:href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
        html = replaceWithImg(html, svgImageRegex, imageMap);// 去除可能存在的 ../ 前缀
        // 将 SVG 中的 <image> 标签转换为 <img> 标签
        html = convertSvgImageToImg(html);

        String style = "width=\"" + ConstUtil.IMAGE_THUMBNAIL_WIDTH + "\"";
        html = html.replaceAll("<img", "<img " + style);

        return html;
    }

    /**
     * 替换 HTML 或 SVG 中的图片链接
     * @param html
     * @param regex
     * @param imageMap
     * @return
     */
    public static String replaceWithImg(String html, String regex, Map<String, String> imageMap) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            result.append(html, lastIndex, matcher.start());
            String src = matcher.group(1);
            // 判断 src 是否存在于 imageMap 中
            boolean isContain = false;
            for (Map.Entry<String, String> entry : imageMap.entrySet()) {
                String key = entry.getKey();
                String imgSrc = entry.getValue();
                if (key.contains(src) ||
                        ((src.startsWith("./") || src.startsWith("../")) && src.endsWith(key)) ||
                        ((key.startsWith("./") || key.startsWith("../")) && key.endsWith(src))) {
                    isContain = true;
                    String newTag = matcher.group().replaceFirst(Pattern.quote(src), Matcher.quoteReplacement(imgSrc));
                    newTag = newTag.replaceAll("width\\s*=\\s*['\"]([^'\"]+)['\"]", "width=\"" + ConstUtil.IMAGE_THUMBNAIL_WIDTH + "\"")
                            .replaceAll("height\\s*=\\s*['\"]([^'\"]+)['\"]", "");
                    result.append(newTag);
                }
            }

            if (!isContain) {
                result.append(matcher.group());
            }

            lastIndex = matcher.end();
        }
        result.append(html.substring(lastIndex));
        return result.toString();
    }

    /**
     * 去除相对路径前缀
     * @param path
     * @return
     */
    private static String removeRelativePathPrefix(String path) {
        while (path.startsWith("../")) {
            path = path.substring(3);
        }
        return path;
    }

    /**
     * 将 SVG 图像转换为 img 标签
     * @param svgContent
     * @return
     */
    public static String convertSvgImageToImg(String svgContent) {
        // 定义匹配 SVG <image> 标签的正则表达式
        String regex = "<image\\s+([^>]*?)xlink:href\\s*=\\s*['\"]([^'\"]+)['\"]([^>]*?)>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(svgContent);

        StringBuilder result = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            // 将匹配到的 <image> 标签之前的内容添加到结果中
            result.append(svgContent, lastIndex, matcher.start());
            // 获取宽度、高度和图片链接
            String attrs = matcher.group(1);
            String src = matcher.group(2);
            String otherAttrs = matcher.group(3);

            // 提取宽度和高度属性
//            String width = extractAttribute(attrs, "width");
//            String height = extractAttribute(attrs, "height");

            // 构建 <img> 标签
            StringBuilder imgTag = new StringBuilder("<img src=\"").append(src).append("\"");
//            if (width != null) {
//                imgTag.append(" width=\"").append(width).append("\"");
//            }
//            if (height != null) {
//                imgTag.append(" height=\"").append(height).append("\"");
//            }
            // 添加其他属性
            imgTag.append(parseOtherAttributes(otherAttrs)).append(">");

            result.append(imgTag);
            lastIndex = matcher.end();
        }
        // 添加最后一个匹配的 <image> 标签之后的内容
        result.append(svgContent.substring(lastIndex));

        // 移除 SVG 标签
        return removeSvgTags(result.toString());
    }

    /**
     * 从属性字符串中提取属性值
     * @param attrs
     * @param attrName
     * @return
     */
    private static String extractAttribute(String attrs, String attrName) {
        String regex = attrName + "\\s*=\\s*['\"]([^'\"]+)['\"]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(attrs);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 表示匹配空白字符
     * @param otherAttrs
     * @return
     */
    private static String parseOtherAttributes(String otherAttrs) {
        // 过滤掉 xlink 相关属性
        String filtered = otherAttrs.replaceAll("xlink:[^=]+\\s*=\\s*['\"][^'\"]+['\"]", "");
        return filtered.trim().isEmpty() ? "" : " " + filtered.trim();
    }

    /**
     * 移除 SVG 标签
     * @param content
     * @return
     */
    private static String removeSvgTags(String content) {
        // 定义匹配 svg 标签的正则表达式
        String svgRegex = "<svg[^>]*>(.*)</svg>";
        Pattern svgPattern = Pattern.compile(svgRegex, Pattern.DOTALL);
        Matcher svgMatcher = svgPattern.matcher(content);
        if (svgMatcher.find()) {
            return svgMatcher.group(1);
        }
        return content;
    }

    public static void main(String[] args) {
        String jsonString = """
                    {"code":0,"message":"SUCCESS",
                    "data":[
                        {"book_id": "123456",
                         "book_data": [
                            {"book_name": "bk001"},
                            {"book_name": "bk002"}
                         ]},
                        {"book_id": "123457",
                         "book_data": [
                            {"book_name": "bk003"},
                            {"book_name": "bk004"}
                         ]}
                    ]}
                """;

        // 获取所有 data 项
        Object read = JsonPath.read(jsonString, "$.data[*].book_data[*]");
        Object read2 = JsonPath.read(read, "$.[1].book_name");
        System.out.println(read);
        System.out.println(read2);

        System.out.println(convertString("Az"));

        String svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\" width=\"200\" height=\"200\">" +
                "<image width=\"200\" height=\"200\" xlink:href=\"file:///C:/Users/win10/AppData/Local/Temp/WReader/images/images/00001.jpeg\"/>" +
                "</svg>";
        String htmlContent = convertSvgImageToImg(svgContent);
        System.out.println(htmlContent);
    }
}
