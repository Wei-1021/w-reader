package com.wei.wreader.widget;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.wei.wreader.pojo.*;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;

/**
 * 状态栏视图
 * @author weizhanjie
 */
public class WReaderStatusBarWidget extends EditorBasedStatusBarPopup {
    private static final Logger log = LoggerFactory.getLogger(WReaderStatusBarWidget.class);
    private final Project project;
    private static String WIDGET_ID;
    private ConfigYaml configYaml;
    private ToolWindowInfo toolWindow;
    private CacheService cacheService;
    private BookInfo selectedBookInfo;
    private ChapterInfo selectedChapterInfo;
    private Settings settings;
    private String[] contentArr;

    public WReaderStatusBarWidget(@NotNull Project project) {
        super(project, false);
        this.project = project;
        SwingUtilities.invokeLater(() -> {
            configYaml = ConfigYaml.getInstance();
            cacheService = CacheService.getInstance();

            selectedBookInfo = cacheService.getSelectedBookInfo();
            selectedChapterInfo = cacheService.getSelectedChapterInfo();
            settings = cacheService.getSettings();
            if (settings == null) {
                settings = configYaml.getSettings();
            }
            WIDGET_ID = configYaml.getNameHump() + "StatusBarWidget";
            toolWindow = configYaml.getToolWindow();
        });
    }

    public String getTooltipText() {
        if (selectedBookInfo == null || selectedChapterInfo == null) {
            return configYaml.getNameHump();
        }

        return selectedBookInfo.getBookName() + "|" + selectedChapterInfo.getChapterTitle();
    }

    /**
     * 创建实例
     * @param project
     * @return
     */
    @NotNull
    @Override
    protected StatusBarWidget createInstance(@NotNull Project project) {
        return new WReaderStatusBarWidget(project);
    }

    /**
     * 获取状态栏显示文本
     * @param virtualFile
     * @return
     */
    @NotNull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile virtualFile) {
        String chapterContentStr = selectedChapterInfo.getChapterContentStr();
        int singleLineChars = settings.getSingleLineChars();
        // 按照单行最大字数将字符串分割成数组
        long st = System.currentTimeMillis();
        contentArr = StringUtil.splitStringByMaxChars(chapterContentStr, singleLineChars);
        long et = System.currentTimeMillis();
        System.out.println("split time:" + (et - st));
        int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();
        String thisContentLineStr = "";
        if (contentArr != null && contentArr.length > 0 && lastReadLineNum < contentArr.length) {
            lastReadLineNum = lastReadLineNum <= 0 ? 1 : lastReadLineNum;
            thisContentLineStr = contentArr[lastReadLineNum - 1];

            if (settings.isShowLineNum()) {
                thisContentLineStr = lastReadLineNum + "|" + thisContentLineStr;
            }
        }

        String tooltipText = getTooltipText();

        EditorBasedStatusBarPopup.WidgetState widgetState = new EditorBasedStatusBarPopup
                .WidgetState(tooltipText, thisContentLineStr, true);
        Icon icon = IconLoader.getIcon("/icon/mainIcon.svg", WReaderStatusBarWidget.class);
        widgetState.setIcon(icon);
        return widgetState;
    }

    /**
     * 创建弹出菜单
     * @param dataContext
     * @return
     */
    @Nullable
    @Override
    protected ListPopup createPopup(@NotNull DataContext dataContext) {
        ComponentIdKey componentIdKey = configYaml.getComponentIdKey();

        ActionManager instance = ActionManager.getInstance();
        AnAction settingAction = instance.getAction(componentIdKey.getSetting());
        AnAction searchBookNameAction = instance.getAction(componentIdKey.getSearchBook());
        AnAction chapterListAction = instance.getAction(componentIdKey.getBookDirectory());
        AnAction prevChapterAction = instance.getAction(componentIdKey.getPrevChapter());
        AnAction nextChapterAction = instance.getAction(componentIdKey.getNextChapter());
        List<AnAction> actionList = List.of(settingAction, searchBookNameAction, chapterListAction,
                prevChapterAction, nextChapterAction);
        DefaultActionGroup actionGroup = new DefaultActionGroup(actionList);
        return JBPopupFactory
                .getInstance()
                .createActionGroupPopup(configYaml.getName(), actionGroup, dataContext,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
    }

    @NotNull
    @Override
    public String ID() {
        WIDGET_ID = ConstUtil.WREADER_ID + "StatusBarWidget";
        return WIDGET_ID;
    }

    @Nullable
    private static StatusBarWidget findWidget(@NotNull Project project) {
        StatusBar bar = WindowManager.getInstance().getStatusBar(project);
        if (bar != null) {
            return bar.getWidget(WIDGET_ID);
        }
        return null;
    }

    @Override
    public void update(@Nullable Runnable finishUpdate) {
        super.update(finishUpdate);
    }

}
