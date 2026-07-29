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
        getTemplatePresentation().setIcon(WReaderIcons.ADD_TO_SHELF);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        BookshelfService bookshelfService = BookshelfService.getInstance();
        BookInfo bookInfo = cacheService.getSelectedBookInfo();
        if (bookInfo == null || StringUtils.isBlank(bookInfo.getBookName())) {
            return;
        }

        String uniqueKey = buildCurrentBookUniqueKey();

        if (bookshelfService.isInShelf(uniqueKey)) {
            bookshelfService.removeFromShelf(uniqueKey);
        } else {
            BookshelfItem item = new BookshelfItem();
            item.setUniqueKey(uniqueKey);
            SiteBean siteBean = cacheService.getSelectedSiteBean();
            item.setSiteId(siteBean != null ? siteBean.getId() : "local");
            item.setBookId(resolveBookId(bookInfo));
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

            item.setLastReadAt(System.currentTimeMillis());
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
            String uniqueKey = buildCurrentBookUniqueKey(cs);

            if (bookshelfService.isInShelf(uniqueKey)) {
                e.getPresentation().setText("移出书架");
            } else {
                e.getPresentation().setText("加入书架");
            }
        }
    }

    private String buildCurrentBookUniqueKey() {
        return buildCurrentBookUniqueKey(CacheService.getInstance());
    }

    private String buildCurrentBookUniqueKey(CacheService cs) {
        BookInfo bookInfo = cs.getSelectedBookInfo();
        SiteBean siteBean = cs.getSelectedSiteBean();
        String siteId = siteBean != null ? siteBean.getId() : "local";
        String bookId = resolveBookId(bookInfo);
        return BookshelfItem.buildUniqueKey(siteId, bookId);
    }

    private String resolveBookId(BookInfo bookInfo) {
        if (bookInfo == null) return "";
        if (StringUtils.isNotBlank(bookInfo.getBookId())) {
            return bookInfo.getBookId();
        }
        if (StringUtils.isNotBlank(bookInfo.getBookUrl())) {
            return bookInfo.getBookUrl();
        }
        return bookInfo.getBookName();
    }
}