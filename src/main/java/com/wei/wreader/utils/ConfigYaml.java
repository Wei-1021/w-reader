package com.wei.wreader.utils;

import com.wei.wreader.pojo.BookSiteInfo;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * yaml 配置文件解析器
 * @author weizhanjie"
 */
public class ConfigYaml {

    private static final String CONFIG_FILE_PATH = "config.yml";

    private static Map<String, Object> DATA_MAP = new LinkedHashMap<>();
    private static ConfigYaml instance;

    /**
     * 获取单例
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

    public List<BookSiteInfo> getSiteList() {
        List<LinkedHashMap<String, Object>> siteListList = (List<LinkedHashMap<String, Object> >) getObject("wreader.site_list");

        List<BookSiteInfo> siteList = new ArrayList<>();
        siteListList.forEach(objMap -> {
            BookSiteInfo bookSiteInfo = new BookSiteInfo();
            bookSiteInfo.setId(objMap.get("id").toString());
            bookSiteInfo.setName(objMap.get("name").toString());
            bookSiteInfo.setBaseUrl(objMap.get("baseUrl").toString());
            bookSiteInfo.setSearchUrl(objMap.get("searchUrl").toString());
            bookSiteInfo.setSearchBookNameParam(objMap.get("searchBookNameParam").toString());
            bookSiteInfo.setBookListElementName(objMap.get("bookListElementName").toString());
            bookSiteInfo.setBookListElementType(objMap.get("bookListElementType").toString());
            bookSiteInfo.setListMainElementName(objMap.get("listMainElementName").toString());
            bookSiteInfo.setListMainElementType(objMap.get("listMainElementType").toString());
            bookSiteInfo.setChapterContentElementName(objMap.get("chapterContentElementName").toString());
            bookSiteInfo.setChapterContentElementType(objMap.get("chapterContentElementType").toString());
            bookSiteInfo.setBookNameField(objMap.get("bookNameField").toString());
            bookSiteInfo.setBookUrlField(objMap.get("bookUrlField").toString());
            bookSiteInfo.setBookAuthorField(objMap.get("bookAuthorField").toString());
            bookSiteInfo.setBookDescField(objMap.get("bookDescField").toString());
            bookSiteInfo.setBookImgUrlField(objMap.get("bookImgUrlField").toString());
            bookSiteInfo.setHtml(Boolean.parseBoolean(objMap.get("isHtml").toString()));
            siteList.add(bookSiteInfo);
        });
        return siteList;
    }
}
