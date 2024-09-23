package com.wei.wreader.listener;

import org.jsoup.nodes.Element;

import java.util.List;

/**
 * 目录列表监听器
 * @author weizhanjie
 */
public abstract class BookDirectoryListener {

    /**
     * 点击目录列表项
     * @param position 下标
     * @param chapterList 章节列表
     * @param chapterElement 章节元素
     */
    public void onClickItem(int position, List<String> chapterList, Element chapterElement) {

    };
}
