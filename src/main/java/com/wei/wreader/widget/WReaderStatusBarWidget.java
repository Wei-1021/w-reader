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
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.wei.wreader.action.SearchBookNameAction;
import com.wei.wreader.action.SelectReadSiteAction;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.pojo.ToolWindowInfo;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.utils.ConstUtil;
import com.wei.wreader.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class WReaderStatusBarWidget extends EditorBasedStatusBarPopup {
    private final Project project;
    private String WIDGET_ID;
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
        contentArr = StringUtil.splitStringByMaxChars(chapterContentStr, singleLineChars);
        int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();
        String thisContentLineStr = "";
        if (contentArr != null && contentArr.length > 0 && lastReadLineNum < contentArr.length) {
            lastReadLineNum = lastReadLineNum <= 0 ? 1 : lastReadLineNum;
            thisContentLineStr = contentArr[lastReadLineNum - 1];
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
        ActionManager instance = ActionManager.getInstance();
//        AnAction searchAction = instance.getAction(toolWindow.getSearchTitle());
//        AnAction chapterListAction = instance.getAction(toolWindow.getChapterListTitle());
//        AnAction prevChapterAction = instance.getAction(toolWindow.getPrevChapterTitle());
//        AnAction nextChapterAction = instance.getAction(toolWindow.getNextChapterTitle());
        AnAction selectReadSiteAction = instance.getAction("SelectReadSite");
        AnAction searchBookNameAction = instance.getAction("SearchBookName");
        List<AnAction> actionList = List.of(selectReadSiteAction, searchBookNameAction);
        DefaultActionGroup actionGroup = new DefaultActionGroup(actionList);
        return JBPopupFactory
                .getInstance()
                .createActionGroupPopup(configYaml.getName(), actionGroup, dataContext,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
    }

    @NotNull
    @Override
    public String ID() {
        return ConstUtil.WREADER_ID + "StatusBarWidget";
    }
}
