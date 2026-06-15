package com.wei.wreader.action;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.ErrorStripeEditorCustomization;
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
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义书源规则对话框 (JFrame 版本)
 *
 * @author weizhanjie
 */
public class CustomSiteRuleDialog {

    private static final String RULE_FILE_NAME = "w-reader-custom-rule.json";

    private final Project project;
    private final CustomSiteUtil customSiteUtil;
    private final SiteRuleService siteRuleService;
    private final Settings settings;

    // 定义组件
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel firstLayer;
    private JPanel secondLayer;
    private JPanel thirdLayer;
    private JPanel fourthLayer;
    private JPanel fifthLayer;
    private ComboBox<String> comboBox;
    private JButton loadButton;
    private JButton resetButton;
    private JButton deleteButton;
    private JTextField groupNameTextField;
    private JBScrollPane scrollPane;
    private JTextArea textArea;
    private JButton guideButton;
    private JButton verifyButton;
    private JButton confirmButton;
    private Document document;
    private EditorEx editor;

    // 参数
    private String loadSourceGroupKeyName = "";

    public CustomSiteRuleDialog(@NotNull Project project, Settings settings) {
        this.project = project;
        this.settings = settings;
        this.customSiteUtil = CustomSiteUtil.getInstance(project);
        this.siteRuleService = SiteRuleService.getInstance();
    }

    public void show() {
        ApplicationManager.getApplication().invokeLater(() -> {
            buildWindow();
        });
    }

    private void buildWindow() {
        List<String> customSiteKeyGroupList = customSiteUtil.getCustomSiteKeyGroupList();
        String selectedKey = siteRuleService.getSelectedCustomSiteRuleKey();

        loadSourceGroupKeyName = "";

        frame = new JFrame("自定义书源规则(Beta)");
        frame.setSize(850, 700);
        frame.setLocationRelativeTo(null);

        // 创建主面板，使用垂直布局
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 第一层：下拉框和按钮
        firstLayer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstLayer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        comboBox = new ComboBox<>();
        if (customSiteKeyGroupList != null && !customSiteKeyGroupList.isEmpty()) {
            for (String key : customSiteKeyGroupList) {
                comboBox.addItem(key);
            }
        }
        comboBox.setSelectedItem(selectedKey);
        loadButton = new JButton("加载");
        resetButton = new JButton("重置");
        deleteButton = new JButton("删除");
        firstLayer.add(comboBox);
        firstLayer.add(Box.createHorizontalStrut(5));
        firstLayer.add(loadButton);
        firstLayer.add(Box.createHorizontalStrut(5));
        firstLayer.add(resetButton);
        firstLayer.add(Box.createHorizontalStrut(5));
        firstLayer.add(deleteButton);

        // 第二层：标签和文本框
        secondLayer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        secondLayer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        JLabel groupNameLabel = new JLabel("分组名称:");
        groupNameTextField = new JTextField(30);
        secondLayer.add(groupNameLabel);
        secondLayer.add(Box.createHorizontalStrut(5));
        secondLayer.add(groupNameTextField);

        // 第三层：滚动区域包含文本编辑区
        thirdLayer = new JPanel(new BorderLayout());
        if (isCodeEditorEnabled()) {
            createEditor();
            setEditorSetting();
            thirdLayer.add(new JLabel("书源规则:"), BorderLayout.NORTH);
            thirdLayer.add(editor.getComponent(), BorderLayout.CENTER);
        } else {
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            scrollPane = new JBScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(800, 500));
            scrollPane.setMinimumSize(new Dimension(600, 500));
            thirdLayer.add(new JLabel("书源规则:"), BorderLayout.NORTH);
            thirdLayer.add(scrollPane, BorderLayout.CENTER);
        }

        // 第四层：提示文本区域
        fourthLayer = new JPanel(new BorderLayout());
        fourthLayer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JTextArea noticeTextArea = new JTextArea();
        noticeTextArea.setLineWrap(true);
        noticeTextArea.setWrapStyleWord(true);
        noticeTextArea.setEditable(false);
        noticeTextArea.setBorder(JBUI.Borders.empty());
        noticeTextArea.setText("提示：本功能规则比较简陋，目前只适合获取相对简单的书源，部分包括但不限于需要登录权限、字体加密等复杂的书源暂时是没法获取的。" +
                "如您有更好的想法，欢迎email或github留言。\"书源规则说明\"请前往Gitee/GitHub仓库Wiki页查看，或者点击下方按钮跳转。QQ群: 1060150904");
        noticeTextArea.setBackground(UIManager.getColor("Panel.background"));
        fourthLayer.add(noticeTextArea, BorderLayout.CENTER);

        // 第五层：规则教程放左边，校验和确定按钮放右边
        fifthLayer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        fifthLayer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        guideButton = new JButton("规则教程");
        verifyButton = new JButton("校验");
        confirmButton = new JButton("确定");
        fifthLayer.add(guideButton);
        fifthLayer.add(Box.createHorizontalStrut(30));
        fifthLayer.add(verifyButton);
        fifthLayer.add(Box.createHorizontalStrut(5));
        fifthLayer.add(confirmButton);

        // 添加各层到主面板
        mainPanel.add(firstLayer);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(secondLayer);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(thirdLayer);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(fourthLayer);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(fifthLayer);

        frame.add(mainPanel);
        frame.setVisible(true);

        addEventListeners();
    }

    /**
     * 添加事件监听器
     */
    private void addEventListeners() {
        addMainPanelComponentListener();
        addLoadBtnEventListeners();
        addResetBtnEventListeners();
        addDeleteBtnEventListeners();
        addGuideBtnEventListeners();
        addVerifyBtnEventListeners();
        addConfirmBtnEventListeners();
    }

    /**
     * 添加主面板的组件监听器
     */
    private void addMainPanelComponentListener() {
        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = mainPanel.getWidth() - 50;
                int height = mainPanel.getHeight() - 220;
                if (scrollPane != null) {
                    scrollPane.setPreferredSize(new Dimension(width, height));
                }
                firstLayer.setPreferredSize(new Dimension(width, 35));
                secondLayer.setPreferredSize(new Dimension(width, 35));
                thirdLayer.setPreferredSize(new Dimension(width, height));
                fourthLayer.setPreferredSize(new Dimension(width, 80));
                fifthLayer.setPreferredSize(new Dimension(width, 40));
            }
        });
    }

    /**
     * 添加"加载"按钮监听器
     */
    private void addLoadBtnEventListeners() {
        loadButton.addActionListener(e -> {
            String groupKeyName = (String) comboBox.getSelectedItem();
            if (groupKeyName == null || groupKeyName.isEmpty()) {
                Messages.showInfoMessage("请选择分组", "提示");
                return;
            }

            loadSourceGroupKeyName = groupKeyName;

            Map<String, String> customSiteRuleGroupMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
            String siteBeanJson = customSiteRuleGroupMap.get(groupKeyName);
            if (StringUtils.isEmpty(siteBeanJson)) {
                Messages.showInfoMessage("分组不存在", "提示");
                return;
            }

            siteBeanJson = siteBeanJson.replace("\r\n", "\n").replace("\r", "\n");
            groupNameTextField.setText(groupKeyName);
            setRuleText(siteBeanJson);
        });
    }

    /**
     * 添加"重置"按钮监听器
     */
    private void addResetBtnEventListeners() {
        resetButton.addActionListener(e -> {
            comboBox.setSelectedIndex(0);
            setRuleText("");
            groupNameTextField.setText("");
            loadSourceGroupKeyName = "";
        });
    }

    /**
     * 添加"删除"按钮监听器
     */
    private void addDeleteBtnEventListeners() {
        deleteButton.addActionListener(e -> {
            String groupKeyName = (String) comboBox.getSelectedItem();
            if (groupKeyName == null || groupKeyName.isEmpty()) {
                Messages.showInfoMessage("请选择分组", "提示");
                return;
            }

            if (ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(groupKeyName)) {
                Messages.showInfoMessage("默认分组不能删除", "提示");
                return;
            }

            if (Messages.showYesNoDialog("确定要删除分组【" + groupKeyName + "】吗？", "提示", Messages.getQuestionIcon()) != Messages.YES) {
                return;
            }

            // 删除对应的缓存信息--原始JSON字符串
            Map<String, String> originalMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
            originalMap.remove(groupKeyName);
            siteRuleService.setCustomSiteRuleOriginalStrMap(originalMap);
            // 删除对应的缓存信息--转换后续的列表
            Map<String, List<SiteBean>> siteMap = siteRuleService.getCustomSiteRuleGroupMap();
            siteMap.remove(groupKeyName);
            siteRuleService.setCustomSiteRuleGroupMap(siteMap);

            // 删除下拉框的下拉选项
            comboBox.removeItem(groupKeyName);
            // 清空文本框
            groupNameTextField.setText("");
            setRuleText("");

            Messages.showInfoMessage("删除成功", "提示");
        });
    }

    /**
     * 添加"规则教程"按钮监听器
     */
    private void addGuideBtnEventListeners() {
        guideButton.addActionListener(e -> {
            BrowserUtil.browse("https://gitee.com/weizhanjie/w-reader/wikis/%E8%87%AA%E5%AE%9A%E4%B9%89%E4%B9%A6%E6%BA%90%E8%A7%84%E5%88%99%E8%AF%B4%E6%98%8E");
        });
    }

    /**
     * 添加"验证"按钮监听器
     */
    private void addVerifyBtnEventListeners() {
        verifyButton.addActionListener(e -> {
            String rule = getRuleText();
            if (rule == null || rule.isEmpty()) {
                Messages.showInfoMessage("请输入自定义书源规则", "提示");
                return;
            }

            customSiteUtil.parseCustomSiteRule(rule, successValidationResult -> {
                List<SiteBean> siteBeans = successValidationResult.getBeanList();
                for (SiteBean siteBean : siteBeans) {
                    System.out.println(siteBean);
                }
                Messages.showInfoMessage("校验通过", "提示");
            }, null);
        });
    }

    /**
     * 添加"确定"按钮监听器
     */
    private void addConfirmBtnEventListeners() {
        confirmButton.addActionListener(e -> {
            saveRule();
        });
    }

    /**
     * 保存规则
     */
    private void saveRule() {
        String groupName = groupNameTextField.getText();
        if (groupName == null || groupName.isEmpty()) {
            Messages.showInfoMessage("请输入分组名称", "提示");
            return;
        }

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
            siteMap.put(groupName, siteBeans);
            siteRuleService.setCustomSiteRuleGroupMap(siteMap);

            Map<String, String> customSiteRuleOriginalStrMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
            if (customSiteRuleOriginalStrMap == null) {
                customSiteRuleOriginalStrMap = new HashMap<>();
            }
            customSiteRuleOriginalStrMap.put(groupName, rule);

            comboBox.addItem(groupName);

            Messages.showInfoMessage("保存成功", "提示");
        }, null);
    }

    /**
     * 判断当前是否启用代码编辑器模式
     */
    private boolean isCodeEditorEnabled() {
        return settings.getCustomSiteRuleTextAreaType() == 0
                || settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR;
    }

    /**
     * 创建代码编辑器
     */
    private void createEditor() {
        FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        PsiFile psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText(RULE_FILE_NAME, jsonFileType, "");
        document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

        if (document == null) {
            document = EditorFactory.getInstance().createDocument("");
        }

        editor = (EditorEx) EditorFactory.getInstance()
                .createEditor(document, project, jsonFileType, false);

        JComponent editorComponent = editor.getComponent();
        editorComponent.setPreferredSize(new Dimension(800, 500));
        editorComponent.setMinimumSize(new Dimension(600, 500));
    }

    /**
     * 设置编辑器配置
     */
    private void setEditorSetting() {
        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setLineNumbersShown(true);
        editorSettings.setIndentGuidesShown(true);
        editorSettings.setFoldingOutlineShown(true);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setCaretRowShown(true);

        ErrorStripeEditorCustomization.ENABLED.customize(editor);

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> refreshFolding());
            }
        });
    }

    /**
     * 刷新 JSON 代码折叠区域
     */
    private void refreshFolding() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (editor != null && !editor.isDisposed()) {
                CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
            }
        });
    }

    /**
     * 设置规则编辑区文本
     */
    private void setRuleText(String content) {
        if (!isCodeEditorEnabled()) {
            textArea.setText(content);
            return;
        }

        if (document == null) {
            return;
        }

        if (content.length() <= 8000) {
            String finalContent = content;
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(0, document.getTextLength(), finalContent);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            });
            refreshFolding();
            return;
        }

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
        refreshFolding();
    }

    /**
     * 获取当前规则编辑区文本
     */
    private String getRuleText() {
        if (!isCodeEditorEnabled()) {
            return textArea.getText();
        }
        if (document == null) {
            return "";
        }
        PsiDocumentManager.getInstance(project).commitDocument(document);
        return document.getText();
    }
}
