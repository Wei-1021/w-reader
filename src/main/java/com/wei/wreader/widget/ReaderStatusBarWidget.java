package com.wei.wreader.widget;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.model.BookInfo;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.Settings;
import com.wei.wreader.reader.ReaderOrchestrator;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.WReaderIcons;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.StringUtil;
import com.wei.wreader.util.yml.ConfigYaml;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class ReaderStatusBarWidget implements CustomStatusBarWidget {
    public static final String ID = ConstUtil.WREADER_STATUS_BAR_WIDGET_ID;

    private final Project project;
    private StatusBar statusBar;
    private final JLabel label;

    private CacheService cacheService;
    private ConfigYaml configYaml;
    private BookInfo selectedBookInfo;
    private Settings settings;

    private List<String> contentArr;
    private String currentContentStr = "";
    private String showContentStr = "";
    private boolean isHideText;

    public ReaderStatusBarWidget(Project project) {
        this.project = project;
        initData();

        label = new JBLabel();
        label.setText(showContentStr);
        label.setBorder(JBUI.Borders.empty(0, 6));
        Icon mainIcon = WReaderIcons.getMainIcon(project);
        label.setIcon(mainIcon);

        label.setOpaque(false);
        applyFont();

        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent event, int clickCount) {
                showPopup();
                return true;
            }
        }.installOn(label);

        // 监听主题颜色变化
        appColorsListener();
    }

    private void initData() {
        configYaml = ConfigYaml.getInstance();
        cacheService = CacheService.getInstance();

        selectedBookInfo = cacheService.getSelectedBookInfo();
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }

        isHideText = cacheService.isHideText();
        refreshText();
    }

    /**
     * 根据当前阅读状态刷新显示文本
     */
    private void refreshText() {
        if (isHideText) {
            showContentStr = "";
            return;
        }

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            showContentStr = "";
            return;
        }

        String chapterContentStr = selectedChapterInfo.getChapterContentStr();
        contentArr = selectedChapterInfo.getChapterContentList();
        int singleLineChars = settings.getSingleLineChars();
        int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();

        if (contentArr == null || contentArr.isEmpty()) {
            contentArr = StringUtil.splitStringByMaxCharList(chapterContentStr, singleLineChars);
            selectedChapterInfo.setChapterContentList(contentArr);
            cacheService.setSelectedChapterInfo(selectedChapterInfo);
        }

        if (contentArr != null && !contentArr.isEmpty() && lastReadLineNum > 0 && lastReadLineNum <= contentArr.size()) {
            currentContentStr = contentArr.get(lastReadLineNum - 1);

            if (settings.isShowLineNum()) {
                currentContentStr = lastReadLineNum + "/" + contentArr.size() + "|" + currentContentStr;
            }
        } else {
            currentContentStr = "";
        }

        showContentStr = currentContentStr;
    }

    private void showPopup() {
        ActionGroup group = (ActionGroup) ActionManager.getInstance()
                .getAction(ConstUtil.WREADER_GROUP_STATUS_BAR_ID);

        if (group == null) {
            return;
        }

        configYaml = ConfigYaml.getInstance();
        ListPopup popup = JBPopupFactory.getInstance()
                .createActionGroupPopup(
                        configYaml.getName(),
                        group,
                        DataManager.getInstance().getDataContext(label),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true
                );

        Dimension size = popup.getContent().getPreferredSize();
        popup.show(new RelativePoint(label, new Point(0, -size.height)));
    }

    @Override
    public @NotNull String ID() {
        return ID;
    }

    @Override
    public JComponent getComponent() {
        return label;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    @Override
    public void dispose() {
//        statusBar = null;
//        cacheService = null;
//        configYaml = null;
//        selectedBookInfo = null;
//        settings = null;
//        contentArr = null;
    }

    /**
     * 设置状态栏组件可见性
     */
    public void setVisible(boolean visible) {
        label.setVisible(visible);
        refresh();
    }

    public void setText() {
        setText(showContentStr);
    }

    public void setText(String text) {
        label.setText(text);
        refresh();
    }

    public void setTooltip(String tooltip) {
        label.setToolTipText(tooltip);
        refresh();
    }

    public void setForeground(Color color) {
        label.setForeground(color);
        refresh();
    }

    public void setIcon(Icon icon) {
        label.setIcon(icon);
        refresh();
    }

    private void refresh() {
        if (statusBar != null) {
            statusBar.updateWidget(ID);
        }
    }

    /**
     * 从缓存读取字体大小、颜色和加粗设置并应用到 label
     */
    private void applyFont() {
        int fontSize = cacheService.getFontSize();
        if (fontSize <= 0) {
            fontSize = (int) ConstUtil.DEFAULT_FONT_SIZE;
        }
        int fontStyle = cacheService.isFontBold() ? Font.BOLD : Font.PLAIN;
        JBFont labelFont = JBFont.label().deriveFont(fontStyle, (float) fontSize);
        label.setFont(labelFont);

        String fontColorHex = cacheService.getFontColorHex();
        if (fontColorHex != null && !fontColorHex.isEmpty()) {
            try {
                label.setForeground(Color.decode(fontColorHex));
            } catch (NumberFormatException ignored) {
                defaultFontColor();
            }
        } else {
            defaultFontColor();
        }
    }

    /**
     * 更新状态栏字体（字体大小或颜色变化后调用）
     */
    public static void updateFont(@NotNull Project project) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget != null) {
            widget.applyFont();
            widget.refresh();
        }
    }

    // ==================== 静态查找与操作 ====================

    public static ReaderStatusBarWidget findWidget(@NotNull Project project) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar == null) {
            return null;
        }
        return (ReaderStatusBarWidget) statusBar.getWidget(ID);
    }

    /**
     * 更新状态栏显示文本
     */
    public static void update(@NotNull Project project) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget != null) {
            widget.refreshText();
            widget.setText();
            widget.setTooltip(widget.getTooltipText());
        }
    }

    /**
     * 更新状态栏显示文本
     */
    public static void update(@NotNull Project project, String text) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget != null) {
            widget.setText(text);
        }
    }

    /**
     * 更新 Tooltip
     */
    public static void updateTooltip(@NotNull Project project, String tooltip) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget != null) {
            widget.setTooltip(tooltip);
        }
    }

    /**
     * 清空状态栏文本
     */
    public static void hide(@NotNull Project project) {
        update(project, "");
    }

    /**
     * 隐藏文字（保留图标）
     */
    public static void hideText(@NotNull Project project) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget != null) {
            widget.isHideText = true;
            widget.showContentStr = "";
            widget.setText("");
        }
    }

    /**
     * 显示文字
     */
    public static void showText(@NotNull Project project) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget != null) {
            widget.isHideText = false;
            widget.refreshText();
            widget.setTooltip(widget.getTooltipText());
            widget.setText(widget.showContentStr);
        }
    }

    /**
     * 上一行
     */
    public static void prevLine(@NotNull Project project) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget == null) return;

        CacheService cacheService = CacheService.getInstance();
        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) return;

        List<String> chapterContentList = selectedChapterInfo.getChapterContentList();
        int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();

        if (chapterContentList == null || chapterContentList.isEmpty() || lastReadLineNum <= 1) {
            return;
        }

        int newLineNum = lastReadLineNum - 1;
        updateChapterLineNum(cacheService, selectedChapterInfo, newLineNum, lastReadLineNum);

        widget.refreshText();
        widget.setText();
        widget.setTooltip(widget.getTooltipText());
    }

    /**
     * 下一行
     */
    public static void nextLine(@NotNull Project project) {
        ReaderStatusBarWidget widget = findWidget(project);
        if (widget == null) return;

        CacheService cacheService = CacheService.getInstance();
        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) return;

        List<String> chapterContentList = selectedChapterInfo.getChapterContentList();
        int lastReadLineNum = selectedChapterInfo.getLastReadLineNum();

        if (chapterContentList == null || chapterContentList.isEmpty() || lastReadLineNum >= chapterContentList.size()) {
            return;
        }

        int newLineNum = lastReadLineNum + 1;
        updateChapterLineNum(cacheService, selectedChapterInfo, newLineNum, lastReadLineNum);

        widget.refreshText();
        widget.setText();
        widget.setTooltip(widget.getTooltipText());
    }

    /**
     * 上一章
     */
    public static void prevChapter(@NotNull Project project) {
        CacheService cacheService = CacheService.getInstance();
        List<String> chapterList = cacheService.getChapterList();
        if (chapterList == null || chapterList.isEmpty()) return;

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null || selectedChapterInfo.getSelectedChapterIndex() == 0) return;

        ReaderOrchestrator orchestrator = ReaderOrchestrator.getInstance(project);
        orchestrator.prevPageChapter((chapterInfo, bodyElement) -> {
            if (chapterInfo == null) return;

            resetChapterLineNum(cacheService, chapterInfo);

            if (bodyElement != null) {
                orchestrator.loadThisChapterNextContent(chapterInfo.getChapterUrl(), bodyElement);
            }

            hide(project);
            ReaderStatusBarWidget widget = findWidget(project);
            if (widget != null) {
                widget.refreshText();
                widget.setText();
                widget.setTooltip(widget.getTooltipText());
            }
        });
    }

    /**
     * 下一章
     */
    public static void nextChapter(@NotNull Project project) {
        CacheService cacheService = CacheService.getInstance();
        List<String> chapterList = cacheService.getChapterList();
        if (chapterList == null || chapterList.isEmpty()) return;

        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null || selectedChapterInfo.getSelectedChapterIndex() >= chapterList.size() - 1) return;

        ReaderOrchestrator orchestrator = ReaderOrchestrator.getInstance(project);
        orchestrator.nextPageChapter((chapterInfo, bodyElement) -> {
            if (chapterInfo == null) return;

            resetChapterLineNum(cacheService, chapterInfo);

            if (bodyElement != null) {
                orchestrator.loadThisChapterNextContent(chapterInfo.getChapterUrl(), bodyElement);
            }

            hide(project);
            ReaderStatusBarWidget widget = findWidget(project);
            if (widget != null) {
                widget.refreshText();
                widget.setText();
                widget.setTooltip(widget.getTooltipText());
            }
        });
    }

    // ==================== 内部辅助方法 ====================

    private String getTooltipText() {
        ChapterInfo selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedBookInfo == null || selectedChapterInfo == null) {
            return ConfigYaml.getInstance().getNameHump();
        }
        return selectedBookInfo.getBookName() + "|" + selectedChapterInfo.getChapterTitle();
    }

    /**
     * 更新章节行号（翻行时调用）
     */
    private static void updateChapterLineNum(CacheService cacheService, ChapterInfo chapterInfo, int newLineNum, int oldLineNum) {
        ChapterInfo newChapterInfo = new ChapterInfo();
        newChapterInfo.setChapterUrl(chapterInfo.getChapterUrl());
        newChapterInfo.setChapterTitle(chapterInfo.getChapterTitle());
        newChapterInfo.setChapterContent(chapterInfo.getChapterContent());
        newChapterInfo.setChapterContentStr(chapterInfo.getChapterContentStr());
        newChapterInfo.setSelectedChapterIndex(chapterInfo.getSelectedChapterIndex());
        newChapterInfo.setLastReadLineNum(newLineNum);
        newChapterInfo.setPrevReadLineNum(Math.max(1, newLineNum - 1));
        newChapterInfo.setNextReadLineNum(oldLineNum);
        newChapterInfo.setChapterContentList(chapterInfo.getChapterContentList());
        cacheService.setSelectedChapterInfo(newChapterInfo);
    }

    /**
     * 重置章节行号到第1行（翻章时调用）
     */
    private static void resetChapterLineNum(CacheService cacheService, ChapterInfo chapterInfo) {
        Settings settings = cacheService.getSettings();
        List<String> contentArr = StringUtil.splitStringByMaxCharList(
                chapterInfo.getChapterContentStr(), settings.getSingleLineChars());
        chapterInfo.setChapterContentList(contentArr);
        chapterInfo.setLastReadLineNum(1);
        chapterInfo.setPrevReadLineNum(1);
        chapterInfo.setNextReadLineNum(2);
        cacheService.setSelectedChapterInfo(chapterInfo);
    }

    /**
     * 主题切换监听器
     */
    private void appColorsListener() {
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        bus.connect().subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) editorColorsScheme -> {
            if (editorColorsScheme == null) {
                return;
            }

            defaultFontColor();
        });
    }

    /**
     * 默认字体颜色
     */
    private void defaultFontColor() {
        Color foreground = JBUI.CurrentTheme.StatusBar.Widget.FOREGROUND;
        String foregroundHex = String.format(
                "#%02x%02x%02x",
                foreground.getRed(),
                foreground.getGreen(),
                foreground.getBlue()
        );
        label.setForeground(foreground);
        cacheService.setFontColorHex(foregroundHex);
    }
}
