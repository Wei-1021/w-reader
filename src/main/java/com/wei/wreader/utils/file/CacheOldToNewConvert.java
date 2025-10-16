package com.wei.wreader.utils.file;

import com.wei.wreader.pojo.*;
import com.wei.wreader.service.CacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 缓存数据转换，旧版本缓存数据转换成新版本缓存数据
 * @author weizhanjie
 */
public class CacheOldToNewConvert {
    private static CacheService cacheService;
    public CacheOldToNewConvert() {
        cacheService = CacheService.getInstance();
    }

    /**
     * 书源站点信息缓存数据转换
     */
    public void convertBookSiteInfo() {
        BookSiteInfo selectedBookSiteInfo = cacheService.getSelectedBookSiteInfo();
        SiteBean selectedSiteBean = cacheService.getSelectedSiteBean();
        selectedSiteBean = convertBookSiteInfo(selectedBookSiteInfo, selectedSiteBean);
        cacheService.setSelectedSiteBean(selectedSiteBean);

        BookSiteInfo tempSelectedBookSiteInfo = cacheService.getTempSelectedBookSiteInfo();
        SiteBean tempSelectedSiteBean = cacheService.getTempSelectedSiteBean();
        tempSelectedSiteBean = convertBookSiteInfo(tempSelectedBookSiteInfo, tempSelectedSiteBean);
        cacheService.setTempSelectedSiteBean(tempSelectedSiteBean);
    }

    /**
     * 书源站点信息数据转换
     */
    public SiteBean convertBookSiteInfo(BookSiteInfo selectedBookSiteInfo, SiteBean selectedSiteBean) {
        if (selectedBookSiteInfo != null && (selectedSiteBean == null || StringUtils.isBlank(selectedSiteBean.getId()))) {
            // 搜索规则
            SearchRules searchRules = new SearchRules();
            searchRules.setUrl(selectedBookSiteInfo.getSearchUrl());
            searchRules.setDataBookListRule(selectedBookSiteInfo.getSearchDataBookListRule());
            searchRules.setBookListElementName(selectedBookSiteInfo.getBookListElementName());
            searchRules.setBookListUrlElement(selectedBookSiteInfo.getBookListUrlElement());
            searchRules.setBookListTitleElement(selectedBookSiteInfo.getBookListTitleElement());
            // 目录规则
            ListMainRules listMainRules = new ListMainRules();
            listMainRules.setUrl(selectedBookSiteInfo.getListMainUrl());
            listMainRules.setUrlDataRule(selectedBookSiteInfo.getListMainUrlDataRule());
            listMainRules.setItemIdField(selectedBookSiteInfo.getListMainItemIdField());
            listMainRules.setItemTitleField(selectedBookSiteInfo.getListMainItemTitleField());
            listMainRules.setListMainElementName(selectedBookSiteInfo.getListMainElementName());
            listMainRules.setUrlElement(selectedBookSiteInfo.getChapterListUrlElement());
            listMainRules.setTitleElement(selectedBookSiteInfo.getChapterListTitleElement());
            // 章节内容规则
            ChapterRules chapterRules = new ChapterRules();
            chapterRules.setUrl(selectedBookSiteInfo.getChapterContentUrl());
            chapterRules.setUrlDataRule(selectedBookSiteInfo.getChapterContentUrlDataRule());
            chapterRules.setContentHandleRule(selectedBookSiteInfo.getChapterContentHandleRule());
            chapterRules.setUseContentOriginalStyle(selectedBookSiteInfo.isContentOriginalStyle());
            chapterRules.setReplaceContentOriginalRegex(selectedBookSiteInfo.getReplaceContentOriginalRegex());
            chapterRules.setContentElementName(selectedBookSiteInfo.getChapterContentElementName());
            chapterRules.setNextContentUrl("");
            if (StringUtils.isNotBlank(selectedBookSiteInfo.getChapterContentRegex())) {
                chapterRules.setContentRegexList(List.of(selectedBookSiteInfo.getChapterContentRegex()));
            }
            // 书籍信息规则
            BookInfoRules bookInfoRules = new BookInfoRules();
            bookInfoRules.setBookIdField(selectedBookSiteInfo.getBookIdField());
            bookInfoRules.setBookNameField(selectedBookSiteInfo.getBookNameField());
            bookInfoRules.setBookUrlField(selectedBookSiteInfo.getBookUrlField());
            bookInfoRules.setBookAuthorField(selectedBookSiteInfo.getBookAuthorField());
            bookInfoRules.setBookDescField(selectedBookSiteInfo.getBookDescField());
            bookInfoRules.setBookImgUrlField(selectedBookSiteInfo.getBookImgUrlField());
            // 站点信息
            selectedSiteBean = new SiteBean();
            selectedSiteBean.setEnabled(selectedBookSiteInfo.getEnabled());
            selectedSiteBean.setId(selectedBookSiteInfo.getId());
            selectedSiteBean.setName(selectedBookSiteInfo.getName());
            selectedSiteBean.setBaseUrl(selectedBookSiteInfo.getBaseUrl());
            selectedSiteBean.setHeader(selectedBookSiteInfo.getHeader());
            selectedSiteBean.setSearchRules(searchRules);
            selectedSiteBean.setListMainRules(listMainRules);
            selectedSiteBean.setChapterRules(chapterRules);
            selectedSiteBean.setBookInfoRules(bookInfoRules);
            selectedSiteBean.setHasHtml(selectedBookSiteInfo.isHtml());
        }

        return selectedSiteBean;
    }


}
