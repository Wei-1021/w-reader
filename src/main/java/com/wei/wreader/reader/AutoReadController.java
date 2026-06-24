package com.wei.wreader.reader;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.AppStateService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.widget.ReaderStatusBarWidget;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自动阅读控制器 - 管理自动阅读定时任务
 */
public class AutoReadController {
    private static final Logger LOG = Logger.getInstance(AutoReadController.class);

    private static AutoReadController instance;

    private Project project;
    private CacheService cacheService;
    private AppStateService appState;
    private ScheduledExecutorService executorService;

    private AutoReadController() {
    }

    public static synchronized AutoReadController getInstance() {
        if (instance == null) {
            instance = new AutoReadController();
        }
        return instance;
    }

    public void init(Project project, CacheService cacheService, AppStateService appState) {
        this.project = project;
        this.cacheService = cacheService;
        this.appState = appState;
    }

    /**
     * 自动阅读下一行功能
     */
    public void autoReadNextLine() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            executorService = null;
            return;
        }

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) return;

        AtomicReference<List<String>> chapterContentList =
                new AtomicReference<>(selectedChapterInfo.getChapterContentList());
        if (chapterContentList.get() == null || chapterContentList.get().isEmpty()) return;

        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }

        Settings settings = cacheService.getSettings();
        float autoReadTime = settings.getAutoReadTime();
        if (autoReadTime <= 0f) autoReadTime = 5f;

        Runnable readNextLineTask = createAutoReadTask();
        long autoReadTimeMillis = (long) (autoReadTime * 1000);

        executorService.scheduleAtFixedRate(readNextLineTask,
                autoReadTimeMillis, autoReadTimeMillis, TimeUnit.MILLISECONDS);
    }

    private Runnable createAutoReadTask() {
        return () -> {
            try {
                ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
                if (selectedChapterInfo == null) return;

                int autoReadLastReadLineNum = selectedChapterInfo.getLastReadLineNum();
                int contentLength = selectedChapterInfo.getChapterContentList() == null ?
                        0 : selectedChapterInfo.getChapterContentList().size();

                if (autoReadLastReadLineNum < contentLength) {
                    ReaderStatusBarWidget.nextLine(project);
                } else {
                    cacheService.setEditorMessageVerticalScrollValue(0);
                    ReaderStatusBarWidget.nextChapter(project);
                }
            } catch (Exception e) {
                LOG.error("Auto-read task error", e);
            }
        };
    }

    /**
     * 停止自动阅读
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            executorService = null;
        }
    }
}
