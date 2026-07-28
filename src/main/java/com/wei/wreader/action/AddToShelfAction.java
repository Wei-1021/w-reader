package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.wei.wreader.model.BookInfo;
import com.wei.wreader.model.BookshelfItem;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.BookshelfService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.WReaderIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class AddToShelfAction extends BaseAction {

    public AddToShelfAction() {
        super();
        getTemplatePresentation().setText("加入书架");
        getTemplatePresentation().setIcon(WReaderIcons.BOOK_INFO);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        BookshelfService bookshelfService = BookshelfService.getInstance();
        BookInfo bookInfo = cacheService.getSelectedBookInfo();
        if (bookInfo == null || StringUtils.isBlank(bookInfo.getBookName())) {
            return;
        }

        SiteBean siteBean = cacheService.getSelectedSiteBean();
        String siteId = siteBean != null ? siteBean.getId() : "local";
        String bookId = bookInfo.getBookId() != null ? bookInfo.getBookId() : bookInfo.getBookName();
        String uniqueKey = BookshelfItem.buildUniqueKey(siteId, bookId);

        if (bookshelfService.isInShelf(uniqueKey)) {
            bookshelfService.removeFromShelf(uniqueKey);
        } else {
            BookshelfItem item = new BookshelfItem();
            item.setUniqueKey(uniqueKey);
            item.setSiteId(siteId);
            item.setBookId(bookId);
            item.copyFromBookInfo(bookInfo);
            item.setDataLoadType(settings.getDataLoadType());

            com.wei.wreader.model.ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
            if (chapterInfo != null) {
                item.setChapterIndex(chapterInfo.getSelectedChapterIndex());
                item.setChapterTitle(chapterInfo.getChapterTitle());
                item.setLastReadLineNum(chapterInfo.getLastReadLineNum());
            }

            java.util.List<String> chapterList = cacheService.getChapterList();
            item.setTotalChapters(chapterList != null ? chapterList.size() : 0);

            item.setLastReadAt(Instant.now());
            bookshelfService.addToShelf(item);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        BookshelfService bookshelfService = BookshelfService.getInstance();
        CacheService cs = CacheService.getInstance();
        BookInfo bookInfo = cs != null ? cs.getSelectedBookInfo() : null;

        boolean hasBook = bookInfo != null && StringUtils.isNotBlank(bookInfo.getBookName());
        e.getPresentation().setEnabled(hasBook);

        if (hasBook) {
            SiteBean siteBean = cs.getSelectedSiteBean();
            String siteId = siteBean != null ? siteBean.getId() : "local";
            String bookId = bookInfo.getBookId() != null ? bookInfo.getBookId() : bookInfo.getBookName();
            String uniqueKey = BookshelfItem.buildUniqueKey(siteId, bookId);

            if (bookshelfService.isInShelf(uniqueKey)) {
                e.getPresentation().setText("移出书架");
            } else {
                e.getPresentation().setText("加入书架");
            }
        }
    }
}