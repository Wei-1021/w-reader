package com.wei.wreader.reader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.jayway.jsonpath.JsonPath;
import com.wei.wreader.content.ContentFormatter;
import com.wei.wreader.content.ContentParser;
import com.wei.wreader.listener.BookDirectoryListener;
import com.wei.wreader.model.*;
import com.wei.wreader.service.AppConfigService;
import com.wei.wreader.service.AppStateService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.comm.ScriptCodeUtil;
import com.wei.wreader.util.comm.UrlUtil;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.ListUtil;
import com.wei.wreader.util.data.StringUtil;
import com.wei.wreader.util.file.EpubReaderComplete;
import com.wei.wreader.util.file.FileUtil;
import com.wei.wreader.util.http.HttpUtil;
import com.wei.wreader.util.ui.MessageDialogUtil;
import com.wei.wreader.util.ui.ToolWindowUtil;
import com.wei.wreader.util.yml.ConfigYaml;
import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.epub.EpubReader;
import io.documentnode.epub4j.util.IOUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节导航器 - 负责章节切换和目录管理
 */
public class ChapterNavigator {
    private static final Logger LOG = Logger.getInstance(ChapterNavigator.class);
    private static final String LOAD_FILE_TASK_TITLE = "【W-Reader】正在读取文件...";
    private static final String LOAD_NEXT_CONTENT_TITLE = "【W-Reader】加载本章节下一页内容";

    private final Project project;
    private final CacheService cacheService;
    private final AppConfigService appConfig;
    private final SiteRuleService siteRuleService;
    private final AppStateService appState;
    private final CustomSiteUtil customSiteUtil;
    private NextContentLoadTask nextContentTask;

    public ChapterNavigator(Project project, CacheService cacheService, AppConfigService appConfig,
                            SiteRuleService siteRuleService, AppStateService appState,
                            CustomSiteUtil customSiteUtil) {
        this.project = project;
        this.cacheService = cacheService;
        this.appConfig = appConfig;
        this.siteRuleService = siteRuleService;
        this.appState = appState;
        this.customSiteUtil = customSiteUtil;
    }

    /**
     * 显示当前小说目录
     */
    public void showBookDirectory(BookDirectoryListener listener) {
        SwingUtilities.invokeLater(() -> {
            Settings settings = cacheService.getSettings();
            int dataLoadType = settings.getDataLoadType();

            List<String> chapterList = cacheService.getChapterList();
            if (ListUtil.isEmpty(chapterList)) {
                Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CHAPTER_LIST_ERROR, "提示");
                return;
            }

            if (dataLoadType == Settings.DATA_LOAD_TYPE_LOCAL) {
                if (ListUtil.isEmpty(cacheService.getChapterContentList())) {
                    Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CHAPTER_LIST_ERROR, "提示");
                    return;
                }
            }

            ChapterInfo currentChapterInfo = cacheService.getSelectedChapterInfo();
            int currentChapterIndex = currentChapterInfo != null ? currentChapterInfo.getSelectedChapterIndex() : 0;

            JBList<String> chapterListJBList = new JBList<>(chapterList);
            chapterListJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            chapterListJBList.setBorder(JBUI.Borders.empty());
            chapterListJBList.setSelectedIndex(currentChapterIndex);
            chapterListJBList.ensureIndexIsVisible(currentChapterIndex);

            chapterListJBList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    handleChapterSelection(chapterListJBList, listener, dataLoadType);
                }
            });

            JBScrollPane scrollPane = new JBScrollPane(chapterListJBList);
            scrollPane.setPreferredSize(new Dimension(400, 500));
            MessageDialogUtil.showMessage(project, "目录", scrollPane);
        });
    }

    private void handleChapterSelection(JBList<String> chapterListJBList, BookDirectoryListener listener, int dataLoadType) {
        switch (dataLoadType) {
            case Settings.DATA_LOAD_TYPE_NETWORK:
                loadBookDirectoryRemote(chapterListJBList, listener);
                break;
            case Settings.DATA_LOAD_TYPE_LOCAL:
                loadBookDirectoryLocal(chapterListJBList, listener);
                break;
        }
    }

    /**
     * 远程加载小说目录
     */
    public void loadBookDirectoryRemote(JBList<String> chapterListJBList, BookDirectoryListener listener) {
        List<String> chapterList = cacheService.getChapterList();
        List<String> chapterUrlList = cacheService.getChapterUrlList();

        int selectedIndex = chapterListJBList.getSelectedIndex();
        if (selectedIndex < 0) selectedIndex = 0;
        if (selectedIndex >= chapterUrlList.size()) selectedIndex = chapterUrlList.size() - 1;

        String chapterTitle = chapterList.get(selectedIndex);
        if (ListUtil.isEmpty(chapterUrlList)) {
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
            return;
        }

        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
        String baseUrl = selectedSiteBean != null ? selectedSiteBean.getBaseUrl() : "";
        String chapterSuffixUrl = chapterUrlList.get(selectedIndex);
        String chapterUrl = buildFullChapterUrl(chapterSuffixUrl, baseUrl);

        ChapterInfo currentChapterInfo = cacheService.getSelectedChapterInfo();
        currentChapterInfo.setChapterTitle(chapterTitle);
        currentChapterInfo.setChapterUrl(chapterUrl);

        int finalSelectedIndex = selectedIndex;
        searchBookContentRemote(chapterUrl, (searchBookCallParam) -> {
            Settings settings = cacheService.getSettings();
            String chapterContentHtml = searchBookCallParam.getChapterContentHtml();
            String chapterContentText = searchBookCallParam.getChapterContentText();
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, finalSelectedIndex, settings.getSingleLineChars());
            cacheService.setSelectedChapterInfo(currentChapterInfo);

            if (listener != null) {
                listener.onClickItem(finalSelectedIndex, chapterList, currentChapterInfo, searchBookCallParam.getBodyElement());
            }
        });
    }

    /**
     * 本地加载小说目录
     */
    public void loadBookDirectoryLocal(JBList<String> chapterListJBList, BookDirectoryListener listener) {
        List<String> chapterContentList = cacheService.getChapterContentList();
        List<String> chapterList = cacheService.getChapterList();

        int selectedIndex = chapterListJBList.getSelectedIndex();
        if (selectedIndex < 0) selectedIndex = 0;
        if (chapterContentList != null && selectedIndex >= chapterContentList.size()) {
            selectedIndex = chapterContentList.size() - 1;
        }

        if (chapterContentList != null && !chapterContentList.isEmpty()) {
            String chapterTitle = chapterList.get(selectedIndex);
            ChapterInfo currentChapterInfo = cacheService.getSelectedChapterInfo();
            currentChapterInfo.setChapterTitle(chapterTitle);

            String chapterContentHtml = chapterContentList.get(selectedIndex);
            String chapterContentText = ContentFormatter.processChapterContentText(chapterContentHtml);

            Settings settings = cacheService.getSettings();
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, selectedIndex, settings.getSingleLineChars());
            cacheService.setSelectedChapterInfo(currentChapterInfo);

            if (listener != null) {
                listener.onClickItem(selectedIndex, chapterList, currentChapterInfo, null);
            }
        }
    }

    /**
     * 切换到上一个章节
     */
    public void prevPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        try {
            ChapterInfo currentChapterInfo = cacheService.getSelectedChapterInfo();
            List<String> chapterList = cacheService.getChapterList();
            int currentChapterIndex = currentChapterInfo.getSelectedChapterIndex();

            if (currentChapterIndex <= 0) return;
            if (currentChapterIndex >= chapterList.size()) currentChapterIndex = chapterList.size() - 1;

            currentChapterIndex--;
            String chapterTitle = chapterList.get(currentChapterIndex);
            currentChapterInfo.setChapterTitle(chapterTitle);
            currentChapterInfo.setSelectedChapterIndex(currentChapterIndex);

            Settings settings = cacheService.getSettings();
            if (settings.getDataLoadType() == Settings.DATA_LOAD_TYPE_NETWORK) {
                loadPrevChapterNetwork(currentChapterIndex, currentChapterInfo, runnable);
            } else {
                loadPrevChapterLocal(currentChapterIndex, currentChapterInfo, runnable);
            }
        } catch (Exception e) {
            LOG.error("Failed to load previous chapter", e);
        }
    }

    private void loadPrevChapterNetwork(int chapterIndex, ChapterInfo chapterInfo, BiConsumer<ChapterInfo, Element> runnable) {
        List<String> chapterUrlList = cacheService.getChapterUrlList();
        if (ListUtil.isEmpty(chapterUrlList)) {
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
            return;
        }

        if (chapterIndex < 0) chapterIndex = 0;
        if (chapterIndex >= chapterUrlList.size()) chapterIndex = chapterUrlList.size() - 1;
        final int finalChapterIndex = chapterIndex;

        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
        String baseUrl = selectedSiteBean != null ? selectedSiteBean.getBaseUrl() : "";
        String prevChapterSuffixUrl = chapterUrlList.get(chapterIndex);
        String prevChapterUrl = buildFullChapterUrl(prevChapterSuffixUrl, baseUrl);
        chapterInfo.setChapterUrl(prevChapterUrl);

        searchBookContentRemote(prevChapterUrl, (searchBookCallParam) -> {
            Settings settings = cacheService.getSettings();
            String chapterContentHtml = searchBookCallParam.getChapterContentHtml();
            String chapterContentText = searchBookCallParam.getChapterContentText();
            chapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, finalChapterIndex, settings.getSingleLineChars());
            cacheService.setSelectedChapterInfo(chapterInfo);
            runnable.accept(chapterInfo, searchBookCallParam.getBodyElement());
        });
    }

    private void loadPrevChapterLocal(int chapterIndex, ChapterInfo chapterInfo, BiConsumer<ChapterInfo, Element> runnable) {
        List<String> chapterContentList = cacheService.getChapterContentList();
        if (chapterContentList != null && !chapterContentList.isEmpty()) {
            if (chapterIndex < 0) chapterIndex = 0;
            if (chapterIndex >= chapterContentList.size()) chapterIndex = chapterContentList.size() - 1;

            String chapterContentHtml = chapterContentList.get(chapterIndex);
            String chapterContentText = ContentFormatter.processChapterContentText(chapterContentHtml);
            Settings settings = cacheService.getSettings();
            chapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, chapterIndex, settings.getSingleLineChars());
            cacheService.setSelectedChapterInfo(chapterInfo);
        }
        runnable.accept(chapterInfo, null);
    }

    /**
     * 切换到下一个章节
     */
    public void nextPageChapter(BiConsumer<ChapterInfo, Element> runnable) {
        try {
            Settings settings = cacheService.getSettings();
            if (settings.getDataLoadType() == Settings.DATA_LOAD_TYPE_NETWORK) {
                loadNextChapterNetwork(runnable);
            } else {
                loadNextChapterLocal(runnable);
            }
        } catch (Exception e) {
            LOG.error("Failed to load next chapter", e);
        }
    }

    private void loadNextChapterNetwork(BiConsumer<ChapterInfo, Element> runnable) {
        List<String> chapterList = cacheService.getChapterList();
        List<String> chapterUrlList = cacheService.getChapterUrlList();
        ChapterInfo currentChapterInfo = cacheService.getSelectedChapterInfo();
        int currentChapterIndex = currentChapterInfo.getSelectedChapterIndex();

        if (ListUtil.isEmpty(chapterUrlList)) {
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_CONTENT_ERROR, "提示");
            return;
        }

        if (currentChapterIndex >= chapterUrlList.size() - 1) return;
        if (currentChapterIndex < 0) currentChapterIndex = 0;

        currentChapterIndex++;
        final int nextIndex = currentChapterIndex;
        String chapterTitle = chapterList.get(nextIndex);
        currentChapterInfo.setChapterTitle(chapterTitle);
        currentChapterInfo.setSelectedChapterIndex(nextIndex);

        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
        String baseUrl = selectedSiteBean != null ? selectedSiteBean.getBaseUrl() : "";
        String nextChapterSuffixUrl = chapterUrlList.get(nextIndex);
        String nextChapterUrl = buildFullChapterUrl(nextChapterSuffixUrl, baseUrl);
        currentChapterInfo.setChapterUrl(nextChapterUrl);

        searchBookContentRemote(nextChapterUrl, (searchBookCallParam) -> {
            Settings settings = cacheService.getSettings();
            String chapterContentHtml = searchBookCallParam.getChapterContentHtml();
            String chapterContentText = searchBookCallParam.getChapterContentText();
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, nextIndex, settings.getSingleLineChars());
            cacheService.setSelectedChapterInfo(currentChapterInfo);
            runnable.accept(currentChapterInfo, searchBookCallParam.getBodyElement());
        });
    }

    private void loadNextChapterLocal(BiConsumer<ChapterInfo, Element> runnable) {
        List<String> chapterList = cacheService.getChapterList();
        List<String> chapterContentList = cacheService.getChapterContentList();
        ChapterInfo currentChapterInfo = cacheService.getSelectedChapterInfo();
        int currentChapterIndex = currentChapterInfo.getSelectedChapterIndex();

        if (chapterContentList == null || currentChapterIndex >= chapterContentList.size() - 1) return;
        if (currentChapterIndex < 0) currentChapterIndex = 0;

        currentChapterIndex++;
        String chapterTitle = chapterList.get(currentChapterIndex);
        currentChapterInfo.setChapterTitle(chapterTitle);
        currentChapterInfo.setSelectedChapterIndex(currentChapterIndex);

        if (!chapterContentList.isEmpty()) {
            String chapterContentHtml = chapterContentList.get(currentChapterIndex);
            String chapterContentText = ContentFormatter.processChapterContentText(chapterContentHtml);
            Settings settings = cacheService.getSettings();
            currentChapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, currentChapterIndex, settings.getSingleLineChars());
            cacheService.setSelectedChapterInfo(currentChapterInfo);
        }
        runnable.accept(currentChapterInfo, null);
    }

    /**
     * 远程获取小说内容
     */
    public void searchBookContentRemote(String url, Consumer<SearchBookCallParam> callback) {
        new ContentLoadTask(url, callback).queue();
    }

    /**
     * 加载本地文件
     *
     * @param regex TXT文件章节分割正则表达式
     */
    public void loadLocalFile(String regex) {
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false,
                false, false, false, false);
        fileChooserDescriptor.setTitle("选择文本文件");
        fileChooserDescriptor.setDescription(ConstUtil.WREADER_LOAD_LOCAL_TIP);

        VirtualFile virtualFile = FileChooser.chooseFile(fileChooserDescriptor, project, null);
        if (virtualFile != null) {
            processSelectedFile(virtualFile, regex);
        }
    }

    /**
     * 处理选中的本地文件
     */
    private void processSelectedFile(VirtualFile virtualFile, String regex) {
        String filePath = virtualFile.getPath();
        String fileExtension = virtualFile.getExtension();

        List<String> allowFileExtensions = ConfigYaml.getInstance().getAllowFileExtension();
        if (fileExtension == null || !allowFileExtensions.contains(fileExtension)) {
            String message = String.format(ConstUtil.WREADER_ONLY_SUPPORTED_FILE_TYPE,
                    allowFileExtensions.toString());
            Messages.showMessageDialog(message, "提示", Messages.getInformationIcon());
            return;
        }

        File file = new File(filePath);

        // 重置状态
        cacheService.setEditorMessageVerticalScrollValue(0);
        clearCacheData();

        new LocalFileLoadTask(file, fileExtension, regex).queue();
    }

    /**
     * 清空缓存数据
     */
    private void clearCacheData() {
        cacheService.setChapterList(null);
        cacheService.setChapterContentList(null);
        cacheService.setSelectedChapterInfo(null);
        cacheService.setSelectedBookInfo(null);
        cacheService.setChapterUrlList(null);
    }

    /**
     * 加载本章节下一页的内容
     *
     * @param chapterUrl  章节URL
     * @param bodyElement 页面body元素
     */
    public void loadThisChapterNextContent(String chapterUrl, Element bodyElement) {
        SiteBean siteBean = cacheService.getSelectedSiteBean();
        if (siteBean == null) {
            return;
        }
        ChapterRules chapterRules = siteBean.getChapterRules();

        if (chapterRules == null || StringUtils.isEmpty(chapterRules.getNextContentUrl())) {
            return;
        }

        if (!ScriptCodeUtil.isJavaCodeConfig(chapterRules.getNextContentUrl())) {
            return;
        }

        if (nextContentTask != null) {
            nextContentTask.onCancel();
        }

        nextContentTask = new NextContentLoadTask(chapterUrl, bodyElement);
        nextContentTask.queue();
    }

    /**
     * 请求下一页内容（用于NextContentLoadTask分页加载）
     */
    private String requestNextPageContent(String url, BiConsumer<String, Element> call) {
        String content = "";
        SiteBean siteBean = cacheService.getSelectedSiteBean();
        if (siteBean == null) return content;
        ChapterRules chapterRules = siteBean.getChapterRules();
        if (chapterRules == null) return content;

        try {
            if (chapterRules.isUseNextContentApi()) {
                HttpRequestBase requestBase = HttpUtil.commonRequest(url);
                requestBase.setHeader("User-Agent", ConstUtil.HEADER_USER_AGENT);
                try (CloseableHttpClient httpClient = HttpClients.createDefault();
                     CloseableHttpResponse httpResponse = httpClient.execute(requestBase)) {
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        HttpEntity entity = httpResponse.getEntity();
                        String result = EntityUtils.toString(entity);
                        JsonObject resJson = new Gson().fromJson(result, JsonObject.class);
                        Object readJson = JsonPath.read(resJson.toString(), chapterRules.getNextContentApiDataRule());
                        call.accept(result, null);
                        content = (String) readJson;
                    }
                }
            } else {
                Document document = Jsoup.connect(url)
                        .header("User-Agent", ConstUtil.HEADER_USER_AGENT)
                        .get();
                Element bodyElement = document.body();
                call.accept(bodyElement.html(), bodyElement);
                Elements chapterContentElements = bodyElement.select(chapterRules.getContentElementName());
                if (!chapterContentElements.isEmpty()) {
                    StringBuilder contentHtml = new StringBuilder();
                    for (Element element : chapterContentElements) {
                        Tag tag = element.tag();
                        String html = element.html();
                        if (!tag.isEmpty() && StringUtils.trimToNull(html) != null) {
                            contentHtml.append(String.format("<%s>%s</%s>",
                                    tag.normalName(), html, tag.normalName()));
                        }
                    }
                    content = contentHtml.toString();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to request next page content", e);
            call.accept("", null);
        }

        return ContentParser.formatAndApplyRegex(content, chapterRules);
    }

    /**
     * 获取带字体样式的内容HTML
     */
    private String getStyledContent(String text) {
        String fontFamily = cacheService.getFontFamily();
        int fontSize = cacheService.getFontSize();
        String fontColorHex = cacheService.getFontColorHex();
        if (fontColorHex == null) fontColorHex = "#cccccc";

        String style = "font-family: '" + fontFamily + "'; " +
                "font-size: " + fontSize + "px; color:" + fontColorHex + ";";
        text = text.replaceAll("(?s)<style[^>]*>.*?</style>", "");
        return "<div style=\"" + style + "\">" + text + "</div>";
    }

    // ==================== 本地文件加载辅助方法 ====================

    /**
     * 加载TXT格式文件
     */
    private void loadFileTypeTxt(File file, String regex) {
        String textRegex = StringUtils.isEmpty(regex) ? ConstUtil.TEXT_FILE_DIR_REGEX : regex;
        Settings settings = cacheService.getSettings();
        String charset = settings.getCharset();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {

            StringBuilder contentBuilder = new StringBuilder();
            String line;
            List<String> chapterList = new ArrayList<>();
            List<String> chapterContentList = new ArrayList<>();

            Pattern pattern = Pattern.compile(textRegex);
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if (!chapterList.isEmpty()) {
                        chapterContentList.add(contentBuilder.toString());
                    }
                    chapterList.add(line);
                    contentBuilder.setLength(0);
                }
                contentBuilder.append(line).append("<br>");
            }
            chapterContentList.add(contentBuilder.toString());

            cacheService.setChapterList(chapterList);
            cacheService.setChapterContentList(chapterContentList);

            BookInfo bookInfo = new BookInfo();
            bookInfo.setBookName(file.getName());
            bookInfo.setBookDesc(file.getName());
            cacheService.setSelectedBookInfo(bookInfo);

        } catch (IOException e) {
            LOG.error("Failed to load TXT file", e);
            Messages.showMessageDialog(ConstUtil.WREADER_LOAD_FAIL, "提示", Messages.getInformationIcon());
        }
    }

    /**
     * 加载EPUB格式文件
     */
    private void loadFileTypeEpub(File file) {
        Settings settings = cacheService.getSettings();
        String charset = settings.getCharset();
        boolean isShowLocalImg = settings.isShowLocalImg();

        try (FileInputStream fis = new FileInputStream(file)) {
            String tempDirPath = getTempDirectoryPath();
            FileUtils.deleteDirectory(new File(tempDirPath));

            EpubReader epubReader = new EpubReader();
            Book book = epubReader.readEpub(fis, charset);

            Map<String, String> imgTempPathMap = new HashMap<>();
            Map<String, Integer> imgTempWidthMap = new HashMap<>();

            if (isShowLocalImg) {
                processEpubImages(book, tempDirPath, imgTempPathMap, imgTempWidthMap);
            }

            List<String> chapterList = new ArrayList<>();
            List<String> chapterContentList = new ArrayList<>();

            EpubReaderComplete.readEpub(book, resMap -> {
                String title = resMap.get("title");
                String content = resMap.get("content");
                content = StringUtil.extractBodyContent(content);

                if (isShowLocalImg) {
                    content = StringUtil.replaceImageLinks(content, imgTempPathMap, imgTempWidthMap);
                }

                chapterList.add(title);
                chapterContentList.add(content);
            });

            cacheService.setChapterList(chapterList);
            cacheService.setChapterContentList(chapterContentList);

            saveBookMetadata(book);

        } catch (IOException e) {
            LOG.error("Failed to load EPUB file", e);
            Messages.showMessageDialog(ConstUtil.WREADER_LOAD_FAIL, "提示", Messages.getInformationIcon());
        }
    }

    /**
     * 获取临时目录路径
     */
    private String getTempDirectoryPath() {
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempDirFile = new File(tempDir);
        return tempDirFile.getAbsolutePath() + File.separator + ConstUtil.WREADER_ID +
                File.separator + "images" + File.separator;
    }

    /**
     * 处理EPUB中的图片资源
     */
    private void processEpubImages(Book book, String tempDirPath,
                                   Map<String, String> imgPathMap,
                                   Map<String, Integer> imgWidthMap) throws IOException {
        Map<String, Resource> resourceMap = book.getResources().getResourceMap();
        for (Map.Entry<String, Resource> entry : resourceMap.entrySet()) {
            Resource resource = entry.getValue();
            String key = entry.getKey();
            if ((resource.getMediaType() != null &&
                    resource.getMediaType().getName().startsWith("image/")) ||
                    FileUtil.isUnsupportedImageFormat(key)) {
                processImageResource(resource, key, tempDirPath, imgPathMap, imgWidthMap);
            }
        }
    }

    /**
     * 处理单个EPUB图片资源
     */
    private void processImageResource(Resource resource, String key, String tempDirPath,
                                      Map<String, String> imgPathMap,
                                      Map<String, Integer> imgWidthMap) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] data = IOUtil.toByteArray(inputStream);
            String filePath = tempDirPath + key;

            if (FileUtil.isUnsupportedImageFormat(key)) {
                filePath = FileUtil.convertImgToJPG(data, filePath);
            } else {
                FileUtils.writeByteArrayToFile(new File(filePath), data);
            }

            if (StringUtils.isNotBlank(filePath)) {
                BufferedImage originalImage = ImageIO.read(new File(filePath));
                imgWidthMap.put(key, originalImage.getWidth());
                imgPathMap.put(key, "file:///" + filePath.replace("\\", "/"));
            }
        }
    }

    /**
     * 保存EPUB书籍元数据
     */
    private void saveBookMetadata(Book book) {
        BookInfo bookInfo = new BookInfo();
        String bookName = ListUtil.listToString(book.getMetadata().getTitles());
        bookInfo.setBookName(bookName);

        List<String> descriptions = book.getMetadata().getDescriptions();
        String bookDesc = descriptions != null && !descriptions.isEmpty() ?
                ListUtil.listToString(descriptions) : bookName;
        bookInfo.setBookDesc(bookDesc);

        String author = ListUtil.listToString(book.getMetadata().getAuthors());
        bookInfo.setBookAuthor(author);

        cacheService.setSelectedBookInfo(bookInfo);
    }

    /**
     * 构建完整的章节URL
     */
    private String buildFullChapterUrl(String chapterSuffixUrl, String baseUrl) {
        if (chapterSuffixUrl.startsWith(ConstUtil.HTTP_SCHEME) ||
                chapterSuffixUrl.startsWith(ConstUtil.HTTPS_SCHEME)) {
            return chapterSuffixUrl;
        }
        return baseUrl + chapterSuffixUrl;
    }

    /**
     * 内容加载后台任务
     */
    private class ContentLoadTask extends Task.Backgroundable {
        private final String url;
        private final Consumer<SearchBookCallParam> callback;

        ContentLoadTask(String url, Consumer<SearchBookCallParam> callback) {
            super(project, "【W-Reader】正在获取内容...");
            this.url = url;
            this.callback = callback;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText("【W-Reader】正在获取内容...");
            indicator.setIndeterminate(true);

            SiteBean siteBean = cacheService.getSelectedSiteBean();
            if (siteBean == null || StringUtils.isBlank(siteBean.getId())) {
                Messages.showErrorDialog(ConstUtil.WREADER_SITE_BEAN_ERROR, "提示");
                return;
            }

            ChapterRules chapterRules = siteBean.getChapterRules();
            try {
                String chapterContent;
                String contentText = "";
                Element bodyElement = null;
                String bodyElementStr = "";
                String contentOriginalStyle = "";

                if (ContentParser.shouldUseApiMethod(chapterRules)) {
                    chapterContent = ContentParser.loadContentViaApi(url, chapterRules);
                } else {
                    ContentParser.HtmlParseResult result = ContentParser.loadContentViaHtml(url, chapterRules);
                    chapterContent = result.getContentHtml();
                    contentText = result.getContentText();
                    bodyElement = result.getBodyElement();
                    bodyElementStr = bodyElement != null ? bodyElement.html() : "";
                    contentOriginalStyle = result.getContentOriginalStyle();
                }

                chapterContent = ContentParser.handleContent(chapterContent, siteBean);

                // Add chapter title
                ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
                Settings settings = cacheService.getSettings();
                String fontColorHex = cacheService.getFontColorHex();
                if (fontColorHex == null) fontColorHex = "#cccccc";

                chapterContent = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" +
                        fontColorHex + ";\">" + chapterInfo.getChapterTitle() +
                        "</h3>" + chapterContent;

                String chapterContentText = ContentFormatter.processChapterContentText(chapterContent);

                SearchBookCallParam param = new SearchBookCallParam();
                param.setBodyElement(bodyElement);
                param.setBodyContentStr(bodyElementStr);
                param.setChapterContentHtml(chapterContent);
                param.setChapterContentText(chapterContentText);

                ApplicationManager.getApplication().invokeLater(() -> callback.accept(param));

            } catch (Exception e) {
                LOG.error("Content load failed for URL: " + url, e);
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog(ConstUtil.WREADER_SEARCH_NETWORK_ERROR, "提示");
                });
            }
        }
    }

    /**
     * 下一页内容加载任务（分页加载当前章节的后续页面）
     */
    private class NextContentLoadTask extends Task.Backgroundable {
        private final String chapterUrl;
        private String initialBodyContent;
        private Element initialBodyElement;
        private volatile boolean isRunning = true;
        private String returnResult;
        private final String nextContentUrl;
        private final StringBuilder nextContent = new StringBuilder();

        public NextContentLoadTask(String chapterUrl, Element bodyContent) {
            super(project, LOAD_NEXT_CONTENT_TITLE);
            this.chapterUrl = chapterUrl;
            this.initialBodyContent = bodyContent.html();
            this.initialBodyElement = bodyContent;
            this.nextContentUrl = getNextContentUrl();
            this.returnResult = nextContentUrl;
        }

        private String getNextContentUrl() {
            SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
            if (selectedSiteBean == null || StringUtils.isBlank(selectedSiteBean.getId())) {
                return "";
            }
            ChapterRules chapterRules = selectedSiteBean.getChapterRules();
            if (chapterRules == null) {
                return "";
            }
            return chapterRules.getNextContentUrl();
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setIndeterminate(true);

            try {
                SiteBean siteBean = cacheService.getSelectedSiteBean();
                String baseUrl = siteBean != null ? siteBean.getBaseUrl() : "";
                String previousContentUrl = "";
                int pageCount = 0;

                while (isRunning) {
                    indicator.checkCanceled();
                    pageCount++;
                    indicator.setText2("正在加载第 " + pageCount + " 页...");

                    returnResult = executeNextContentScript(chapterUrl, pageCount,
                            previousContentUrl, initialBodyContent, initialBodyElement);

                    if (StringUtils.isBlank(returnResult)) {
                        isRunning = false;
                        break;
                    }

                    returnResult = UrlUtil.buildFullURL(baseUrl, returnResult);
                    previousContentUrl = returnResult;

                    nextContent.append(requestNextPageContent(returnResult, (resContent, resElement) -> {
                        initialBodyContent = resContent;
                        initialBodyElement = resElement;
                    }));

                    Thread.sleep(1000);
                }
            } catch (ProcessCanceledException e) {
                LOG.debug("Next content loading cancelled");
            } catch (Exception e) {
                LOG.error("Failed to load next page content", e);
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog(
                            "本章节下一页内容加载失败: " + e.getMessage(),
                            "提示"
                    );
                });
            }
        }

        private String executeNextContentScript(String chapterUrl, int pageCount, String previousUrl,
                                                String previousContent, Element previousElement) {
            try {
                return (String) ScriptCodeUtil.getScriptCodeExeResult(
                        nextContentUrl,
                        new Class[]{String.class, int.class, String.class, String.class},
                        new Object[]{chapterUrl, pageCount, previousUrl, previousContent},
                        Map.of(
                                "chapterUrl", chapterUrl,
                                "loadingPage", pageCount,
                                "preContentUrl", previousUrl,
                                "prePageContent", previousContent,
                                "prePageElement", previousElement
                        )
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onCancel() {
            isRunning = false;
            super.onCancel();
        }

        @Override
        public void onSuccess() {
            ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
            if (selectedChapterInfo == null) {
                return;
            }

            String text = nextContent.toString();
            String fontColorHex = cacheService.getFontColorHex();
            if (fontColorHex == null) fontColorHex = "#cccccc";
            text = "<h3 style=\"text-align: center;margin-bottom: 20px;color:" +
                    fontColorHex + ";\">" + selectedChapterInfo.getChapterTitle() + "</h3>" + text;

            // 更新缓存中的章节内容
            updateChapterInfoWithContent(text);

            // 通知所有显示模式更新内容
            ReaderOrchestrator.getInstance(project).updateContentText(text);
        }

        private void updateChapterInfoWithContent(String text) {
            SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
            if (selectedSiteBean == null || StringUtils.isBlank(selectedSiteBean.getId())) {
                return;
            }
            ChapterRules chapterRules = selectedSiteBean.getChapterRules();

            Pattern pattern = Pattern.compile(ConstUtil.HTML_TAG_REGEX_STR);
            String chapterContentText = pattern.matcher(text).replaceAll("　");
            chapterContentText = StringUtils.normalizeSpace(chapterContentText);
            chapterContentText = StringEscapeUtils.unescapeHtml4(chapterContentText);
            chapterContentText = ContentParser.formatAndApplyRegex(chapterContentText, chapterRules);

            ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
            // 将章节内容分割成集合
            Settings settings = cacheService.getSettings();
            List<String> contentArr = StringUtil.splitStringByMaxCharList(
                    chapterContentText,
                    settings.getSingleLineChars()
            );
            text = text.replaceAll("(?s)<style[^>]*>.*?</style>", "");
            selectedChapterInfo.setChapterContent(text);
            selectedChapterInfo.setChapterContentStr(chapterContentText);
            selectedChapterInfo.setChapterContentList(contentArr);
            cacheService.setSelectedChapterInfo(selectedChapterInfo);

        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            if (!(error instanceof ProcessCanceledException)) {
                super.onThrowable(error);
            }
        }
    }

    /**
     * 本地文件加载任务
     */
    private class LocalFileLoadTask extends Task.Backgroundable {
        private final File file;
        private final String fileExtension;
        private final String regex;

        public LocalFileLoadTask(File file, String fileExtension, String regex) {
            super(project, LOAD_FILE_TASK_TITLE);
            this.file = file;
            this.fileExtension = fileExtension;
            this.regex = regex;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText(LOAD_FILE_TASK_TITLE);
            indicator.setIndeterminate(true);

            if (ConstUtil.FILE_TYPE_TXT.equalsIgnoreCase(fileExtension)) {
                loadFileTypeTxt(file, regex);
            } else if (ConstUtil.FILE_TYPE_EPUB.equalsIgnoreCase(fileExtension)) {
                loadFileTypeEpub(file);
            }
        }

        @Override
        public void onSuccess() {
            ChapterInfo chapterInfo = ChapterInfo.initEmptyChapterInfo();
            cacheService.setSelectedChapterInfo(chapterInfo);

            Settings settings = cacheService.getSettings();
            settings.setDataLoadType(Settings.DATA_LOAD_TYPE_LOCAL);
            cacheService.setSettings(settings);

            List<String> chapterList = cacheService.getChapterList();
            List<String> chapterContentList = cacheService.getChapterContentList();

            if (chapterList != null && !chapterList.isEmpty() &&
                    chapterContentList != null && !chapterContentList.isEmpty()) {
                String chapterTitle = chapterList.get(0);
                chapterInfo.setChapterTitle(chapterTitle);
                String chapterContentHtml = chapterContentList.get(0);
                String chapterContentText = ContentFormatter.processChapterContentText(chapterContentHtml);
                chapterInfo.initChapterInfo(chapterContentHtml, chapterContentText, 0, settings.getSingleLineChars());
                cacheService.setSelectedChapterInfo(chapterInfo);

                String styledContent = getStyledContent(chapterContentHtml);
                ToolWindowUtil.updateContentText(project, styledContent);
            }

            Messages.showMessageDialog(ConstUtil.WREADER_LOAD_SUCCESS, "提示", Messages.getInformationIcon());
        }

        @Override
        public void onThrowable(@NotNull Throwable error) {
            super.onThrowable(error);
            Messages.showErrorDialog(ConstUtil.WREADER_LOAD_FAIL, MessageDialogUtil.TITLE_ERROR);
        }
    }
}
