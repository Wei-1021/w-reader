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
import com.wei.wreader.service.SiteRuleService;
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
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookshelfPanel extends JPanel {

    private final Project project;
    private final CacheService cacheService;
    private final BookshelfService bookshelfService;
    private final SiteRuleService siteRuleService;

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
        this.siteRuleService = SiteRuleService.getInstance();

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        createUI();
        refreshList();
    }

    private void createUI() {
        JLabel titleLabel = new JLabel("📚 书架");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(JBUI.Borders.empty(5, 0));
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

        shelfList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedBook();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupMenu(e);
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
        sb.append("【")
                .append(item.getSiteGroupKey())
                .append("】 ")
                .append(item.getBookName());
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

    private void showPopupMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) return;

        int row = shelfList.locationToIndex(e.getPoint());
        if (row < 0 || row >= currentShelfItems.size()) return;

        shelfList.setSelectedIndex(row);

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("打开阅读");
        openItem.addActionListener(ev -> openSelectedBook());
        JMenuItem removeItem = new JMenuItem("移出书架");
        removeItem.addActionListener(ev -> removeSelectedBook());

        popupMenu.add(openItem);
        popupMenu.addSeparator();
        popupMenu.add(removeItem);
        popupMenu.show(shelfList, e.getX(), e.getY());
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
        SiteBean siteBean = findSiteBean(item);
        if (siteBean == null) {
            siteBean = cacheService.getSelectedSiteBean();
        }

        prepareBookContext(item, siteBean);
        loadBookContent(item, siteBean);
    }

    /**
     * 根据书架条目查找对应的书源规则
     */
    private SiteBean findSiteBean(BookshelfItem item) {
        if (StringUtils.isBlank(item.getSiteId()) || item.getSiteId().equals("local")) {
            return null;
        }

        Map<String, List<SiteBean>> customSiteRuleGroupMap = siteRuleService.getCustomSiteRuleGroupMap();

        // 无自定义书源，从默认规则中查找
        if (customSiteRuleGroupMap == null || customSiteRuleGroupMap.isEmpty()) {
            return findSiteBeanFromDefault(item);
        }

        // 优先使用 siteGroupKey 精确定位分组
        String siteGroupKey = item.getSiteGroupKey();
        if (StringUtils.isNotBlank(siteGroupKey)) {
            List<SiteBean> targetGroup = customSiteRuleGroupMap.get(siteGroupKey);
            SiteBean found = findSiteBeanInGroup(targetGroup, item.getSiteId());
            if (found != null) {
                applySiteBean(found, targetGroup.indexOf(found), siteGroupKey);
                return found;
            }
        }

        // 回退：遍历所有分组查找
        return findSiteBeanFromAllGroups(customSiteRuleGroupMap, item.getSiteId());
    }

    /**
     * 从默认书源规则中查找
     */
    private SiteBean findSiteBeanFromDefault(BookshelfItem item) {
        List<SiteBean> siteBeanList = FileUtil.readResourcesJsonList(
                CustomSiteUtil.DEFAULT_SITE_RULE_PATH, SiteBean.class
        );
        if (siteBeanList == null) return null;

        SiteBean found = findSiteBeanInGroup(siteBeanList, item.getSiteId());
        if (found != null) {
            cacheService.setSelectedSiteBean(found);
            cacheService.setSelectedBookSiteIndex(siteBeanList.indexOf(found));
            cacheService.setSelectedBookInfoRules(found.getBookInfoRules());
            cacheService.setSelectedChapterRules(found.getChapterRules());
        }
        return found;
    }

    /**
     * 从所有分组中查找书源
     */
    private SiteBean findSiteBeanFromAllGroups(Map<String, List<SiteBean>> groupMap, String siteId) {
        for (Map.Entry<String, List<SiteBean>> entry : groupMap.entrySet()) {
            SiteBean found = findSiteBeanInGroup(entry.getValue(), siteId);
            if (found != null) {
                applySiteBean(found, entry.getValue().indexOf(found), entry.getKey());
                return found;
            }
        }
        return null;
    }

    /**
     * 在指定分组中查找书源
     */
    private SiteBean findSiteBeanInGroup(List<SiteBean> group, String siteId) {
        if (group == null || StringUtils.isBlank(siteId)) return null;
        for (SiteBean sb : group) {
            if (siteId.equals(sb.getId())) {
                return sb;
            }
        }
        return null;
    }

    /**
     * 应用选中的书源到缓存
     */
    private void applySiteBean(SiteBean siteBean, int index, String siteGroupKey) {
        cacheService.setSelectedSiteBean(siteBean);
        cacheService.setSelectedBookSiteIndex(index);
        cacheService.setSelectedBookInfoRules(siteBean.getBookInfoRules());
        cacheService.setSelectedChapterRules(siteBean.getChapterRules());
        siteRuleService.setSelectedCustomSiteRuleKey(siteGroupKey);
    }

    /**
     * 准备书籍上下文信息
     */
    private void prepareBookContext(BookshelfItem item, SiteBean siteBean) {
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
    }

    /**
     * 加载书籍内容
     */
    private void loadBookContent(BookshelfItem item, SiteBean siteBean) {
        final int targetChapterIndex = item.getChapterIndex();
        final String targetChapterTitle = item.getChapterTitle();

        SearchService searchService = new SearchService(project);
        searchService.loadBookDirectory(item.toBookInfo(),
                chapterNames -> {
                    if (chapterNames != null && !chapterNames.isEmpty()) {
                        cacheService.setChapterList(new ArrayList<>(chapterNames));
                    }
                },
                chapterUrls -> {
                    if (chapterUrls == null || chapterUrls.isEmpty()) return;
                    cacheService.setChapterUrlList(new ArrayList<>(chapterUrls));
                    loadChapterContent(item, siteBean, targetChapterIndex, targetChapterTitle, searchService);
                },
                appendResult -> appendChapterData(appendResult)
        );
    }

    /**
     * 加载指定章节内容
     */
    private void loadChapterContent(BookshelfItem item, SiteBean siteBean,
                                     int targetChapterIndex, String targetChapterTitle,
                                     SearchService searchService) {
        List<String> chapterUrls = cacheService.getChapterUrlList();
        int rawIdx = Math.min(targetChapterIndex, chapterUrls.size() - 1);
        int idx = Math.max(rawIdx, 0);

        String chapterSuffixUrl = chapterUrls.get(idx);
        String chapterUrl = buildFullChapterUrl(chapterSuffixUrl, siteBean);

        ChapterInfo chapterInfo = new ChapterInfo();
        List<String> chapterList = cacheService.getChapterList();
        chapterInfo.setChapterTitle(idx < chapterList.size() ? chapterList.get(idx) : targetChapterTitle);
        chapterInfo.setChapterUrl(chapterUrl);
        chapterInfo.setSelectedChapterIndex(idx);
        chapterInfo.setLastReadLineNum(item.getLastReadLineNum());

        searchService.searchBookContentRemote(chapterUrl, chapterInfo, param -> {
            processChapterContent(param, chapterInfo, idx);
        });
    }

    /**
     * 追加章节数据（分页加载）
     */
    private void appendChapterData(Map<String, List<String>> appendResult) {
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