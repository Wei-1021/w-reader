# W-Reader 书架与阅读进度 - 设计文档

## 1. 功能概述

为 W-Reader 添加**书架**和**阅读进度**功能，使用户可以：
- 收藏多本书到书架，随时切换阅读
- 自动记录每本书的阅读进度（章节索引 + 滚动位置）
- 重新打开时一键恢复到上次阅读位置
- 查看最近阅读历史

## 2. 数据模型

### 2.1 新增 `BookshelfItem` 实体

```java
package com.wei.wreader.model;

import java.io.Serializable;
import java.time.Instant;

public class BookshelfItem implements Serializable {
    private static final long serialVersionUID = 1L;

    // === 书籍标识 ===
    /** 唯一键 = siteId + ":" + bookId，用于区分不同书源的同一本书 */
    private String uniqueKey;
    /** 书源ID */
    private String siteId;
    /** 书籍ID（来自书源） */
    private String bookId;

    // === 书籍信息 ===
    private String bookName;
    private String bookAuthor;
    private String bookDesc;
    private String bookImgUrl;
    private String bookUrl;

    // === 阅读进度 ===
    /** 当前章节索引 */
    private int chapterIndex;
    /** 当前章节标题 */
    private String chapterTitle;
    /** 章节内滚动位置（侧边栏模式） */
    private int scrollBarValue;
    /** 自动阅读最后行号（状态栏模式） */
    private int lastReadLineNum;
    /** 总章节数 */
    private int totalChapters;

    // === 时间戳 ===
    /** 加入书架时间 */
    private Instant addedAt;
    /** 最后阅读时间 */
    private Instant lastReadAt;

    // === 状态 ===
    /** 是否在书架中 */
    private boolean inShelf;
    /** 数据加载类型：1-网络，2-本地 */
    private int dataLoadType;

    /**
     * 生成唯一键
     */
    public static String buildUniqueKey(String siteId, String bookId) {
        return siteId + ":" + bookId;
    }

    // getters/setters ...
}
```

### 2.2 新增 `BookshelfService` 持久化服务

```java
package com.wei.wreader.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.wei.wreader.model.BookshelfItem;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service(Service.Level.APP)
@State(name = "BookshelfService", storages = {@Storage("w-reader-bookshelf.xml")})
public final class BookshelfService implements PersistentStateComponent<BookshelfService> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /** 书架列表（含收藏和历史） */
    private List<BookshelfItem> bookshelfItems = new ArrayList<>();

    /** 最大历史记录数 */
    private static final int MAX_HISTORY_SIZE = 50;

    public static BookshelfService getInstance() {
        return ApplicationManager.getApplication().getService(BookshelfService.class);
    }

    @Override
    public @NotNull BookshelfService getState() { return this; }

    @Override
    public void loadState(@NotNull BookshelfService state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // --- 核心操作 ---

    /** 加入书架（收藏） */
    public void addToShelf(BookshelfItem item) { ... }

    /** 从书架移除（取消收藏） */
    public void removeFromShelf(String uniqueKey) { ... }

    /** 判断是否已收藏 */
    public boolean isInShelf(String uniqueKey) { ... }

    /** 获取书架列表（仅收藏的） */
    public List<BookshelfItem> getShelfItems() { ... }

    /** 更新阅读进度 */
    public void updateReadingProgress(String uniqueKey, int chapterIndex,
                                       String chapterTitle, int scrollBarValue,
                                       int lastReadLineNum, int totalChapters) { ... }

    /** 记录/更新阅读历史（自动调用） */
    public void recordReadingHistory(BookshelfItem item) { ... }

    /** 获取最近阅读列表（按 lastReadAt 排序） */
    public List<BookshelfItem> getRecentItems(int limit) { ... }

    /** 根据 uniqueKey 获取项 */
    public BookshelfItem getByUniqueKey(String uniqueKey) { ... }
}
```

## 3. 阅读进度自动保存

### 3.1 保存时机

| 触发点 | 保存内容 | 说明 |
|---|---|---|
| 章节切换 | `chapterIndex`, `chapterTitle`, `totalChapters` | 在 `ChapterNavigator` 的章节加载回调中触发 |
| 侧边栏滚动 | `scrollBarValue` | 监听 `JBScrollPane` 的 `AdjustmentListener`，防抖保存 |
| 状态栏换行 | `lastReadLineNum` | 在 `AutoReadController` / `NextLineAction` 中触发 |
| 窗口失焦/关闭 | 全部进度 | 监听 `AppLifecycleListener` |
| 搜索打开新书 | 旧书进度 + 新书历史记录 | 在 `SearchDialog` 选择书籍时触发 |

### 3.2 进度恢复流程

```
用户点击书架中的书籍
    ↓
BookshelfService.getByUniqueKey() 获取进度
    ↓
恢复 CacheService 中的 selectedBookInfo / selectedChapterInfo
    ↓
根据 chapterIndex 加载对应章节内容
    ↓
侧边栏模式：scrollBarValue 恢复滚动位置
状态栏模式：lastReadLineNum 恢复行号
```

## 4. UI 设计

### 4.1 书架窗口

在现有侧边栏中增加 Tab 切换：

```
┌─────────────────────────────────────────┐
│  📚 书架  │  📖 阅读                    │  ← Tab 切换
├─────────────────────────────────────────┤
│  🔍 搜索书架...                         │
├─────────────────────────────────────────┤
│  ┌───────────────────────────────────┐  │
│  │ 📕 斗破苍穹          天蚕土豆     │  │
│  │    第1203章 大结局      80% ████░ │  │
│  │    🗑️ 移除  ·  3分钟前阅读       │  │
│  ├───────────────────────────────────┤  │
│  │ 📗 遮天              辰东         │  │
│  │    第856章 帝血      45% ██░░░░  │  │
│  │    🗑️ 移除  ·  昨天              │  │
│  ├───────────────────────────────────┤  │
│  │ 📘 凡人修仙传        忘语         │  │
│  │    第234章 筑基      12% █░░░░░  │  │
│  │    🗑️ 移除  ·  3天前             │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ─── 最近阅读 ─────────────────────────  │
│  📄 诡秘之主  ·  第456章  ·  1小时前    │
│  📄 一念永恒  ·  第789章  ·  2小时前    │
└─────────────────────────────────────────┘
```

### 4.2 工具栏新增按钮

在现有侧边栏工具栏中新增：

| 按钮 | 图标 | 功能 |
|---|---|---|
| 书架 | `AllIcons.Actions.MenuOpen` 或自定义 | 打开书架面板 |
| 加入书架 | `AllIcons.General.Add` 或自定义星标 | 收藏/取消收藏当前书籍 |

### 4.3 书架面板实现方案

**方案（推荐）：在侧边栏顶部增加 Tab 切换**

```
┌──────────────────────────┐
│ [📚 书架] [📖 阅读]      │  ← JBTabsPanel
├──────────────────────────┤
│  (书架列表 / 阅读内容)    │
└──────────────────────────┘
```

- 优点：不占用额外 IDE 空间，切换方便
- 实现：修改 `WReaderToolWindow.form`，在 `readerPanel` 上方添加 `JBTabsPanel`

## 5. 核心流程改造

### 5.1 搜索选书时自动记录

```
SearchDialog → 选择书籍 → 加载章节
    ↓
BookshelfService.recordReadingHistory(item)  // 自动记录历史
    ↓
如果已在书架 → BookshelfService.updateReadingProgress(...)
```

### 5.2 章节切换时更新进度

在 `ChapterNavigator` 的章节加载回调中增加：

```java
BookshelfService bookshelfService = BookshelfService.getInstance();
BookInfo bookInfo = cacheService.getSelectedBookInfo();
SiteBean siteBean = cacheService.getSelectedSiteBean();
String uniqueKey = BookshelfItem.buildUniqueKey(siteBean.getId(), bookInfo.getBookId());

bookshelfService.updateReadingProgress(
    uniqueKey,
    chapterIndex,
    chapterTitle,
    0,          // scrollBarValue - 滚动时异步更新
    0,          // lastReadLineNum - 状态栏模式异步更新
    chapterList.size()
);
```

### 5.3 从书架恢复阅读

```java
// BookshelfAction.java
public void openBookFromShelf(BookshelfItem item) {
    // 1. 恢复书源选择
    SiteBean siteBean = findSiteById(item.getSiteId());
    cacheService.setSelectedSiteBean(siteBean);

    // 2. 恢复书籍信息
    BookInfo bookInfo = new BookInfo();
    bookInfo.setBookId(item.getBookId());
    bookInfo.setBookName(item.getBookName());
    // ... 复制其他字段
    cacheService.setSelectedBookInfo(bookInfo);

    // 3. 加载目录列表
    // ... 触发目录加载

    // 4. 加载对应章节
    chapterNavigator.searchBookContentRemote(chapterUrl, param -> {
        // 5. 恢复滚动位置/行号
        if (displayType == SIDEBAR) {
            scrollPane.getVerticalScrollBar().setValue(item.getScrollBarValue());
        } else {
            currentChapterInfo.setLastReadLineNum(item.getLastReadLineNum());
        }
    });
}
```

## 6. 持久化方案

使用 IntelliJ `PersistentStateComponent` (XML) 存储，与项目现有方案一致。

使用独立的 `@State` 存储（`w-reader-bookshelf.xml`），与现有缓存数据分离，避免相互影响。

## 7. 需要新增/修改的文件清单

| 类型 | 文件 | 说明 |
|---|---|---|
| **新增** | `model/BookshelfItem.java` | 书架项实体 |
| **新增** | `service/BookshelfService.java` | 书架持久化服务 |
| **新增** | `action/BookshelfAction.java` | 打开书架 Action |
| **新增** | `action/AddToShelfAction.java` | 加入/移除书架 Action |
| **新增** | `ui/BookshelfPanel.java` | 书架面板 UI |
| **修改** | `plugin.xml` | 注册新 Service、Action |
| **修改** | `WReaderToolWindow.java` | 添加 Tab 切换或书架入口 |
| **修改** | `ChapterNavigator.java` | 章节切换时更新进度 |
| **修改** | `ReaderOrchestrator.java` | 添加书架相关方法 |
| **修改** | `SearchDialog.java` | 选书时记录历史 |
| **修改** | `WReaderToolWindow.form` | UI 布局调整 |

## 8. 实现优先级

| 阶段 | 内容 | 价值 |
|---|---|---|
| **P0** | `BookshelfItem` + `BookshelfService` + 阅读进度自动保存/恢复 | 核心：不再丢失进度 |
| **P1** | 书架 UI 面板 + 收藏/移除 + 点击恢复阅读 | 核心：多书管理 |
| **P2** | 最近阅读历史列表 | 增强：快速回到最近读的书 |
| **P3** | 书架搜索/排序、进度百分比显示 | 锦上添花 |

## 9. 注意事项

1. **uniqueKey 设计**：使用 `siteId:bookId` 组合键，因为不同书源可能有相同的 bookId，同一本书在不同书源中应视为不同条目
2. **本地文件处理**：本地加载的书籍没有 siteId，可使用 `local:` + 文件路径哈希作为 siteId
3. **数据迁移**：新增 `BookshelfService` 不影响现有 `CacheService`，无需数据迁移
4. **性能**：滚动位置保存需防抖（建议 500ms），避免频繁写入 XML
5. **并发**：`BookshelfService` 使用 `ReadWriteLock`，与 `CacheService` 保持一致