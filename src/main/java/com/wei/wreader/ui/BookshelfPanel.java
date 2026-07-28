package com.wei.wreader.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.model.BookshelfItem;
import com.wei.wreader.model.BookInfo;
import com.wei.wreader.model.ChapterInfo;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.model.Settings;
import com.wei.wreader.reader.ReaderOrchestrator;
import com.wei.wreader.service.BookshelfService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.file.FileUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter
            .ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

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
        cacheService.setSelectedBookInfo(item.toBookInfo());

        if (StringUtils.isNotBlank(item.getSiteId()) && !item.getSiteId().equals("local")) {
            List<SiteBean> siteBeanList = FileUtil.readResourcesJsonList(
                    CustomSiteUtil.DEFAULT_SITE_RULE_PATH, SiteBean.class);
            for (SiteBean siteBean : siteBeanList) {
                if (item.getSiteId().equals(siteBean.getId())) {
                    cacheService.setSelectedSiteBean(siteBean);
                    cacheService.setSelectedBookSiteIndex(siteBeanList.indexOf(siteBean));
                    cacheService.setSelectedBookInfoRules(siteBean.getBookInfoRules());
                    cacheService.setSelectedChapterRules(siteBean.getChapterRules());
                    break;
                }
            }
        }

        ChapterInfo chapterInfo = cacheService.getSelectedChapterInfo();
        if (chapterInfo == null) {
            chapterInfo = new ChapterInfo();
        }
        chapterInfo.setSelectedChapterIndex(item.getChapterIndex());
        chapterInfo.setChapterTitle(item.getChapterTitle());
        chapterInfo.setLastReadLineNum(item.getLastReadLineNum());
        cacheService.setSelectedChapterInfo(chapterInfo);

        Settings settings = cacheService.getSettings();
        if (settings != null) {
            settings.setDataLoadType(item.getDataLoadType());
        }

        ReaderOrchestrator orchestrator = ReaderOrchestrator.getInstance(project);
        orchestrator.updateContentText();

        bookshelfService.updateReadingProgress(
                item.getUniqueKey(),
                item.getChapterIndex(),
                item.getChapterTitle(),
                item.getScrollBarValue(),
                item.getLastReadLineNum(),
                item.getTotalChapters()
        );
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