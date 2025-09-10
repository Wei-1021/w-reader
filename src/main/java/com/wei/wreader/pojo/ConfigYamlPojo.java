package com.wei.wreader.pojo;

import java.util.List;
import java.util.Map;

/**
 * yaml配置文件
 * @author weizhanjie
 */
public class ConfigYamlPojo {
    private Wreader wreader;

    public Wreader getWreader() {
        return wreader;
    }

    public void setWreader(Wreader wreader) {
        this.wreader = wreader;
    }

    public static class Wreader {
        /**
         * 项目名称
         */
        private String name;
        /**
         * 项目驼峰名称
         */
        private String nameHump;
        /**
         * 项目版本
         */
        private String version;
        /**
         * 项目描述
         */
        private String description;
        /**
         * 项目作者
         */
        private String author;
        /**
         * 允许的文件后缀
         */
        private List<String> allowFileExtension;
        /**
         * 设置
         */
        private Settings settings;
        /**
         * 各类开发语言的注释配置
         */
        private Map<String, Object> language;
        /**
         * 站点列表
         */
        private List<SiteBean> siteList;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNameHump() {
            return nameHump;
        }

        public void setNameHump(String nameHump) {
            this.nameHump = nameHump;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public List<String> getAllowFileExtension() {
            return allowFileExtension;
        }

        public void setAllowFileExtension(List<String> allowFileExtension) {
            this.allowFileExtension = allowFileExtension;
        }

        public Settings getSettings() {
            return settings;
        }

        public void setSettings(Settings settings) {
            this.settings = settings;
        }

        public Map<String, Object> getLanguage() {
            return language;
        }

        public void setLanguage(Map<String, Object> language) {
            this.language = language;
        }

        public List<SiteBean> getSiteList() {
            return siteList;
        }

        public void setSiteList(List<SiteBean> siteList) {
            this.siteList = siteList;
        }

        @Override
        public String toString() {
            return "Wreader{" +
                    "name='" + name + '\'' +
                    ", nameHump='" + nameHump + '\'' +
                    ", version='" + version + '\'' +
                    ", description='" + description + '\'' +
                    ", author='" + author + '\'' +
                    ", allowFileExtension=" + allowFileExtension +
                    ", settings=" + settings +
                    ", language=" + language +
                    ", siteList=" + siteList +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ConfigYamlPojo{" +
                "wreader=" + wreader +
                '}';
    }
}