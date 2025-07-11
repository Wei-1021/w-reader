package com.wei.wreader.utils;

import com.wei.wreader.pojo.BookSiteInfo;
import com.wei.wreader.pojo.ComponentIdKey;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.pojo.ToolWindowInfo;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * yaml 配置文件解析器
 *
 * @author weizhanjie"
 */
public class ConfigYaml {

    private static final String CONFIG_FILE_PATH = "config.yml";

    private static Map<String, Object> DATA_MAP = new LinkedHashMap<>();
    private static ConfigYaml instance;

    /**
     * 获取单例
     *
     * @return
     */
    public static ConfigYaml getInstance() {
        if (instance == null) {
            instance = new ConfigYaml();
        }
        return instance;
    }

    public ConfigYaml() {
        Yaml yaml = new Yaml();
        // 加载路径为classpath下的constConfig.yml
        InputStream is = ConfigYaml.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
        Map<String, Object> ymlMap = new LinkedHashMap<>();
        for (Object obj : yaml.loadAll(is)) {
            ymlMap = asMap(obj);
        }

        DATA_MAP = ymlMap;
    }

    /**
     * 获取对应的字符串结果
     *
     * @param name
     * @return
     */
    public static String getValue(String name) {
        if (name == null) {
            return null;
        }

        Yaml yaml = new Yaml();
        // 加载路径为classpath下的constConfig.yml
        InputStream is = ConfigYaml.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
        Properties properties = yaml.loadAs(is, Properties.class);
        return properties.getProperty(name);
    }

    /**
     * 获取对应的结果
     *
     * @param name
     * @return
     */
    public Object getObject(String name) {
        Object result = null;
        if (name == null) {
            return result;
        }

        Map<String, Object> ymlMap = DATA_MAP;
        String[] keys = name.split("\\.");
        // 遍历节点数组，获取子节点结果
        for (String key : keys) {
            if (ymlMap.containsKey(key)) {
                Object resultObj = ymlMap.get(key);
                if (resultObj instanceof Map) {
                    ymlMap = (Map<String, Object>) resultObj;
                } else {
                    result = resultObj;
                }
            }
        }

        if (result == null) {
            result = ymlMap;
        }

        return result;
    }

    private static Map<String, Object> asMap(Object object) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            result.put("document", object);
            return result;
        } else {
            Map<Object, Object> map = (Map) object;
            map.forEach((key, value) -> {
                if (value instanceof Map) {
                    value = asMap(value);
                }

                if (key instanceof CharSequence) {
                    result.put(key.toString(), value);
                } else {
                    result.put("[" + key.toString() + "]", value);
                }

            });
            return result;
        }
    }

    /**
     * 获取配置文件名称
     *
     * @return
     */
    public String getName() {
        return (String) getObject("wreader.name");
    }

    public String getNameHump() {
        return (String) getObject("wreader.nameHump");
    }

    public String getVersion() {
        return (String) getObject("wreader.version");
    }

    public String getDescription() {
        return (String) getObject("wreader.description");
    }

    public String getAuthor() {
        return (String) getObject("wreader.author");
    }

    public Map<String, Object> getLanguage() {
        return (Map<String, Object>) getObject("wreader.language");
    }

    public List<String> getAllowFileExtension() {
        return (List<String>) getObject("wreader.allowFileExtension");
    }

    public ComponentIdKey getComponentIdKey() {
        LinkedHashMap<String, Object> componentIdKeyMap = (LinkedHashMap<String, Object>) getObject("wreader.componentIdKey");
        ComponentIdKey componentIdKey = new ComponentIdKey();
        componentIdKey.setBookDirectory(componentIdKeyMap.get("bookDirectory").toString());
        componentIdKey.setNextChapter(componentIdKeyMap.get("nextChapter").toString());
        componentIdKey.setPrevChapter(componentIdKeyMap.get("prevChapter").toString());
        componentIdKey.setPrevLine(componentIdKeyMap.get("prevLine").toString());
        componentIdKey.setNextLine(componentIdKeyMap.get("nextLine").toString());
        componentIdKey.setSearchBook(componentIdKeyMap.get("searchBook").toString());
        componentIdKey.setSetting(componentIdKeyMap.get("setting").toString());
        return componentIdKey;
    }

    public Settings getSettings() {
        LinkedHashMap<String, Object> settingsMap = (LinkedHashMap<String, Object>) getObject("wreader.settings");
        Settings settings = new Settings();
        settings.setSingleLineChars(Integer.parseInt(settingsMap.get("singleLineChars").toString()));
        settings.setShowLineNum(Boolean.parseBoolean(settingsMap.get("isShowLineNum").toString()));
        settings.setDisplayType(Integer.parseInt(settingsMap.get("displayType").toString()));
        settings.setDataLoadType(Integer.parseInt(settingsMap.get("dataLoadType").toString()));
        settings.setCharset(settingsMap.get("charset").toString());
        settings.setAutoReadTime(Integer.parseInt(settingsMap.get("autoReadTime").toString()));
        settings.setMainIconStyle(Integer.parseInt(settingsMap.get("mainIconStyle").toString()));
        settings.setEditorHintWidth(Integer.parseInt(settingsMap.get("editorHintWidth").toString()));
        settings.setEditorHintHeight(Integer.parseInt(settingsMap.get("editorHintHeight").toString()));
        settings.setVoiceRole(settingsMap.get("voiceRole").toString());
        settings.setAudioTimeout(Integer.parseInt(settingsMap.get("audioTimeout").toString()));
        settings.setRate(Float.parseFloat(settingsMap.get("rate").toString()));
        settings.setVolume(Integer.parseInt(settingsMap.get("volume").toString()));
        settings.setAudioStyle(settingsMap.get("audioStyle").toString());
        return settings;
    }

    public ToolWindowInfo getToolWindow() {
        LinkedHashMap<String, Object> toolWindowMap = (LinkedHashMap<String, Object>) getObject("wreader.toolWindow");
        ToolWindowInfo toolWindowInfo = new ToolWindowInfo();
        toolWindowInfo.setSearchTitle(toolWindowMap.get("search-title").toString());
        toolWindowInfo.setFontSizeSubTitle(toolWindowMap.get("font-size-sub-title").toString());
        toolWindowInfo.setFontSizeAddTitle(toolWindowMap.get("font-size-add-title").toString());
        toolWindowInfo.setChapterListTitle(toolWindowMap.get("chapter-list-title").toString());
        toolWindowInfo.setPrevChapterTitle(toolWindowMap.get("prev-chapter-title").toString());
        toolWindowInfo.setNextChapterTitle(toolWindowMap.get("next-chapter-title").toString());
        return toolWindowInfo;
    }

    public List<BookSiteInfo> getSiteList() {
        List<LinkedHashMap<String, Object>> siteListList = (List<LinkedHashMap<String, Object>>) getObject("wreader.site_list");

        List<BookSiteInfo> siteList = new ArrayList<>();
        siteListList.forEach(objMap -> {
            BookSiteInfo bookSiteInfo = new BookSiteInfo();
            bookSiteInfo.setEnabled(Boolean.parseBoolean(objMap.get("isEnabled").toString()));
            bookSiteInfo.setId(objMap.get("id").toString());
            bookSiteInfo.setName(objMap.get("name").toString());
            bookSiteInfo.setBaseUrl(objMap.get("baseUrl").toString());
            bookSiteInfo.setSearchUrl(objMap.get("searchUrl").toString());
            bookSiteInfo.setHeader(objMap.get("header").toString());
            bookSiteInfo.setSearchBookNameParam(objMap.get("searchBookNameParam").toString());
            bookSiteInfo.setSearchDataBookListRule(objMap.get("searchDataBookListRule").toString());
            bookSiteInfo.setBookDataId(objMap.get("bookDataId").toString());
            bookSiteInfo.setBookListElementName(objMap.get("bookListElementName").toString());
            bookSiteInfo.setBookListUrlElement(objMap.get("bookListUrlElement").toString());
            bookSiteInfo.setBookListTitleElement(objMap.get("bookListTitleElement").toString());
            bookSiteInfo.setListMainUrl(objMap.get("listMainUrl").toString());
            bookSiteInfo.setListMainUrlDataRule(objMap.get("listMainUrlDataRule").toString());
            bookSiteInfo.setIsSavelistMainUrlData(Boolean.parseBoolean(objMap.get("isSavelistMainUrlData").toString()));
            bookSiteInfo.setListMainItemIdField(objMap.get("listMainItemIdField").toString());
            bookSiteInfo.setListMainItemTitleField(objMap.get("listMainItemTitleField").toString());
            bookSiteInfo.setListMainElementName(objMap.get("listMainElementName").toString());
            bookSiteInfo.setChapterListUrlElement(objMap.get("chapterListUrlElement").toString());
            bookSiteInfo.setChapterListTitleElement(objMap.get("chapterListTitleElement").toString());
            bookSiteInfo.setChapterContentUrl(objMap.get("chapterContentUrl").toString());
            bookSiteInfo.setChapterContentUrlDataRule(objMap.get("chapterContentUrlDataRule").toString());
            bookSiteInfo.setChapterContentHandleRule(objMap.get("chapterContentHandleRule").toString());
            bookSiteInfo.setContentOriginalStyle(Boolean.parseBoolean(objMap.get("isContentOriginalStyle").toString()));
            bookSiteInfo.setReplaceContentOriginalRegex(objMap.get("replaceContentOriginalRegex").toString());
            bookSiteInfo.setChapterContentElementName(objMap.get("chapterContentElementName").toString());
            bookSiteInfo.setChapterContentRegex(objMap.get("chapterContentRegex").toString());
            bookSiteInfo.setBookIdField(objMap.get("bookIdField").toString());
            bookSiteInfo.setBookNameField(objMap.get("bookNameField").toString());
            bookSiteInfo.setBookUrlField(objMap.get("bookUrlField").toString());
            bookSiteInfo.setBookAuthorField(objMap.get("bookAuthorField").toString());
            bookSiteInfo.setBookDescField(objMap.get("bookDescField").toString());
            bookSiteInfo.setBookImgUrlField(objMap.get("bookImgUrlField").toString());
            bookSiteInfo.setHtml(Boolean.parseBoolean(objMap.get("isHtml").toString()));
            bookSiteInfo.setPathParam(Boolean.parseBoolean(objMap.get("isPathParam").toString()));
            siteList.add(bookSiteInfo);
        });
        return siteList;
    }

    /**
     * 获取所有启用的站点
     *
     * @return
     */
    public List<BookSiteInfo> getEnableSiteList() {
        List<LinkedHashMap<String, Object>> siteListList = (List<LinkedHashMap<String, Object>>) getObject("wreader.site_list");

        List<BookSiteInfo> siteList = new ArrayList<>();
        siteListList.forEach(objMap -> {
            boolean isEnabled = Boolean.parseBoolean(objMap.get("isEnabled").toString());
            if (isEnabled) {
                BookSiteInfo bookSiteInfo = new BookSiteInfo();
                bookSiteInfo.setEnabled(isEnabled);
                bookSiteInfo.setId(objMap.get("id").toString());
                bookSiteInfo.setName(objMap.get("name").toString());
                bookSiteInfo.setBaseUrl(objMap.get("baseUrl").toString());
                bookSiteInfo.setSearchUrl(objMap.get("searchUrl").toString());
                bookSiteInfo.setHeader(objMap.get("header").toString());
                bookSiteInfo.setSearchBookNameParam(objMap.get("searchBookNameParam").toString());
                bookSiteInfo.setSearchDataBookListRule(objMap.get("searchDataBookListRule").toString());
                bookSiteInfo.setBookDataId(objMap.get("bookDataId").toString());
                bookSiteInfo.setBookListElementName(objMap.get("bookListElementName").toString());
                bookSiteInfo.setBookListUrlElement(objMap.get("bookListUrlElement").toString());
                bookSiteInfo.setBookListTitleElement(objMap.get("bookListTitleElement").toString());
                bookSiteInfo.setListMainUrl(objMap.get("listMainUrl").toString());
                bookSiteInfo.setListMainUrlDataRule(objMap.get("listMainUrlDataRule").toString());
                bookSiteInfo.setIsSavelistMainUrlData(Boolean.parseBoolean(objMap.get("isSavelistMainUrlData").toString()));
                bookSiteInfo.setListMainItemIdField(objMap.get("listMainItemIdField").toString());
                bookSiteInfo.setListMainItemTitleField(objMap.get("listMainItemTitleField").toString());
                bookSiteInfo.setListMainElementName(objMap.get("listMainElementName").toString());
                bookSiteInfo.setChapterListUrlElement(objMap.get("chapterListUrlElement").toString());
                bookSiteInfo.setChapterListTitleElement(objMap.get("chapterListTitleElement").toString());
                bookSiteInfo.setChapterContentUrl(objMap.get("chapterContentUrl").toString());
                bookSiteInfo.setChapterContentUrlDataRule(objMap.get("chapterContentUrlDataRule").toString());
                bookSiteInfo.setChapterContentHandleRule(objMap.get("chapterContentHandleRule").toString());
                bookSiteInfo.setContentOriginalStyle(Boolean.parseBoolean(objMap.get("isContentOriginalStyle").toString()));
                bookSiteInfo.setReplaceContentOriginalRegex(objMap.get("replaceContentOriginalRegex").toString());
                bookSiteInfo.setChapterContentElementName(objMap.get("chapterContentElementName").toString());
                bookSiteInfo.setChapterContentRegex(objMap.get("chapterContentRegex").toString());
                bookSiteInfo.setBookIdField(objMap.get("bookIdField").toString());
                bookSiteInfo.setBookNameField(objMap.get("bookNameField").toString());
                bookSiteInfo.setBookUrlField(objMap.get("bookUrlField").toString());
                bookSiteInfo.setBookAuthorField(objMap.get("bookAuthorField").toString());
                bookSiteInfo.setBookDescField(objMap.get("bookDescField").toString());
                bookSiteInfo.setBookImgUrlField(objMap.get("bookImgUrlField").toString());
                bookSiteInfo.setHtml(Boolean.parseBoolean(objMap.get("isHtml").toString()));
                bookSiteInfo.setPathParam(Boolean.parseBoolean(objMap.get("isPathParam").toString()));
                siteList.add(bookSiteInfo);
            }
        });
        return siteList;
    }
}
