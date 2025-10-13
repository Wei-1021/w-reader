package com.wei.wreader.utils.file;

import com.intellij.remoteServer.agent.annotation.AsyncCall;
import io.documentnode.epub4j.domain.*;
import io.documentnode.epub4j.domain.Book;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * epub文件读取
 * @author weizhanjie
 */
public class EpubReaderComplete {

    /**
     * 读取epub文件
     * @param book  book
     * @param fallback 回调函数，每成功读取到一个章节的内容都会执行一次此方法
     * @throws IOException
     */
    public synchronized static void readEpub(Book book, Consumer<Map<String, String>> fallback) throws IOException {
        // 1. 获取 Spine (核心阅读顺序)
        List<SpineReference> spineReferences = book.getSpine().getSpineReferences();
        // 2. 获取 TOC (用于查找标题和层级)
        TableOfContents tableOfContents = book.getTableOfContents();
        List<TOCReference> tocReferences = tableOfContents.getTocReferences();

        // 3. 构建一个从 Resource ID 到 TOCReference 的映射，用于快速查找
        Map<String, TOCReference> resourceToTocMap = new HashMap<>();
        buildTocResourceMap(tocReferences, resourceToTocMap);

        for (SpineReference spineRef : spineReferences) {
            Resource resource = spineRef.getResource();

            if (resource == null) {
                continue;
            }

            String resourceId = resource.getId();
            String resourceHref = resource.getHref(); // 文件路径

            // 4. 尝试从 TOC 中查找对应的标题和层级
            TOCReference tocRef = resourceToTocMap.get(resourceId);
            String title = "Untitled";
            int tocLevel = -1; // -1 表示不在 TOC 中

            if (tocRef != null) {
                title = tocRef.getTitle();
                if (title == null || title.trim().isEmpty()) {
                    title = "Untitled (from TOC)";
                }
                // 计算层级 (需要遍历树找到深度)
                tocLevel = findTocLevel(tocReferences, resourceId, 0);
            } else {
                // 如果不在 TOC 中，可以使用文件名作为标题
                title = "No TOC Entry - " + resourceHref;
            }

            // 每级缩进四个空格
            String indent = "    ".repeat(Math.max(0, tocLevel));

            // 5. (可选) 获取章节内容
            byte[] data = resource.getData();
            // 获取输入编码
            String inputEncoding = resource.getInputEncoding();
            String content = data != null ? new String(data, inputEncoding) : "";
            // processContent(content, title, tocLevel);

            String finalTitle = indent + title;
            fallback.accept(new HashMap<>() {{
                put("title", finalTitle);
                put("content", content);
            }});
        }
    }

    /**
     * 递归构建 Resource ID 到 TOCReference 的映射
     */
    private static void buildTocResourceMap(List<TOCReference> tocRefs, Map<String, TOCReference> map) {
        if (tocRefs == null) return;
        for (TOCReference ref : tocRefs) {
            Resource resource = ref.getResource();
            if (resource != null && resource.getId() != null) {
                // 判断map中是否有 Resource ID
                if (!map.containsKey(resource.getId())) {
                    // 添加 Resource ID 到 TOCReference 的映射
                    map.put(resource.getId(), ref);
                }
            }
            // 递归处理子项
            buildTocResourceMap(ref.getChildren(), map);
        }
    }

    /**
     * 递归查找指定 Resource ID 在 TOC 树中的层级深度
     *
     * @param tocRefs      当前处理的 TOCReference 列表
     * @param targetId     要查找的 Resource ID
     * @param currentLevel 当前搜索的层级
     * @return 找到的层级，未找到返回 -1
     */
    private static int findTocLevel(List<TOCReference> tocRefs, String targetId, int currentLevel) {
        if (tocRefs == null) return -1;
        for (TOCReference ref : tocRefs) {
            Resource resource = ref.getResource();
            if (resource != null && targetId.equals(resource.getId())) {
                return currentLevel;
            }
            // 在子项中递归查找
            int levelInChild = findTocLevel(ref.getChildren(), targetId, currentLevel + 1);
            if (levelInChild != -1) {
                return levelInChild;
            }
        }
        return -1;
    }
}