package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.wei.wreader.model.BookInfo;
import com.wei.wreader.model.BookshelfItem;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.BookshelfService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.WReaderIcons;
import com.wei.wreader.util.data.ConstUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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
            int choice = Messages.showYesNoDialog(project,
                    "确定将《" + bookInfo.getBookName() + "》移出书架吗？阅读进度会保留在历史记录中。",
                    "移出书架", Messages.getQuestionIcon());
            if (choice != Messages.YES) {
                return;
            }
            bookshelfService.removeFromShelf(uniqueKey);
        } else {
            BookshelfItem item = new BookshelfItem();
            item.setUniqueKey(uniqueKey);
            SiteBean siteBean = cacheService.getSelectedSiteBean();
            String siteGroupKey = SiteRuleService.getInstance().getSelectedCustomSiteRuleKey();
            item.setSiteGroupKey(StringUtils.isNotBlank(siteGroupKey)
                    ? siteGroupKey : ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY);
            item.setSiteId(siteBean != null ? siteBean.getId() : "local");
            item.setBookId(resolveBookId(bookInfo));
            item.copyFromBookInfo(bookInfo);
            item.setDataLoadType(settings.getDataLoadType());

            ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
            if (chapterInfo != null) {
                item.setChapterIndex(chapterInfo.getSelectedChapterIndex());
                item.setChapterTitle(chapterInfo.getChapterTitle());
                item.setLastReadLineNum(chapterInfo.getLastReadLineNum());
            }

            java.util.List<String> chapterList = cacheService.getChapterList();
            item.setTotalChapters(chapterList != null ? chapterList.size() : 0);

            item.setLastReadAt(System.currentTimeMillis());
            bookshelfService.addToShelf(item);

            // 重新加入时以当前正在阅读的位置覆盖历史进度；没有章节上下文时保留旧进度。
            if (chapterInfo != null) {
                bookshelfService.updateReadingProgress(
                        uniqueKey,
                        chapterInfo.getSelectedChapterIndex(),
                        chapterInfo.getChapterTitle(),
                        cacheService.getEditorMessageVerticalScrollValue(),
                        chapterInfo.getLastReadLineNum(),
                        item.getTotalChapters()
                );
            }
        }

        updatePresentation(e, bookshelfService.isInShelf(uniqueKey));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        BookshelfService bookshelfService = BookshelfService.getInstance();
        CacheService cs = CacheService.getInstance();
        BookInfo bookInfo = cs != null ? cs.getSelectedBookInfo() : null;

        boolean hasBook = bookInfo != null && StringUtils.isNotBlank(bookInfo.getBookName());
        e.getPresentation().setEnabled(hasBook);

        boolean inShelf = hasBook && bookshelfService.isInShelf(buildCurrentBookUniqueKey(cs));
        updatePresentation(e, inShelf);
    }

    private void updatePresentation(@NotNull AnActionEvent event, boolean inShelf) {
        if (inShelf) {
            event.getPresentation().setText("移出书架");
            event.getPresentation().setIcon(WReaderIcons.IN_SHELF);
        } else {
            event.getPresentation().setText("加入书架");
            event.getPresentation().setIcon(WReaderIcons.ADD_TO_SHELF);
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
        String siteGroupKey = SiteRuleService.getInstance().getSelectedCustomSiteRuleKey();
        if (StringUtils.isBlank(siteGroupKey)) {
            siteGroupKey = ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY;
        }
        return BookshelfItem.buildUniqueKey(siteGroupKey, siteId, bookId);
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
