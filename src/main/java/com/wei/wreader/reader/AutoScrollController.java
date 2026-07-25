package com.wei.wreader.reader;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.wei.wreader.model.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.SettingConstants;
import com.wei.wreader.util.ui.ToolWindowUtil;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoScrollController {
    private static final Logger LOG = Logger.getInstance(AutoScrollController.class);

    private static AutoScrollController instance;

    private Project project;
    private CacheService cacheService;
    private ScheduledExecutorService executorService;
    private volatile boolean isRunning = false;
    private volatile boolean isPausedByCursor = false;
    private MouseAdapter mouseAdapter;

    private AutoScrollController() {
    }

    public static synchronized AutoScrollController getInstance() {
        if (instance == null) {
            instance = new AutoScrollController();
        }
        return instance;
    }

    public void init(Project project, CacheService cacheService) {
        this.project = project;
        this.cacheService = cacheService;
    }

    public void toggleAutoScroll() {
        if (isRunning) {
            stopAutoScroll();
        } else {
            startAutoScroll();
        }
    }

    public void startAutoScroll() {
        Settings settings = cacheService.getSettings();
        if (settings == null) {
            LOG.warn("Auto-scroll: settings is null, cannot start");
            return;
        }
        if (settings.getAutoScrollSpeed() == SettingConstants.AUTO_SCROLL_SPEED_OFF) {
            LOG.info("Auto-scroll: speed is 0 (off), cannot start");
            return;
        }

        if (isRunning) return;

        LOG.info("Auto-scroll: starting with speed " + settings.getAutoScrollSpeed() + "%/s");
        isRunning = true;
        isPausedByCursor = false;

        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }

        registerMouseListener();

        Runnable scrollTask = createScrollTask();
        executorService.scheduleAtFixedRate(scrollTask, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void stopAutoScroll() {
        if (!isRunning) return;

        LOG.info("Auto-scroll: stopping");
        isRunning = false;
        isPausedByCursor = false;

        unregisterMouseListener();

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            executorService = null;
        }
    }

    public boolean isAutoScrolling() {
        return isRunning;
    }

    private Runnable createScrollTask() {
        return () -> {
            try {
                if (!isRunning) return;

                Settings settings = cacheService.getSettings();
                if (settings == null || settings.getAutoScrollSpeed() == SettingConstants.AUTO_SCROLL_SPEED_OFF) {
                    ApplicationManager.getApplication().invokeLater(this::stopAutoScroll);
                    return;
                }

                if (isPausedByCursor) {
                    return;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        if (!isRunning) return;

                        JScrollPane scrollPane = ToolWindowUtil.getContentScrollPane(project);
                        if (scrollPane == null) {
                            LOG.warn("Auto-scroll: scrollPane is null, stopping");
                            stopAutoScroll();
                            return;
                        }

                        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();

                        if (isAtBottom(verticalBar)) {
                            LOG.info("Auto-scroll: reached bottom, stopping");
                            stopAutoScroll();
                            return;
                        }

                        int visibleAmount = verticalBar.getVisibleAmount();
                        int speedPercent = settings.getAutoScrollSpeed();
                        int scrollStep = Math.max(1, (int) ((speedPercent / 100.0) * visibleAmount / 10));

                        int currentValue = verticalBar.getValue();
                        int newValue = Math.min(currentValue + scrollStep, verticalBar.getMaximum());
                        verticalBar.setValue(newValue);
                    } catch (Exception e) {
                        LOG.error("Auto-scroll EDT task error", e);
                        stopAutoScroll();
                    }
                });
            } catch (Exception e) {
                LOG.error("Auto-scroll task error", e);
                ApplicationManager.getApplication().invokeLater(this::stopAutoScroll);
            }
        };
    }

    private boolean isAtBottom(JScrollBar verticalBar) {
        int currentValue = verticalBar.getValue();
        int visibleAmount = verticalBar.getVisibleAmount();
        int maximum = verticalBar.getMaximum();
        int threshold = 5;
        return currentValue + visibleAmount >= maximum - threshold;
    }

    private void registerMouseListener() {
        JScrollPane scrollPane = ToolWindowUtil.getContentScrollPane(project);
        if (scrollPane == null) {
            LOG.warn("Auto-scroll: cannot register mouse listener, scrollPane is null");
            return;
        }

        mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isRunning) {
                    isPausedByCursor = true;
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isRunning) {
                    isPausedByCursor = false;
                }
            }
        };
        scrollPane.addMouseListener(mouseAdapter);
    }

    private void unregisterMouseListener() {
        if (mouseAdapter != null) {
            JScrollPane scrollPane = ToolWindowUtil.getContentScrollPane(project);
            if (scrollPane != null) {
                scrollPane.removeMouseListener(mouseAdapter);
            }
            mouseAdapter = null;
        }
    }

    public void shutdown() {
        stopAutoScroll();
    }
}