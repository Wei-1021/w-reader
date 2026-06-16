package com.wei.wreader.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.ErrorStripeEditorCustomization;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.model.Settings;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.data.ConstUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义书源规则对话框 - 重新设计布局版本。
 * <p>
 * 采用左右分栏布局：
 * - 左侧：分组列表 + 操作按钮
 * - 右侧：分组名称 + JSON编辑器 + 提示信息 + 操作按钮
 *
 * @author weizhanjie
 */
public class CustomSiteRuleDialogNew {

    public static final String TEMP_DIR = ".idea/w-reader";
    public static final String TEMP_FILE_NAME = "w-reader-custom-rule-temp.json";
    private static final String GUIDE_URL = "https://gitee.com/weizhanjie/w-reader/wikis/%E8%87%AA%E5%AE%9A%E4%B9%89%E4%B9%A6%E6%BA%90/" +
            "%E8%87%AA%E5%AE%9A%E4%B9%89%E4%B9%A6%E6%BA%90%E8%A7%84%E5%88%99%E8%AF%B4%E6%98%8E";
    private static final String TIP_TEXT = "提示：本功能规则比较简陋，目前只适合获取相对简单的书源，部分包括但不限于需要登录权限、字体加密等复杂的书源暂时是没法获取的。" +
            "如您有更好的想法，欢迎email或github留言。\"书源规则说明\"请前往Gitee/GitHub仓库Wiki页查看，或者点击下方按钮跳转。QQ群: 1060150904";

    private final Project project;
    private final CustomSiteUtil customSiteUtil;
    private final SiteRuleService siteRuleService;
    private final Settings settings;

    // 窗口组件
    private JFrame frame;
    private JPanel mainPanel;
    private JBSplitter splitter;

    // 左侧分组面板
    private JPanel leftPanel;
    private JBList<String> groupList;
    private DefaultListModel<String> groupListModel;
    private JButton addButton;
    private JButton deleteButton;
    private JButton resetButton;

    // 右侧编辑面板
    private JPanel rightPanel;
    private JTextField groupNameTextField;
    private JPanel editorContainer;
    private JTextArea noticeTextArea;
    private JButton guideButton;
    private JButton verifyButton;
    private JButton formatButton;
    private JButton confirmButton;

    // 嵌入的代码编辑器
    private EditorEx editor;
    private Document document;
    private PsiFile psiFile;
    private VirtualFile virtualFile;
    private JComponent currentEditorComponent;

    // 普通文本框模式
    private JTextArea textArea;
    private JBScrollPane textScrollPane;

    // JSON 语法错误标记
    private final List<RangeHighlighter> errorHighlighters = new ArrayList<>();

    // 当前已加载的分组名
    private String loadedGroupKey;

    // 编辑器异步初始化状态
    private volatile boolean editorInitializing = false;
    private String pendingContent;

    public CustomSiteRuleDialogNew(@NotNull Project project, Settings settings) {
        this.project = project;
        this.settings = settings;
        this.customSiteUtil = CustomSiteUtil.getInstance(project);
        this.siteRuleService = SiteRuleService.getInstance();
    }

    public void show() {
        ApplicationManager.getApplication().invokeLater(this::buildWindow);
    }

    private void buildWindow() {
        frame = new JFrame("自定义书源规则(Beta)");
        frame.setSize(1000, 750);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                disposeEditor();
            }
        });

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createLeftPanel();
        createRightPanel();

        splitter = new JBSplitter(false, "w-reader.custom-site-rule.splitter", 0.22f);
        splitter.setFirstComponent(leftPanel);
        splitter.setSecondComponent(rightPanel);
        splitter.setDividerWidth(6);

        mainPanel.add(splitter, BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);

        addEventListeners();
        loadGroupList();
    }

    // ==================== 左侧面板 ====================

    private void createLeftPanel() {
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(220, 0));
        leftPanel.setMinimumSize(new Dimension(180, 0));

        // 分组列表标题
        JPanel listHeaderPanel = new JPanel(new BorderLayout());
        listHeaderPanel.setBorder(JBUI.Borders.emptyBottom(5));
        JLabel listLabel = new JLabel("分组列表");
        listLabel.setFont(listLabel.getFont().deriveFont(Font.BOLD));
        listHeaderPanel.add(listLabel, BorderLayout.WEST);

        // 分组列表
        groupListModel = new DefaultListModel<>();
        groupList = new JBList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setVisibleRowCount(-1);

        JBScrollPane listScrollPane = new JBScrollPane(groupList);
        listScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                JBUI.Borders.empty(2)
        ));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        buttonPanel.setBorder(JBUI.Borders.emptyTop(8));

        addButton = new JButton("添加分组");
        deleteButton = new JButton("删除分组");
        resetButton = new JButton("重置");

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(resetButton);

        // 组装左侧面板
        leftPanel.add(listHeaderPanel, BorderLayout.NORTH);
        leftPanel.add(listScrollPane, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    // ==================== 右侧面板 ====================

    private void createRightPanel() {
        rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setBorder(JBUI.Borders.emptyLeft(10));

        // 顶部：分组名称
        JPanel topPanel = createGroupNamePanel();

        // 中间：编辑器
        JPanel editorPanel = createEditorPanel();

        // 底部：提示信息 + 按钮
        JPanel bottomPanel = createBottomPanel();

        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(editorPanel, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createGroupNamePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setPreferredSize(new Dimension(0, 35));

        JLabel groupNameLabel = new JLabel("分组名称:");
        groupNameTextField = new JTextField();
        groupNameTextField.setToolTipText("输入分组名称");

        panel.add(groupNameLabel, BorderLayout.WEST);
        panel.add(groupNameTextField, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                        "书源规则",
                        TitledBorder.LEFT,
                        TitledBorder.TOP
                ),
                JBUI.Borders.empty(5)
        ));

        editorContainer = new JPanel(new BorderLayout());

        // 代码编辑器模式：编辑器会在 initEditor() 中创建并添加到 editorContainer
        // 这里只判断普通文本框模式
        if (!isCodeEditorEnabled()) {
            // 普通文本框模式
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textScrollPane = new JBScrollPane(textArea);
            editorContainer.add(textScrollPane, BorderLayout.CENTER);
        }

        panel.add(editorContainer, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 判断当前是否启用代码编辑器模式
     */
    private boolean isCodeEditorEnabled() {
        return settings.getCustomSiteRuleTextAreaType() == 0
                || settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));

        // 提示信息
        noticeTextArea = new JTextArea();
        noticeTextArea.setLineWrap(true);
        noticeTextArea.setWrapStyleWord(true);
        noticeTextArea.setEditable(false);
        noticeTextArea.setRows(3);
        noticeTextArea.setBorder(JBUI.Borders.empty(5));
        noticeTextArea.setText(TIP_TEXT);
        noticeTextArea.setBackground(UIManager.getColor("Panel.background"));

        JBScrollPane noticeScrollPane = new JBScrollPane(noticeTextArea);
        noticeScrollPane.setPreferredSize(new Dimension(0, 70));
        noticeScrollPane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        guideButton = new JButton("规则教程");
        formatButton = new JButton("格式化");
        verifyButton = new JButton("校验");
        confirmButton = new JButton("保存");

        if (!isCodeEditorEnabled()) {
            // 隐藏格式化按钮
            formatButton.setVisible(false);
        }

        buttonPanel.add(guideButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(formatButton);
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(verifyButton);
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(confirmButton);

        panel.add(noticeScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== 分组列表管理 ====================

    private void loadGroupList() {
        groupListModel.clear();
        List<String> groupList = customSiteUtil.getCustomSiteKeyGroupList();
        if (groupList != null) {
            for (String key : groupList) {
                groupListModel.addElement(key);
            }
        }

        // 选中之前选中的分组
        String selectedKey = siteRuleService.getSelectedCustomSiteRuleKey();
        if (selectedKey != null && !selectedKey.isEmpty()) {
            this.groupList.setSelectedValue(selectedKey, true);
        }
    }

    private void loadSelectedGroup() {
        String selectedKey = groupList.getSelectedValue();
        if (selectedKey == null || selectedKey.isEmpty()) {
            return;
        }

        loadedGroupKey = selectedKey;

        Map<String, String> customSiteRuleGroupMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
        String siteBeanJson = customSiteRuleGroupMap.get(selectedKey);
        if (StringUtils.isEmpty(siteBeanJson)) {
            Messages.showInfoMessage("分组不存在", "提示");
            return;
        }

        siteBeanJson = siteBeanJson.replace("\r\n", "\n").replace("\r", "\n");
        groupNameTextField.setText(selectedKey);
        setRuleText(siteBeanJson);
    }

    private void addNewGroup() {
        String groupName = Messages.showInputDialog(
                "请输入分组名称：",
                "添加分组",
                Messages.getQuestionIcon()
        );

        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }

        groupName = groupName.trim();

        // 检查是否已存在于服务中
        Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
        if (siteMap.containsKey(groupName)) {
            Messages.showInfoMessage("分组【" + groupName + "】已存在", "提示");
            return;
        }

        // 只准备编辑环境，不立即添加到列表
        groupNameTextField.setText(groupName);
        setRuleText("");
        loadedGroupKey = null;

        // 选中列表中的对应项（如果存在）
        if (groupListModel.contains(groupName)) {
            groupList.setSelectedValue(groupName, true);
        }
    }

    private void deleteSelectedGroup() {
        String selectedKey = groupList.getSelectedValue();
        if (selectedKey == null || selectedKey.isEmpty()) {
            Messages.showInfoMessage("请选择分组", "提示");
            return;
        }

        if (ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(selectedKey)) {
            Messages.showInfoMessage("默认分组不能删除", "提示");
            return;
        }

        if (Messages.showYesNoDialog("确定要删除分组【" + selectedKey + "】吗？", "提示", Messages.getQuestionIcon()) != Messages.YES) {
            return;
        }

        // 从服务中删除
        Map<String, String> originalMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
        originalMap.remove(selectedKey);
        siteRuleService.setCustomSiteRuleOriginalStrMap(originalMap);

        Map<String, List<SiteBean>> siteMap = siteRuleService.getCustomSiteRuleGroupMap();
        siteMap.remove(selectedKey);
        siteRuleService.setCustomSiteRuleGroupMap(siteMap);

        // 从列表中删除
        groupListModel.removeElement(selectedKey);

        // 清空编辑器
        groupNameTextField.setText("");
        setRuleText("");
        loadedGroupKey = null;

        Messages.showInfoMessage("删除成功", "提示");
    }

    private void resetEditor() {
        groupList.clearSelection();
        groupNameTextField.setText("");
        setRuleText("");
        loadedGroupKey = null;
    }

    // ==================== 事件监听 ====================

    private void addEventListeners() {
        // 列表选择事件
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedGroup();
            }
        });

        // 按钮事件
        addButton.addActionListener(e -> addNewGroup());
        deleteButton.addActionListener(e -> deleteSelectedGroup());
        resetButton.addActionListener(e -> resetEditor());

        guideButton.addActionListener(e -> {
            BrowserUtil.browse(GUIDE_URL);
        });

        formatButton.addActionListener(e -> formatJson());
        verifyButton.addActionListener(e -> verifyRule());
        confirmButton.addActionListener(e -> saveRule());
    }

    // ==================== 嵌入式编辑器 ====================

    private void initEditor() {
        if (editor != null && !editor.isDisposed()) {
            return;
        }
        if (editorInitializing) {
            return;
        }
        editorInitializing = true;

        // 显示加载占位
        JLabel loadingLabel = new JLabel("编辑器初始化中...", SwingConstants.CENTER);
        editorContainer.removeAll();
        editorContainer.add(loadingLabel, BorderLayout.CENTER);
        editorContainer.revalidate();
        editorContainer.repaint();

        // 创建或复用临时文件并初始化编辑器
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (frame == null || !frame.isDisplayable()) {
                    return;
                }

                // 复用已有文件或创建新文件
                if (virtualFile == null || !virtualFile.isValid()) {
                    virtualFile = getOrCreateTempVirtualFile("");
                }

                psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile == null) {
                    throw new RuntimeException("无法获取 PSI 文件");
                }

                document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                if (document == null) {
                    document = EditorFactory.getInstance().createDocument("");
                }

                FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
                editor = (EditorEx) EditorFactory.getInstance().createEditor(document, project, jsonFileType, false);

                // 编辑器配置
                EditorSettings editorSettings = editor.getSettings();
                editorSettings.setLineNumbersShown(true);
                editorSettings.setFoldingOutlineShown(true);
                editorSettings.setAutoCodeFoldingEnabled(true);
                editorSettings.setIndentGuidesShown(true);
                editorSettings.setLineMarkerAreaShown(true);
                editorSettings.setCaretRowShown(true);
                editorSettings.setUseSoftWraps(false);

                ErrorStripeEditorCustomization.ENABLED.customize(editor);

                currentEditorComponent = editor.getComponent();
                registerFormatShortcut();

                editorContainer.removeAll();
                editorContainer.add(currentEditorComponent, BorderLayout.CENTER);
                editorContainer.revalidate();
                editorContainer.repaint();

                // 应用等待中的内容
                if (pendingContent != null) {
                    String content = pendingContent;
                    pendingContent = null;
                    applyRuleText(content);
                }
            } catch (Exception ex) {
                Messages.showErrorDialog("编辑器初始化失败: " + ex.getMessage(), "错误");
            } finally {
                editorInitializing = false;
            }
        });
    }

    /**
     * 注册代码格式化快捷键（动态获取 IDE 设置的快捷键）
     */
    private void registerFormatShortcut() {
        if (editor == null || editor.isDisposed()) {
            return;
        }

        try {
            // 获取 "ReformatCode" action 的快捷键
            KeymapManager keymapManager = KeymapManager.getInstance();
            Keymap activeKeymap = keymapManager.getActiveKeymap();

            Shortcut[] shortcuts = activeKeymap.getShortcuts("ReformatCode");

            for (Shortcut shortcut : shortcuts) {
                if (shortcut instanceof KeyboardShortcut keyboardShortcut) {
                    KeyStroke keyStroke = keyboardShortcut.getFirstKeyStroke();
                    // 注册到编辑器的内容组件（实际编辑区域）
                    editor.getContentComponent().registerKeyboardAction(
                            e -> formatJson(),
                            keyStroke,
                            JComponent.WHEN_FOCUSED
                    );
                    break;
                }
            }
        } catch (Exception e) {
            // 如果获取快捷键失败，不注册（不影响其他功能）
        }
    }

    private void setRuleText(String content) {
        String rawContent = content != null ? content : "";

        // 标准化换行符为 \n（IntelliJ 编辑器要求）
        final String finalContent = rawContent.replace("\r\n", "\n").replace("\r", "\n");

        if (!isCodeEditorEnabled()) {
            // 普通文本框模式
            if (textArea != null) {
                textArea.setText(finalContent);
            }
            return;
        }

        // 代码编辑器模式
        if (editor == null || editor.isDisposed()) {
            if (editorInitializing) {
                // 编辑器正在异步初始化，暂存内容
                pendingContent = finalContent;
                return;
            }
            initEditor();
            // initEditor 是异步的，暂存内容
            pendingContent = finalContent;
            return;
        }

        applyRuleText(finalContent);
    }

    /**
     * 将内容写入临时文件并同步到编辑器（必须在 EDT 上调用，编辑器已就绪）
     */
    private void applyRuleText(String content) {
        if (document == null || virtualFile == null) {
            return;
        }

        // 0. 先保存当前 Document，使文件与 Document 同步，避免 IntelliJ 弹出"文件已变更"提示
        FileDocumentManager.getInstance().saveDocument(document);

        // 1. 写入磁盘文件
        try {
            File ioFile = new File(virtualFile.getPath());
            Files.writeString(ioFile.toPath(), content, StandardCharsets.UTF_8);
            // 2. 刷新 VFS，使磁盘变更同步到虚拟文件
            virtualFile.refresh(false, false);
        } catch (IOException e) {
            // 文件写入失败时仍更新编辑器内容
        }

        // 3. 更新编辑器 Document
        if (content.length() <= 8000) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(0, document.getTextLength(), content);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            });
        } else {
            // 大文本分块写入
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(0, document.getTextLength(), "");
                PsiDocumentManager.getInstance(project).commitDocument(document);
            });

            final int CHUNK_SIZE = 5000;
            for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
                final int start = i;
                final int end = Math.min(i + CHUNK_SIZE, content.length());
                final String chunk = content.substring(start, end);
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.insertString(document.getTextLength(), chunk);
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                });
            }
        }

        refreshFolding();
        triggerJsonValidation();
    }

    private String getRuleText() {
        if (!isCodeEditorEnabled()) {
            // 普通文本框模式
            return textArea != null ? textArea.getText() : "";
        }

        // 代码编辑器模式
        if (document == null) {
            return "";
        }
        return document.getText();
    }

    private void refreshFolding() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (editor != null && !editor.isDisposed()) {
                CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
            }
        });
    }

    private void triggerJsonValidation() {
        String text = getRuleText();
        if (text.isEmpty()) {
            clearJsonErrors();
            return;
        }

        clearJsonErrors();

        try {
            new ObjectMapper().readTree(text);
        } catch (JsonProcessingException ex) {
            ApplicationManager.getApplication().invokeLater(() -> markJsonError(ex));
        }
    }

    private void markJsonError(JsonProcessingException ex) {
        if (editor == null || editor.isDisposed() || document == null) {
            return;
        }

        int errorLine = ex.getLocation() != null ? ex.getLocation().getLineNr() - 1 : 0;
        errorLine = Math.max(0, Math.min(errorLine, document.getLineCount() - 1));

        int lineStartOffset = document.getLineStartOffset(errorLine);
        int lineEndOffset = document.getLineEndOffset(errorLine);

        MarkupModel markupModel = editor.getMarkupModel();

        // 错误行红色波浪下划线
        TextAttributes errorAttrs = new TextAttributes();
        errorAttrs.setEffectType(EffectType.WAVE_UNDERSCORE);
        errorAttrs.setEffectColor(JBColor.RED);

        RangeHighlighter lineHighlighter = markupModel.addRangeHighlighter(
                lineStartOffset, lineEndOffset,
                HighlighterLayer.ERROR, errorAttrs,
                HighlighterTargetArea.EXACT_RANGE
        );
        errorHighlighters.add(lineHighlighter);

        // 错误条纹
        TextAttributes stripeAttrs = new TextAttributes();
        stripeAttrs.setErrorStripeColor(JBColor.RED);

        RangeHighlighter stripeHighlighter = markupModel.addRangeHighlighter(
                lineStartOffset, lineEndOffset,
                HighlighterLayer.ERROR, stripeAttrs,
                HighlighterTargetArea.LINES_IN_RANGE
        );
        errorHighlighters.add(stripeHighlighter);
    }

    private void clearJsonErrors() {
        if (editor == null || editor.isDisposed()) {
            return;
        }
        MarkupModel markupModel = editor.getMarkupModel();
        for (RangeHighlighter highlighter : errorHighlighters) {
            if (highlighter.isValid()) {
                markupModel.removeHighlighter(highlighter);
            }
        }
        errorHighlighters.clear();
    }

    private void disposeEditor() {
        clearJsonErrors();

        // 保存 Document 到磁盘，避免关闭时 IntelliJ 弹出"文件已更改"提示
        if (document != null) {
            FileDocumentManager.getInstance().saveDocument(document);
        }

        if (editor != null && !editor.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(editor);
            editor = null;
        }
        document = null;
        psiFile = null;
        // 保留 virtualFile 和磁盘临时文件，下次打开窗口时复用
    }

    private VirtualFile getOrCreateTempVirtualFile(String content) throws IOException {
        String tempDirPath = project.getBasePath() + "/" + TEMP_DIR;
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, TEMP_FILE_NAME);
        // 仅文件不存在时写入初始内容，已有文件则复用
        if (!tempFile.exists()) {
            Files.writeString(tempFile.toPath(), content, StandardCharsets.UTF_8);
        }

        // 先尝试直接查找（VFS 可能已缓存该路径）
        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(tempFile);
        if (vf != null) {
            vf.refresh(false, false);
            return vf;
        }

        // 强制刷新后查找
        LocalFileSystem.getInstance().refreshWithoutFileWatcher(false);
        vf = LocalFileSystem.getInstance().findFileByIoFile(tempFile);
        if (vf != null) {
            return vf;
        }

        // 最后尝试 refreshAndFindFileByIoFile
        vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);
        if (vf == null) {
            throw new IOException("无法找到刷新后的虚拟文件: " + tempFile.getAbsolutePath());
        }
        return vf;
    }

    // ==================== 格式化、校验和保存 ====================

    /**
     * 使用 IntelliJ 内置的代码格式化功能格式化 JSON
     */
    private void formatJson() {
        String rule = getRuleText();
        if (rule == null || rule.isEmpty()) {
            Messages.showInfoMessage("请输入自定义书源规则", "提示");
            return;
        }

        if (psiFile == null || document == null) {
            Messages.showInfoMessage("编辑器未初始化", "提示");
            return;
        }

        try {
            // 使用 IntelliJ 内置的代码格式化功能
            WriteCommandAction.runWriteCommandAction(project, () -> {
                CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
                codeStyleManager.reformat(psiFile);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            });
            Messages.showInfoMessage("格式化成功", "提示");
        } catch (Exception e) {
            Messages.showErrorDialog("格式化出错: " + e.getMessage(), "错误");
        }
    }

    private void verifyRule() {
        String rule = getRuleText();
        if (rule == null || rule.isEmpty()) {
            Messages.showInfoMessage("请输入自定义书源规则", "提示");
            return;
        }

        try {
            customSiteUtil.parseCustomSiteRule(rule, successValidationResult -> {
                Messages.showInfoMessage("校验通过", "提示");
            }, null);
        } catch (IllegalArgumentException e) {
            Messages.showErrorDialog(e.getMessage(), "校验失败");
        } catch (Exception e) {
            Messages.showErrorDialog("校验出错: " + e.getMessage(), "错误");
        }
    }

    private void saveRule() {
        String groupName = groupNameTextField.getText();
        if (groupName == null || groupName.trim().isEmpty()) {
            Messages.showInfoMessage("请输入分组名称", "提示");
            return;
        }

        final String finalGroupName = groupName.trim();

        String rule = getRuleText();
        if (rule == null || rule.isEmpty()) {
            Messages.showInfoMessage("请输入自定义书源规则", "提示");
            return;
        }

        customSiteUtil.parseCustomSiteRule(rule, successValidationResult -> {
            if (Messages.showYesNoDialog("确定保存？", "提示", Messages.getQuestionIcon()) != Messages.YES) {
                return;
            }

            List<SiteBean> siteBeans = successValidationResult.getBeanList();

            Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
            siteMap.put(finalGroupName, siteBeans);
            siteRuleService.setCustomSiteRuleGroupMap(siteMap);

            Map<String, String> customSiteRuleOriginalStrMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
            if (customSiteRuleOriginalStrMap == null) {
                customSiteRuleOriginalStrMap = new HashMap<>();
            }
            customSiteRuleOriginalStrMap.put(finalGroupName, rule);
            siteRuleService.setCustomSiteRuleOriginalStrMap(customSiteRuleOriginalStrMap);

            // 更新列表
            if (!groupListModel.contains(finalGroupName)) {
                groupListModel.addElement(finalGroupName);
            }
            groupList.setSelectedValue(finalGroupName, true);

            Messages.showInfoMessage("保存成功", "提示");
        }, null);
    }
}
