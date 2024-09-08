package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.wei.wreader.pojo.BookInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 已选择的小说基本信息实体数据持久化服务
 * @author weizhanjie
 */
@Service(Service.Level.APP)
@State(name = "SelectBookInfoService", storages = {@Storage("select-book-info.xml")})
public final class SelectBookInfoService implements PersistentStateComponent<SelectBookInfoService> {

    private String bookName;
    private String bookUrl;
    private String bookAuthor;
    private String bookDesc;
    private String bookImgUrl;

    private static SelectBookInfoService instance;

    public static SelectBookInfoService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(SelectBookInfoService.class);
        }
        return instance;
    }

    @Nullable
    @Override
    public SelectBookInfoService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SelectBookInfoService state) {
        System.out.println("loadState" + state);
        XmlSerializerUtil.copyBean(state, this);
    }

    public BookInfo getBookInfo() {
        BookInfo bookInfo = new BookInfo();
        bookInfo.setBookAuthor(bookAuthor);
        bookInfo.setBookDesc(bookDesc);
        bookInfo.setBookImgUrl(bookImgUrl);
        bookInfo.setBookName(bookName);
        bookInfo.setBookUrl(bookUrl);
        return bookInfo;
    }

    public void setBookInfo(BookInfo bookInfo) {
        bookAuthor = bookInfo.getBookAuthor();
        bookDesc = bookInfo.getBookDesc();
        bookImgUrl = bookInfo.getBookImgUrl();
        bookName = bookInfo.getBookName();
        bookUrl = bookInfo.getBookUrl();
    }
}
