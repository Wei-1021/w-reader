package com.wei.wreader.widget;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
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
import com.wei.wreader.utils.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;

/**
 * 状态栏视图
 *
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
    private List<String> contentArr;
    public static String currentContentStr;
    public static String showContentStr;
    /**
     * 是否隐藏文字
     */
    private boolean isHideText;

    public WReaderStatusBarWidget(@NotNull Project project) {
        super(project, false);
        this.project = project;
        initData();
    }

    private void initData() {
        configYaml = ConfigYaml.getInstance();
        cacheService = CacheService.getInstance();

        selectedBookInfo = cacheService.getSelectedBookInfo();
        selectedChapterInfo = cacheService.getSelectedChapterInfo();
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }
        WIDGET_ID = ConstUtil.WREADER_STATUS_BAR_WIDGET_ID;
        toolWindow = configYaml.getToolWindow();

        isHideText = cacheService.isHideText();
    }

    public String getTooltipText() {
        if (selectedBookInfo == null || selectedChapterInfo == null) {
            return configYaml.getNameHump();
        }

        return selectedBookInfo.getBookName() + "|" + selectedChapterInfo.getChapterTitle();
    }

    /**
     * 创建实例
     *
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
     *
     * @param virtualFile
     * @return
     */
    @NotNull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile virtualFile) {
        initData();

        boolean isVisible = settings.getDisplayType() == Settings.DISPLAY_TYPE_STATUSBAR;
        if (!isVisible) {
            // 隐藏状态栏
            return WidgetState.HIDDEN;
        }

        if (!isHideText) {
            if (selectedChapterInfo != null) {
                String chapterContentStr = selectedChapterInfo.getChapterContentStr();
                contentArr = selectedChapterInfo.getChapterContentList();
                int singleLineChars = settings.getSingleLineChars();
                int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();

                // 当contentArr为空时, 按照单行最大字数将字符串分割成数组
                if (contentArr == null || contentArr.isEmpty()) {
                    contentArr = StringUtil.splitStringByMaxCharList(chapterContentStr, singleLineChars);
                }
                selectedChapterInfo.setChapterContentList(contentArr);
                if (contentArr != null && !contentArr.isEmpty() && lastReadLineNum < contentArr.size()) {
                    lastReadLineNum = lastReadLineNum <= 0 ? 1 : lastReadLineNum;
                    currentContentStr = contentArr.get(lastReadLineNum - 1);

                    if (settings.isShowLineNum()) {
                        currentContentStr = lastReadLineNum + "/" + contentArr.size() + "|" + currentContentStr;
                    }
                }

                showContentStr = currentContentStr;
            } else {
                showContentStr = "";
            }
        }

        String tooltipText = getTooltipText();

        EditorBasedStatusBarPopup.WidgetState widgetState = new EditorBasedStatusBarPopup
                .WidgetState(tooltipText, showContentStr, true);
        Icon icon = IconLoader.getIcon("/icon/mainIcon.svg", WReaderStatusBarWidget.class);
        widgetState.setIcon(icon);

        return widgetState;
    }

    /**
     * 创建弹出菜单
     *
     * @param dataContext
     * @return
     */
    @Nullable
    @Override
    protected ListPopup createPopup(@NotNull DataContext dataContext) {
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup group = (ActionGroup) actionManager.getAction(ConstUtil.WREADER_GROUP_STATUS_BAR_ID);

        return JBPopupFactory
                .getInstance()
                .createActionGroupPopup(configYaml.getName(), group, dataContext,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
    }

    @NotNull
    @Override
    public String ID() {
        WIDGET_ID = ConstUtil.WREADER_STATUS_BAR_WIDGET_ID;
        return WIDGET_ID;
    }

    @Nullable
    private static WReaderStatusBarWidget findWidget(@NotNull Project project) {
        StatusBar bar = WindowManager.getInstance().getStatusBar(project);
        if (bar != null) {
            return (WReaderStatusBarWidget) bar.getWidget(ConstUtil.WREADER_STATUS_BAR_WIDGET_ID);
        }
        return null;
    }

    public static void update(@NotNull Project project, String chapterContent) {
        WReaderStatusBarWidget widget = findWidget(project);
        if (widget != null) {
            currentContentStr = chapterContent;
            widget.update(() -> {
                if (widget.myStatusBar == null) {
                    Messages.showErrorDialog("状态栏更新异常", MessageDialogUtil.TITLE_ERROR);
                    return;
                }
                widget.myStatusBar.updateWidget(ConstUtil.WREADER_STATUS_BAR_WIDGET_ID);
            });
        }
    }

    public static String getWidgetId() {
        return ConstUtil.WREADER_STATUS_BAR_WIDGET_ID;
    }

    /**
     * 隐藏文字
     */
    public static void hideText(@NotNull Project project) {
        showContentStr = "";
        update(project, showContentStr);
    }

    /**
     * 显示文字
     */
    public static void showText(@NotNull Project project) {
        showContentStr = currentContentStr;
        update(project, showContentStr);
    }

    /**
     * 上一行
     *
     * @param project
     */
    public static void prevLine(@NotNull Project project) {
        CacheService cacheTemp = CacheService.getInstance();
        ChapterInfo selectedChapterInfoTemp = cacheTemp.getSelectedChapterInfo();
        if (selectedChapterInfoTemp == null) {
            return;
        }

        List<String> chapterContentList = selectedChapterInfoTemp.getChapterContentList();
        int lastReadLineNum = selectedChapterInfoTemp.getLastReadLineNum();
        if (chapterContentList != null &&
                !chapterContentList.isEmpty() &&
                lastReadLineNum > 1) {
            lastReadLineNum--;
            selectedChapterInfoTemp.setLastReadLineNum(lastReadLineNum);
            selectedChapterInfoTemp.setPrevReadLineNum(lastReadLineNum <= 1 ? 1 : lastReadLineNum - 1);
            selectedChapterInfoTemp.setNextReadLineNum(lastReadLineNum >= chapterContentList.size() ?
                    chapterContentList.size() : lastReadLineNum + 1);
            String chapterContent = chapterContentList.get(lastReadLineNum);
            update(project, chapterContent);
        }
    }

    /**
     * 下一行
     *
     * @param project
     */
    public static void nextLine(@NotNull Project project) {
        CacheService cacheTemp = CacheService.getInstance();
        ChapterInfo selectedChapterInfoTemp = cacheTemp.getSelectedChapterInfo();
        if (selectedChapterInfoTemp == null) {
            return;
        }

        List<String> chapterContentList = selectedChapterInfoTemp.getChapterContentList();
        int lastReadLineNum = selectedChapterInfoTemp.getLastReadLineNum();
        if (chapterContentList != null &&
                !chapterContentList.isEmpty() &&
                lastReadLineNum <= chapterContentList.size()) {
            lastReadLineNum++;
            selectedChapterInfoTemp.setLastReadLineNum(lastReadLineNum);
            selectedChapterInfoTemp.setPrevReadLineNum(lastReadLineNum <= 1 ? 1 : lastReadLineNum - 1);
            selectedChapterInfoTemp.setNextReadLineNum(lastReadLineNum >= chapterContentList.size() ?
                    chapterContentList.size() : lastReadLineNum + 1);
            String chapterContent = chapterContentList.get(lastReadLineNum);
            update(project, chapterContent);
        }
    }

    /**
     * 上一章
     */
    public static void prevChapter(@NotNull Project project) {
        CacheService cacheTemp = CacheService.getInstance();
        // 获取章节列表
        List<String> chapterListTemp  = cacheTemp.getChapterList();
        if (chapterListTemp == null || chapterListTemp.isEmpty()) {
            return;
        }

        // 获取当前章节
        ChapterInfo selectedChapterInfoTemp = cacheTemp.getSelectedChapterInfo();
        if (selectedChapterInfoTemp == null) {
            return;
        }

        // 获取当前章节索引
        int selectedChapterIndex = selectedChapterInfoTemp.getSelectedChapterIndex();
        if (selectedChapterIndex == 0) {
            return;
        }

        // 获取上一章
        OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
        operateAction.prevPageChapter();
        selectedChapterInfoTemp.setLastReadLineNum(1);
        selectedChapterInfoTemp.setPrevReadLineNum(1);
        selectedChapterInfoTemp.setNextReadLineNum(1);
        selectedChapterInfoTemp.setChapterContentList(null);
        update(project, "");
    }

    /**
     * 下一章
     */
    public static void nextChapter(@NotNull Project project) {
        CacheService cacheTemp = CacheService.getInstance();
        // 获取章节列表
        List<String> chapterListTemp  = cacheTemp.getChapterList();
        if (chapterListTemp == null || chapterListTemp.isEmpty()) {
            return;
        }

        // 获取当前章节
        ChapterInfo selectedChapterInfoTemp = cacheTemp.getSelectedChapterInfo();
        if (selectedChapterInfoTemp == null) {
            return;
        }

        // 获取当前章节索引
        int selectedChapterIndex = selectedChapterInfoTemp.getSelectedChapterIndex();
        if (selectedChapterIndex == chapterListTemp.size() - 1) {
            return;
        }
        OperateActionUtil operateAction = OperateActionUtil.getInstance(project);
        operateAction.nextPageChapter();
        selectedChapterInfoTemp.setLastReadLineNum(1);
        selectedChapterInfoTemp.setPrevReadLineNum(1);
        selectedChapterInfoTemp.setNextReadLineNum(1);
        selectedChapterInfoTemp.setChapterContentList(null);
        update(project, "");

    }

}
