package com.wei.wreader.pojo;

/**
 * 目录规则
 * @author weizhanjie
 */
public class ListMainRules {
    /**
     * 目录url
     */
    private String url;
    /**
     * 目录列表JSONPath规则
     */
    private String urlDataRule;
    /**
     * 目录列表项id字段名称
     */
    private String itemIdField;
    /**
     * 目录列表项标题字段名称
     */
    private String itemTitleField;
    /**
     * 目录列表项URL字段名称
     */
    private String itemUrlField;
    /**
     * 目录列表内容处理规则；只有当请求结果为JSON类型数据，并且通过urlDataRule规则获取到数据后，才执行此规则；
     * 此规则会对获取的目录列表数据进行进一步处理，例如：将目录列表数据中的URL字段进行拼接等；
     */
    private String urlDataHandleRule;
    /**
     * 目录列表的HTML标签元素名称
     */
    private String listMainElementName;
    /**
     * 目录链接元素CssSelector
     */
    private String urlElement;
    /**
     * 目录标题元素CssSelector
     */
    private String titleElement;
    /**
     * 目录列表的下一页目录的链接（针对某些网站会把目录列表分成多页的情况）。<br>
     * 可以是请求链接，也可以用代码，然后返回一个链接
     */
    private String nextListMainUrl;
    /**
     * 目录列表的下一页目录是否为使用API请求的方式获取。<br>
     * true: 使用API请求方式获取。<br>
     * false: 使用HTML页面获取。
     */
    private boolean useNextListMainApi;
    /**
     * 目录列表的下一页目录为API请求时，获取下一页目录的JSONPath规则
     */
    private String nextListMainApiDataRule;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlDataRule() {
        return urlDataRule;
    }

    public void setUrlDataRule(String urlDataRule) {
        this.urlDataRule = urlDataRule;
    }

    public String getItemIdField() {
        return itemIdField;
    }

    public void setItemIdField(String itemIdField) {
        this.itemIdField = itemIdField;
    }

    public String getItemTitleField() {
        return itemTitleField;
    }

    public void setItemTitleField(String itemTitleField) {
        this.itemTitleField = itemTitleField;
    }

    public String getItemUrlField() {
        return itemUrlField;
    }

    public void setItemUrlField(String itemUrlField) {
        this.itemUrlField = itemUrlField;
    }

    public String getUrlDataHandleRule() {
        return urlDataHandleRule;
    }

    public void setUrlDataHandleRule(String urlDataHandleRule) {
        this.urlDataHandleRule = urlDataHandleRule;
    }

    public String getListMainElementName() {
        return listMainElementName;
    }

    public void setListMainElementName(String listMainElementName) {
        this.listMainElementName = listMainElementName;
    }

    public String getUrlElement() {
        return urlElement;
    }

    public void setUrlElement(String urlElement) {
        this.urlElement = urlElement;
    }

    public String getTitleElement() {
        return titleElement;
    }

    public void setTitleElement(String titleElement) {
        this.titleElement = titleElement;
    }

    public String getNextListMainUrl() {
        return nextListMainUrl;
    }

    public void setNextListMainUrl(String nextListMainUrl) {
        this.nextListMainUrl = nextListMainUrl;
    }

    public boolean isUseNextListMainApi() {
        return useNextListMainApi;
    }

    public void setUseNextListMainApi(boolean useNextListMainApi) {
        this.useNextListMainApi = useNextListMainApi;
    }

    public String getNextListMainApiDataRule() {
        return nextListMainApiDataRule;
    }

    public void setNextListMainApiDataRule(String nextListMainApiDataRule) {
        this.nextListMainApiDataRule = nextListMainApiDataRule;
    }

    @Override
    public String toString() {
        return "ListMainRules{" +
                "url='" + url + '\'' +
                ", urlDataRule='" + urlDataRule + '\'' +
                ", itemIdField='" + itemIdField + '\'' +
                ", itemTitleField='" + itemTitleField + '\'' +
                ", listMainElementName='" + listMainElementName + '\'' +
                ", urlElement='" + urlElement + '\'' +
                ", titleElement='" + titleElement + '\'' +
                '}';
    }
}
