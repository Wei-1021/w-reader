package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.StringUtil;
import com.wei.wreader.widget.WReaderStatusBarWidget;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * 状态栏工厂
 * @author weizhanjie
 */
public class WReaderStatusBarFactory implements StatusBarWidgetFactory {
    private CacheService cacheService;
    private Settings settings;
    private ConfigYaml configYaml;

    private String WIDGET_ID;
    private String DISPLAY_NAME;

    @Override
    public @NotNull @NonNls String getId() {
        WIDGET_ID = ConstUtil.WREADER_STATUS_BAR_ID;
        return WIDGET_ID;
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        DISPLAY_NAME = ConstUtil.WREADER_STATUS_BAR_ID;
        return DISPLAY_NAME;
    }

    /**
     * Creates a widget to be added to the status bar.
     * <p>
     * Once the method is invoked on project initialization, the widget won't be recreated or disposed implicitly.
     * <p>
     * You may need to recreate it if:
     * <ul>
     * <li>its availability has changed. See {@link #isAvailable(Project)}</li>
     * <li>its visibility has changed. See {@link com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetSettings}</li>
     * </ul>
     * <p>
     * To do this, you need to explicitly invoke
     * {@link com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager#updateWidget(StatusBarWidgetFactory)}
     * to recreate the widget and re-add it to the status bar.
     */
    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();

        return new WReaderStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }

    /**
     * Returns availability of the widget.
     * <p>
     * {@code false} means that the IDE won't try to create a widget,
     * or will dispose it on {@link com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager#updateWidget} call.
     * E.g., {@code false} can be returned for:
     * <ul>
     * <li>the "Notifications" widget if the event log is shown as a tool window</li>
     * <li>the "Memory Indicator" widget if it is disabled in the appearance settings</li>
     * <li>the "Git" widget if there are no git repositories in a project</li>
     * </ul>
     * <p>
     * Whenever availability is changed,
     * you need to call {@link com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager#updateWidget(StatusBarWidgetFactory)}
     * explicitly to get the status bar updated.
     */
    @Override
    public boolean isAvailable(@NotNull Project project) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();

        return settings != null && settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
    }

    /**
     * Returns whether the widget can be enabled on the given status bar right now.
     * Status bar's context menu with enable/disable action depends on the result of this method.
     * <p>
     * It's better to have this method aligned with {@link com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup.WidgetState#HIDDEN} -
     * whenever the state is {@code HIDDEN}, this method should return {@code false}.
     * Otherwise, enabling the widget via the context menu will not have any visual effect.
     * <p>
     * E.g., {@link com.intellij.openapi.wm.impl.status.EditorBasedWidget editor-based widgets} are available if an editor is opened
     * in a frame that the given status bar is attached to.
     * For creating editor-based widgets, see also {@link com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory}
     */
    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        return settings != null && settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
    }

    /**
     * 是否启用底部状态栏
     * @param project
     * @param isStartupApp 是否是启动项目
     */
    public void setEnabled(@NotNull Project project, boolean isStartupApp) {
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        configYaml = ConfigYaml.getInstance();
        if (settings == null) {
            settings = configYaml.getSettings();
        }

        if (!isStartupApp) {
            boolean isExistStatusBarWidget = false;

            // 获取状态栏实例
            WindowManager windowManager = WindowManager.getInstance();
            StatusBar statusBar = windowManager.getStatusBar(project);
            if (statusBar != null) {
                // 获取状态栏组件
                StatusBarWidget wReaderStatusBarWidget = statusBar.getWidget(WReaderStatusBarWidget.getWidgetId());
                if (wReaderStatusBarWidget != null) {
                    isExistStatusBarWidget = true;
                }
            }

            // 状态栏组件不存在，则创建，反正则不创建，防止重复创建出现视图重叠
            if (!isExistStatusBarWidget) {
                StatusBarWidgetsManager statusBarWidgetsManager = project.getService(StatusBarWidgetsManager.class);
                statusBarWidgetsManager.updateWidget(this);
            }

            boolean isVisible = settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
            if (isVisible) {
                // 初始化章节内容缓存信息，避免修改设置时无法第一时间生效
                ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
                if (selectedChapterInfo != null) {
                    String chapterContentStr = selectedChapterInfo.getChapterContentStr();
                    int singleLineChars = settings.getSingleLineChars();
                    List<String> contentList = StringUtil.splitStringByMaxCharList(chapterContentStr, singleLineChars);
                    selectedChapterInfo.setChapterContentList(contentList);
                }
            }

            WReaderStatusBarWidget.update(project, "");
        }
    }

//    /**
//     * 是否启用底部状态栏
//     * @param project
//     * @param isStartupApp 是否是启动项目
//     */
//    public void setEnabled2(@NotNull Project project, boolean isStartupApp) {
//        cacheService = CacheService.getInstance();
//        settings = cacheService.getSettings();
//        configYaml = ConfigYaml.getInstance();
//        if (settings == null) {
//            settings = configYaml.getSettings();
//        }
//
//        // 获取状态栏实例
//        WindowManager windowManager = WindowManager.getInstance();
//        StatusBar statusBar = windowManager.getStatusBar(project);
//        if (statusBar != null) {
//            boolean isVisible = settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
//            // 获取状态栏组件
//            StatusBarWidget wReaderStatusBarWidget = statusBar.getWidget(WReaderStatusBarWidget.getWidgetId());
//
//            // 当显示类型为底部状态栏时，添加状态栏组件，反之移除组件
//            if (isVisible) {
//                if (wReaderStatusBarWidget == null && !isStartupApp) {
//                    statusBar.addWidget(new WReaderStatusBarWidget(project));
//                }
//
//                // 初始化章节内容缓存信息，避免修改设置时无法第一时间生效
//                ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
//                if (selectedChapterInfo != null) {
//                    String chapterContentStr = selectedChapterInfo.getChapterContentStr();
//                    int singleLineChars = settings.getSingleLineChars();
//                    List<String> contentList = StringUtil.splitStringByMaxCharList(chapterContentStr, singleLineChars);
//                    selectedChapterInfo.setChapterContentList(contentList);
//                }
//            } else {
//                statusBar.removeWidget(WReaderStatusBarWidget.getWidgetId());
//            }
//        }
//    }

}
