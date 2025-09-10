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
     * 目录列表的HTML标签元素名称
     */
    private String listMainElementName;
    /**
     * 目录链接元素cssQuery
     */
    private String urlElement;
    /**
     * 目录标题元素cssQuery
     */
    private String titleElement;

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
