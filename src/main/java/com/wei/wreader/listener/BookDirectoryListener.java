package com.wei.wreader.listener;

import com.wei.wreader.pojo.ChapterInfo;
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
     * @param chapterInfo 章节内容
     */
    public void onClickItem(int position, List<String> chapterList, ChapterInfo chapterInfo) {

    };
}
