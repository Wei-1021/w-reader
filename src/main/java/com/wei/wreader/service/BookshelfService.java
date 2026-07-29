package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.wei.wreader.model.BookshelfItem;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service(Service.Level.APP)
@State(name = "BookshelfService", storages = {@Storage("w-reader-bookshelf.xml")})
public final class BookshelfService implements PersistentStateComponent<BookshelfService> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private List<BookshelfItem> bookshelfItems = new ArrayList<>();

    private static final int MAX_HISTORY_SIZE = 50;

    public static BookshelfService getInstance() {
        return ApplicationManager.getApplication().getService(BookshelfService.class);
    }

    public BookshelfService() {
    }

    @Override
    public @NotNull BookshelfService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull BookshelfService state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public List<BookshelfItem> getBookshelfItems() {
        lock.readLock().lock();
        try {
            return bookshelfItems;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBookshelfItems(List<BookshelfItem> bookshelfItems) {
        lock.writeLock().lock();
        try {
            this.bookshelfItems = bookshelfItems != null ? bookshelfItems : new ArrayList<>();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addToShelf(BookshelfItem item) {
        lock.writeLock().lock();
        try {
            BookshelfItem existing = findByUniqueKey(item.getUniqueKey());
            if (existing != null) {
                existing.setInShelf(true);
                existing.setAddedAt(System.currentTimeMillis());
            } else {
                item.setInShelf(true);
                item.setAddedAt(System.currentTimeMillis());
                if (item.getLastReadAt() == null) {
                    item.setLastReadAt(System.currentTimeMillis());
                }
                bookshelfItems.add(item);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeFromShelf(String uniqueKey) {
        lock.writeLock().lock();
        try {
            BookshelfItem existing = findByUniqueKey(uniqueKey);
            if (existing != null) {
                existing.setInShelf(false);
                existing.setAddedAt(null);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isInShelf(String uniqueKey) {
        lock.readLock().lock();
        try {
            BookshelfItem existing = findByUniqueKey(uniqueKey);
            return existing != null && existing.isInShelf();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<BookshelfItem> getShelfItems() {
        lock.readLock().lock();
        try {
            return bookshelfItems.stream()
                    .filter(BookshelfItem::isInShelf)
                    .sorted(Comparator.comparing(BookshelfItem::getAddedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateReadingProgress(String uniqueKey, int chapterIndex,
                                      String chapterTitle, int scrollBarValue,
                                      int lastReadLineNum, int totalChapters) {
        lock.writeLock().lock();
        try {
            BookshelfItem existing = findByUniqueKey(uniqueKey);
            if (existing != null) {
                existing.setChapterIndex(chapterIndex);
                existing.setChapterTitle(chapterTitle);
                existing.setScrollBarValue(scrollBarValue);
                existing.setLastReadLineNum(lastReadLineNum);
                existing.setTotalChapters(totalChapters);
                existing.setLastReadAt(System.currentTimeMillis());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void recordReadingHistory(BookshelfItem item) {
        lock.writeLock().lock();
        try {
            BookshelfItem existing = findByUniqueKey(item.getUniqueKey());
            if (existing != null) {
                existing.setBookName(item.getBookName());
                existing.setBookAuthor(item.getBookAuthor());
                existing.setBookDesc(item.getBookDesc());
                existing.setBookImgUrl(item.getBookImgUrl());
                existing.setBookUrl(item.getBookUrl());
                existing.setChapterIndex(item.getChapterIndex());
                existing.setChapterTitle(item.getChapterTitle());
                existing.setTotalChapters(item.getTotalChapters());
                existing.setScrollBarValue(item.getScrollBarValue());
                existing.setLastReadLineNum(item.getLastReadLineNum());
                existing.setDataLoadType(item.getDataLoadType());
                existing.setLastReadAt(System.currentTimeMillis());
            } else {
                item.setLastReadAt(System.currentTimeMillis());
                bookshelfItems.add(item);
                trimHistory();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<BookshelfItem> getRecentItems(int limit) {
        lock.readLock().lock();
        try {
            return bookshelfItems.stream()
                    .filter(item -> item.getLastReadAt() != null)
                    .sorted(Comparator.comparing(BookshelfItem::getLastReadAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public BookshelfItem getByUniqueKey(String uniqueKey) {
        lock.readLock().lock();
        try {
            return findByUniqueKey(uniqueKey);
        } finally {
            lock.readLock().unlock();
        }
    }

    private BookshelfItem findByUniqueKey(String uniqueKey) {
        if (uniqueKey == null) return null;
        for (BookshelfItem item : bookshelfItems) {
            if (uniqueKey.equals(item.getUniqueKey())) {
                return item;
            }
        }
        return null;
    }

    private void trimHistory() {
        List<BookshelfItem> nonShelfItems = bookshelfItems.stream()
                .filter(item -> !item.isInShelf())
                .sorted(Comparator.comparing(BookshelfItem::getLastReadAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        int excess = nonShelfItems.size() - MAX_HISTORY_SIZE;
        if (excess > 0) {
            for (int i = 0; i < excess; i++) {
                bookshelfItems.remove(nonShelfItems.get(i));
            }
        }
    }
}