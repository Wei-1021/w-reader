package com.wei.wreader.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.content.HtmlContentRenderer;
import com.wei.wreader.model.*;
import com.wei.wreader.reader.ReaderOrchestrator;
import com.wei.wreader.search.SearchService;
import com.wei.wreader.service.BookshelfService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.file.FileUtil;
import com.wei.wreader.util.ui.ToolWindowUtil;
import com.wei.wreader.widget.ReaderStatusBarWidget;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookshelfPanel extends JPanel {

    private final Project project;
    private final CacheService cacheService;
    private final BookshelfService bookshelfService;

    private JBList<String> shelfList;
    private DefaultListModel<String> shelfListModel;
    private JButton removeButton;
    private JButton openButton;
    private JLabel statusLabel;

    private List<BookshelfItem> currentShelfItems;

    public BookshelfPanel(Project project) {
        this.project = project;
        this.cacheService = CacheService.getInstance();
        this.bookshelfService = BookshelfService.getInstance();

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        createUI();
        refreshList();
    }

    private void createUI() {
        JLabel titleLabel = new JLabel("📚 书架");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(JBUI.Borders.empty(5, 0, 5, 0));
        add(titleLabel, BorderLayout.NORTH);

        shelfListModel = new DefaultListModel<>();
        shelfList = new JBList<>(shelfListModel);
        shelfList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shelfList.setCellRenderer(new BookshelfCellRenderer());

        shelfList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonState();
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(shelfList);
        scrollPane.setBorder(JBUI.Borders.empty());
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        openButton = new JButton("打开阅读");
        openButton.setEnabled(false);
        openButton.addActionListener(e -> openSelectedBook());

        removeButton = new JButton("移出书架");
        removeButton.setEnabled(false);
        removeButton.addActionListener(e -> removeSelectedBook());

        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshList());

        buttonPanel.add(openButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(refreshButton);

        statusLabel = new JLabel();
        statusLabel.setBorder(JBUI.Borders.empty(2, 5, 0, 0));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    public void refreshList() {
        currentShelfItems = bookshelfService.getShelfItems();
        shelfListModel.clear();

        for (BookshelfItem item : currentShelfItems) {
            String display = formatDisplayText(item);
            shelfListModel.addElement(display);
        }

        statusLabel.setText("共 " + currentShelfItems.size() + " 本");
        updateButtonState();
    }

    private String formatDisplayText(BookshelfItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.getBookName());
        if (StringUtils.isNotBlank(item.getBookAuthor())) {
            sb.append("  -  ").append(item.getBookAuthor());
        }
        sb.append("  [").append(formatProgress(item)).append("]");
        return sb.toString();
    }

    private String formatProgress(BookshelfItem item) {
        if (StringUtils.isNotBlank(item.getChapterTitle())) {
            return item.getChapterTitle();
        }
        return "未读";
    }

    private void updateButtonState() {
        int idx = shelfList.getSelectedIndex();
        boolean selected = idx >= 0 && idx < currentShelfItems.size();
        openButton.setEnabled(selected);
        removeButton.setEnabled(selected);
    }

    private void openSelectedBook() {
        int idx = shelfList.getSelectedIndex();
        if (idx < 0 || idx >= currentShelfItems.size()) return;

        BookshelfItem item = currentShelfItems.get(idx);
        openBookFromShelf(item);
    }

    private void removeSelectedBook() {
        int idx = shelfList.getSelectedIndex();
        if (idx < 0 || idx >= currentShelfItems.size()) return;

        BookshelfItem item = currentShelfItems.get(idx);
        bookshelfService.removeFromShelf(item.getUniqueKey());
        refreshList();
    }

    private void openBookFromShelf(BookshelfItem item) {
        SiteBean siteBean = null;
        if (StringUtils.isNotBlank(item.getSiteId()) && !item.getSiteId().equals("local")) {
            List<SiteBean> siteBeanList = FileUtil.readResourcesJsonList(
                    CustomSiteUtil.DEFAULT_SITE_RULE_PATH, SiteBean.class);
            for (SiteBean sb : siteBeanList) {
                if (item.getSiteId().equals(sb.getId())) {
                    siteBean = sb;
                    cacheService.setSelectedSiteBean(siteBean);
                    cacheService.setSelectedBookSiteIndex(siteBeanList.indexOf(siteBean));
                    cacheService.setSelectedBookInfoRules(siteBean.getBookInfoRules());
                    cacheService.setSelectedChapterRules(siteBean.getChapterRules());
                    break;
                }
            }
        }

        if (siteBean == null) {
            siteBean = cacheService.getSelectedSiteBean();
        }

        BookInfo bookInfo = item.toBookInfo();
        cacheService.setSelectedBookInfo(bookInfo);
        cacheService.setTempSelectedBookInfo(bookInfo);
        cacheService.setTempSelectedSiteBean(siteBean);
        if (siteBean != null) {
            cacheService.setTempSelectedBookSiteIndex(cacheService.getSelectedBookSiteIndex());
        }

        Settings settings = cacheService.getSettings();
        if (settings != null) {
            settings.setDataLoadType(item.getDataLoadType());
        }

        final int targetChapterIndex = item.getChapterIndex();
        final String targetChapterTitle = item.getChapterTitle();
        final SiteBean finalSiteBean = siteBean;

        SearchService searchService = new SearchService(project);
        searchService.loadBookDirectory(bookInfo,
                chapterNames -> {
                    if (chapterNames == null || chapterNames.isEmpty()) {
                        return;
                    }
                    cacheService.setChapterList(new ArrayList<>(chapterNames));
                },
                chapterUrls -> {
                    if (chapterUrls == null || chapterUrls.isEmpty()) {
                        return;
                    }
                    cacheService.setChapterUrlList(new ArrayList<>(chapterUrls));

                    int rawIdx = Math.min(targetChapterIndex, chapterUrls.size() - 1);
                    final int idx = Math.max(rawIdx, 0);

                    String chapterSuffixUrl = (idx < chapterUrls.size()) ? chapterUrls.get(idx) : "";
                    String chapterUrl = buildFullChapterUrl(chapterSuffixUrl, finalSiteBean);

                    ChapterInfo chapterInfo = new ChapterInfo();
                    chapterInfo.setChapterTitle(
                            idx < cacheService.getChapterList().size()
                                    ? cacheService.getChapterList().get(idx)
                                    : targetChapterTitle);
                    chapterInfo.setChapterUrl(chapterUrl);
                    chapterInfo.setSelectedChapterIndex(idx);
                    chapterInfo.setLastReadLineNum(item.getLastReadLineNum());

                    searchService.searchBookContentRemote(chapterUrl, chapterInfo, param -> {
                        processChapterContent(param, chapterInfo, idx);
                    });
                },
                appendResult -> {
                    List<String> extraNames = appendResult.get("chapterNames");
                    List<String> extraUrls = appendResult.get("chapterUrls");
                    if (extraNames != null) {
                        List<String> current = cacheService.getChapterList();
                        if (current != null) {
                            current.addAll(extraNames);
                            cacheService.setChapterList(current);
                        }
                    }
                    if (extraUrls != null) {
                        List<String> currentUrls = cacheService.getChapterUrlList();
                        if (currentUrls != null) {
                            currentUrls.addAll(extraUrls);
                            cacheService.setChapterUrlList(currentUrls);
                        }
                    }
                }
        );
    }

    private String buildFullChapterUrl(String suffixUrl, SiteBean siteBean) {
        if (StringUtils.isBlank(suffixUrl)) {
            return suffixUrl;
        }
        if (suffixUrl.startsWith(ConstUtil.HTTP_SCHEME) ||
                suffixUrl.startsWith(ConstUtil.HTTPS_SCHEME)) {
            return suffixUrl;
        }
        String baseUrl = (siteBean != null) ? siteBean.getBaseUrl() : "";
        return baseUrl + suffixUrl;
    }

    private void processChapterContent(SearchBookCallParam param, ChapterInfo chapterInfo, int selectedIndex) {
        cacheService.setSelectedSiteBean(cacheService.getTempSelectedSiteBean());
        cacheService.setSelectedBookSiteIndex(cacheService.getTempSelectedBookSiteIndex());
        cacheService.setSelectedBookInfo(cacheService.getTempSelectedBookInfo());

        String fontColorHex = ReaderOrchestrator.getInstance(project).getFontManager().getFontColorHex();
        String fontFamily = ReaderOrchestrator.getInstance(project).getFontManager().getFontFamily();
        int fontSize = ReaderOrchestrator.getInstance(project).getFontManager().getFontSize();

        String rawContent = HtmlContentRenderer.buildCustomStyleContent(param.getChapterContentHtml(),
                fontColorHex, fontFamily, fontSize);
        final String content = (rawContent != null) ? rawContent.replaceAll("(?s)<style[^>]*>.*?</style>", "") : null;
        chapterInfo.setChapterContent(content);
        chapterInfo.setChapterContentStr(param.getChapterContentText());
        chapterInfo.setSelectedChapterIndex(selectedIndex);

        Settings settings = cacheService.getSettings();
        if (settings == null) {
            settings = new Settings();
        }
        int singleLineChars = settings.getSingleLineChars();
        chapterInfo.initLineNum(1, 2, 1, singleLineChars);
        cacheService.setSelectedChapterInfo(chapterInfo);

        int displayType = settings.getDisplayType();
        if (displayType == Settings.DISPLAY_TYPE_SIDEBAR) {
            ToolWindowUtil.updateContentText(project, textPane -> {
                if (content != null) {
                    textPane.setText(content);
                    textPane.setCaretPosition(0);
                }
            });
        } else if (displayType == Settings.DISPLAY_TYPE_STATUSBAR) {
            ReaderStatusBarWidget.update(project);
        }

        Element bodyElement = param.getBodyElement();
        if (bodyElement != null) {
            ReaderOrchestrator.getInstance(project).loadThisChapterNextContent(chapterInfo.getChapterUrl(), bodyElement);
        }
    }

    private static class BookshelfCellRenderer extends JLabel implements ListCellRenderer<String> {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            setBorder(new EmptyBorder(4, 8, 4, 8));
            setOpaque(true);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }
}