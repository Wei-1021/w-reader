package com.wei.wreader.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ErrorStripeEditorCustomization;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.agent.AgentCallback;
import com.wei.wreader.agent.SiteRuleAgent;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.CredentialService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.file.FileUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI Agent 生成书源规则 - 对话式界面
 *
 * @author weizhanjie
 */
public class AgentSiteRuleDialog {

    private static final String TEMP_DIR = ".idea/w-reader/agent";
    private static final String TEMP_FILE_NAME = "agent-site-rule-temp.json";

    private final Project project;

    private JFrame frame;
    private JTextField baseUrlField;
    private JPasswordField apiKeyField;
    private JTextField modelField;
    private JTextField websiteUrlField;
    private JButton generateButton;
    private JButton cancelButton;
    private JPanel chatPanel;
    private JBScrollPane chatScrollPane;
    private JLabel statusLabel;
    private JButton copyButton;
    private JButton importButton;

    // 代码编辑器相关
    private EditorEx ruleEditor;
    private Document ruleDocument;
    private VirtualFile ruleVirtualFile;
    private PsiFile rulePsiFile;
    private JPanel ruleEditorContainer;

    private volatile boolean generating = false;
    private AtomicReference<SiteRuleAgent> currentAgent = new AtomicReference<>();
    private volatile String finalRuleJson;

    public AgentSiteRuleDialog(Project project) {
        this.project = project;
    }

    public void show() {
        ApplicationManager.getApplication().invokeLater(this::buildWindow);
    }

    private void buildWindow() {
        frame = new JFrame("AI Agent 生成书源规则");
        frame.setSize(950, 800);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                disposeEditor();
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
        mainPanel.setBorder(JBUI.Borders.empty(8));

        mainPanel.add(createTopPanel(), BorderLayout.NORTH);

        // 中间区域：聊天 + 生成规则显示（上下分割）
        JBSplitter splitter = new JBSplitter(true, "w-reader.agent-site-rule.splitter", 0.4f);
        splitter.setFirstComponent(createChatArea());
        splitter.setSecondComponent(createRuleDisplayArea());
        mainPanel.add(splitter, BorderLayout.CENTER);

        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        loadSavedConfig();
        addSystemMessage("欢迎使用 AI Agent 书源规则生成器。请输入小说网站地址，Agent 将自主分析网站结构并生成书源规则。");
    }

    // ==================== UI 构建 ====================

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(0, 5));

        // API 配置面板
        JPanel apiPanel = new JPanel(new GridBagLayout());
        apiPanel.setBorder(BorderFactory.createTitledBorder("API 配置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(3);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        apiPanel.add(new JLabel("Base URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        baseUrlField = new JTextField();
        baseUrlField.setToolTipText("OpenAI 格式，如 https://api.deepseek.com/v1");
        apiPanel.add(baseUrlField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        apiPanel.add(new JLabel("  API Key:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.8;
        apiKeyField = new JPasswordField(20);
        apiPanel.add(apiKeyField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        apiPanel.add(new JLabel("  Model:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.6;
        modelField = new JTextField(15);
        apiPanel.add(modelField, gbc);

        gbc.gridx = 6; gbc.weightx = 0;
        JButton saveBtn = new JButton("保存");
        saveBtn.setToolTipText("保存 API 配置到 IDE 凭证管理器");
        saveBtn.addActionListener(e -> onSaveConfig());
        apiPanel.add(saveBtn, gbc);

        topPanel.add(apiPanel, BorderLayout.NORTH);

        // 网站 URL 面板
        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        urlPanel.setBorder(BorderFactory.createTitledBorder("目标网站"));
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(new JLabel("URL:"), BorderLayout.WEST);
        websiteUrlField = new JTextField();
        websiteUrlField.setToolTipText("输入小说网站地址");
        inputPanel.add(websiteUrlField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        generateButton = new JButton("开始生成");
        cancelButton = new JButton("取消");
        cancelButton.setEnabled(false);
        btnPanel.add(generateButton);
        btnPanel.add(cancelButton);
        inputPanel.add(btnPanel, BorderLayout.EAST);
        urlPanel.add(inputPanel, BorderLayout.CENTER);

        topPanel.add(urlPanel, BorderLayout.SOUTH);

        // 事件绑定
        generateButton.addActionListener(e -> onGenerate());
        cancelButton.addActionListener(e -> onCancel());

        return topPanel;
    }

    private JScrollPane createChatArea() {
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBorder(JBUI.Borders.empty(5));

        chatScrollPane = new JBScrollPane(chatPanel);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Agent 对话"));

        return chatScrollPane;
    }

    private JPanel createRuleDisplayArea() {
        ruleEditorContainer = new JPanel(new BorderLayout());
        ruleEditorContainer.setBorder(BorderFactory.createTitledBorder("生成的书源规则"));

        // 初始化代码编辑器
        initEditor();

        return ruleEditorContainer;
    }

    private void initEditor() {
        ruleEditorContainer.add(createLoadingLabel(), BorderLayout.CENTER);
        ruleEditorContainer.revalidate();

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (frame == null || !frame.isDisplayable()) {
                    return;
                }

                // 创建磁盘临时文件
                if (ruleVirtualFile == null || !ruleVirtualFile.isValid()) {
                    ruleVirtualFile = getOrCreateTempVirtualFile("");
                }

                rulePsiFile = PsiManager.getInstance(project).findFile(ruleVirtualFile);
                if (rulePsiFile == null) {
                    throw new RuntimeException("无法获取 PSI 文件");
                }

                ruleDocument = PsiDocumentManager.getInstance(project).getDocument(rulePsiFile);
                if (ruleDocument == null) {
                    ruleDocument = EditorFactory.getInstance().createDocument("");
                }

                FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
                ruleEditor = (EditorEx) EditorFactory.getInstance().createEditor(ruleDocument, project, jsonFileType, false);

                // 编辑器配置
                EditorSettings settings = ruleEditor.getSettings();
                settings.setLineNumbersShown(true);
                settings.setFoldingOutlineShown(true);
                settings.setAutoCodeFoldingEnabled(true);
                settings.setIndentGuidesShown(true);
                settings.setLineMarkerAreaShown(true);
                settings.setCaretRowShown(true);
                settings.setUseSoftWraps(false);

                ErrorStripeEditorCustomization.ENABLED.customize(ruleEditor);

                ruleEditorContainer.removeAll();
                ruleEditorContainer.add(ruleEditor.getComponent(), BorderLayout.CENTER);
                ruleEditorContainer.revalidate();
                ruleEditorContainer.repaint();
            } catch (Exception ex) {
                ruleEditorContainer.removeAll();
                ruleEditorContainer.add(createErrorLabel(ex.getMessage()), BorderLayout.CENTER);
                ruleEditorContainer.revalidate();
            }
        });
    }

    private VirtualFile getOrCreateTempVirtualFile(String content) throws IOException {
        String tempDirPath = project.getBasePath() + "/" + TEMP_DIR;
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, TEMP_FILE_NAME);
        if (!tempFile.exists()) {
            Files.writeString(tempFile.toPath(), content, StandardCharsets.UTF_8);
        }

        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(tempFile);
        if (vf != null) {
            vf.refresh(false, false);
        } else {
            // 强制刷新目录后重试
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);
            vf = LocalFileSystem.getInstance().findFileByIoFile(tempFile);
        }

        if (vf == null) {
            throw new IOException("无法创建临时文件: " + tempFile.getAbsolutePath());
        }
        return vf;
    }

    private void disposeEditor() {
        if (ruleDocument != null) {
            WriteCommandAction.runWriteCommandAction(project, () ->
                    FileDocumentManager.getInstance().saveDocument(ruleDocument)
            );
        }
        if (ruleEditor != null && !ruleEditor.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(ruleEditor);
            ruleEditor = null;
        }
        ruleDocument = null;
        rulePsiFile = null;
    }

    private JLabel createLoadingLabel() {
        JLabel label = new JLabel("正在初始化编辑器...", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.ITALIC, 12f));
        label.setForeground(JBColor.GRAY);
        return label;
    }

    private JLabel createErrorLabel(String message) {
        JLabel label = new JLabel("编辑器初始化失败: " + message, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        label.setForeground(JBColor.RED);
        return label;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(JBUI.Borders.empty(2, 5));
        panel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        copyButton = new JButton("复制规则");
        importButton = new JButton("导入到自定义书源");
        JButton closeButton = new JButton("关闭");

        copyButton.setEnabled(false);
        importButton.setEnabled(false);

        buttonPanel.add(copyButton);
        buttonPanel.add(importButton);
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.EAST);

        copyButton.addActionListener(e -> onCopy());
        importButton.addActionListener(e -> onImport());
        closeButton.addActionListener(e -> frame.dispose());

        return panel;
    }

    // ==================== 消息添加 ====================

    private void addUserMessage(String text) {
        JPanel wrapper = createMessageWrapper(Alignment.RIGHT);
        JLabel label = new JLabel("<html><b>「你」</b> " + escapeHtml(text) + "</html>");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        label.setBorder(JBUI.Borders.empty(4, 10));
        label.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        wrapper.add(label);
        addMessageToChat(wrapper);
    }

    private void addAgentMessage(String text) {
        JPanel wrapper = createMessageWrapper(Alignment.LEFT);
        JLabel label = new JLabel("<html><b>「Agent」</b> " + escapeHtml(text) + "</html>");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        label.setBorder(JBUI.Borders.empty(4, 10));
        label.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        wrapper.add(label);
        addMessageToChat(wrapper);
    }

    private void addToolCallMessage(String toolName, String arguments) {
        JPanel wrapper = createMessageWrapper(Alignment.LEFT);
        String toolLabel = getToolDisplayName(toolName);
        String argsSummary = summarizeArgs(toolName, arguments);
        JLabel label = new JLabel("<html>🔧 <b>" + escapeHtml(toolLabel) + "</b>: " + escapeHtml(argsSummary) + "</html>");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setBorder(JBUI.Borders.empty(3, 10));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        wrapper.add(label);
        addMessageToChat(wrapper);
    }

    private void addToolResultMessage(String toolName, String result) {
        JPanel wrapper = createMessageWrapper(Alignment.LEFT);
        String resultSummary = summarizeResult(toolName, result);
        JLabel label = new JLabel("<html>✅ <b>" + escapeHtml(getToolDisplayName(toolName)) + "</b>: " + escapeHtml(resultSummary) + "</html>");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setBorder(JBUI.Borders.empty(3, 10));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        wrapper.add(label);
        addMessageToChat(wrapper);
    }

    private void addSystemMessage(String text) {
        JPanel wrapper = createMessageWrapper(Alignment.CENTER);
        JLabel label = new JLabel("<html><center>" + text + "</center></html>");
        label.setFont(label.getFont().deriveFont(Font.ITALIC, 11f));
        label.setForeground(JBColor.GRAY);
        label.setBorder(JBUI.Borders.empty(8, 10));
        wrapper.add(label);
        addMessageToChat(wrapper);
    }

    private void showRuleInDisplayArea(String jsonRule) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (ruleEditor == null || ruleEditor.isDisposed()) {
                return;
            }
            String normalized = jsonRule.replace("\r\n", "\n").replace("\r", "\n");
            WriteCommandAction.runWriteCommandAction(project, () -> {
                if (ruleDocument != null) {
                    ruleDocument.setText(normalized);
                }
            });
            ruleEditor.getCaretModel().moveToOffset(0);
            ruleEditor.getScrollingModel().scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE);
        });
    }

    // ==================== UI 辅助 ====================

    private enum Alignment { LEFT, CENTER, RIGHT }

    private JPanel createMessageWrapper(Alignment align) {
        JPanel wrapper = new JPanel(new FlowLayout(
                align == Alignment.LEFT ? FlowLayout.LEFT :
                align == Alignment.RIGHT ? FlowLayout.RIGHT : FlowLayout.CENTER,
                0, 2));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return wrapper;
    }

    private void addMessageToChat(JComponent message) {
        SwingUtilities.invokeLater(() -> {
            chatPanel.add(message);
            chatPanel.add(Box.createVerticalStrut(4));
            chatPanel.revalidate();
            chatPanel.repaint();
            // 自动滚动到底部
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String getToolDisplayName(String toolName) {
        return switch (toolName) {
            case "fetch_page" -> "获取页面";
            case "search_website" -> "搜索书籍";
            case "extract_html_elements" -> "提取元素";
            case "parse_json_path" -> "解析 JSON";
            case "validate_site_rule" -> "校验规则";
            case "complete_rule" -> "提交规则";
            default -> toolName;
        };
    }

    private String summarizeArgs(String toolName, String arguments) {
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(arguments);
            return switch (toolName) {
                case "fetch_page" -> "URL: " + node.path("url").asText();
                case "search_website" -> "关键词: " + node.path("keyword").asText();
                case "extract_html_elements" -> "选择器: " + node.path("css_selector").asText();
                case "parse_json_path" -> "路径: " + node.path("json_path").asText();
                case "validate_site_rule" -> "校验规则...";
                case "complete_rule" -> "提交最终规则";
                default -> arguments.length() > 200 ? arguments.substring(0, 200) + "..." : arguments;
            };
        } catch (Exception e) {
            return arguments.length() > 200 ? arguments.substring(0, 200) + "..." : arguments;
        }
    }

    private String summarizeResult(String toolName, String result) {
        if (result == null) return "(空结果)";
        if (result.length() > 500) {
            return result.substring(0, 500) + "... (共 " + result.length() + " 字符)";
        }
        return result;
    }

    // ==================== 事件处理 ====================

    private void loadSavedConfig() {
        CredentialService cs = CredentialService.getInstance();
        String savedBaseUrl = cs.getLlmBaseUrl();
        String savedApiKey = cs.getLlmApiKey();
        String savedModel = cs.getLlmModel();

        if (savedBaseUrl != null && !savedBaseUrl.isEmpty()) baseUrlField.setText(savedBaseUrl);
        if (savedApiKey != null && !savedApiKey.isEmpty()) apiKeyField.setText(savedApiKey);
        if (savedModel != null && !savedModel.isEmpty()) modelField.setText(savedModel);
    }

    private void onSaveConfig() {
        String baseUrl = baseUrlField.getText().trim();
        String apiKey = new String(apiKeyField.getPassword()).trim();
        String model = modelField.getText().trim();

        if (baseUrl.isEmpty() && apiKey.isEmpty() && model.isEmpty()) {
            Messages.showInfoMessage("没有可保存的配置", "提示");
            return;
        }

        CredentialService cs = CredentialService.getInstance();
        cs.saveLlmBaseUrl(baseUrl);
        cs.saveLlmApiKey(apiKey);
        cs.saveLlmModel(model);
        Messages.showInfoMessage("API 配置已保存", "提示");
    }

    /**
     * 生成按钮点击事件处理
     */
    private void onGenerate() {
        String baseUrl = baseUrlField.getText().trim();
        String apiKey = new String(apiKeyField.getPassword()).trim();
        String model = modelField.getText().trim();
        String websiteUrl = websiteUrlField.getText().trim();

        if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty() || websiteUrl.isEmpty()) {
            Messages.showInfoMessage("请填写完整的 API 配置和网站 URL", "提示");
            return;
        }

        if (!websiteUrl.startsWith("http://") && !websiteUrl.startsWith("https://")) {
            websiteUrl = "https://" + websiteUrl;
            websiteUrlField.setText(websiteUrl);
        }

        final String finalWebsiteUrl = websiteUrl;

        generating = true;
        finalRuleJson = null;
        generateButton.setEnabled(false);
        cancelButton.setEnabled(true);
        copyButton.setEnabled(false);
        importButton.setEnabled(false);

        // 清空聊天区域和规则编辑器
        chatPanel.removeAll();
        chatPanel.revalidate();
        chatPanel.repaint();
        showRuleInDisplayArea("");

        addUserMessage("请为 " + finalWebsiteUrl + " 生成书源规则");
        statusLabel.setText("Agent 正在工作...");

        SiteRuleAgent agent = new SiteRuleAgent(project, baseUrl, apiKey, model);
        currentAgent.set(agent);

        new Task.Backgroundable(project, "AI Agent 生成书源规则", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                agent.generate(finalWebsiteUrl, new AgentCallback() {
                    @Override
                    public void onMessage(String text) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                addAgentMessage(text);
                                statusLabel.setText("Agent 思考中...");
                            }
                        });
                    }

                    @Override
                    public void onToolCall(String toolName, String arguments) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                addToolCallMessage(toolName, arguments);
                                statusLabel.setText("执行: " + getToolDisplayName(toolName));
                            }
                        });
                    }

                    @Override
                    public void onToolResult(String toolName, String result) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                addToolResultMessage(toolName, result);
                                statusLabel.setText("Agent 分析中...");
                            }
                        });
                    }

                    @Override
                    public void onComplete(String siteRuleJson) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                finalRuleJson = siteRuleJson;
                                addAgentMessage("规则生成完成！请查看下方「生成的书源规则」区域。");
                                showRuleInDisplayArea(siteRuleJson);
                                statusLabel.setText("生成完成");
                                generateButton.setEnabled(true);
                                cancelButton.setEnabled(false);
                                copyButton.setEnabled(true);
                                importButton.setEnabled(true);
                                generating = false;
                                currentAgent.set(null);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                addSystemMessage("❌ " + error);
                                statusLabel.setText("生成失败");
                                generateButton.setEnabled(true);
                                cancelButton.setEnabled(false);
                                generating = false;
                                currentAgent.set(null);
                            }
                        });
                    }
                });
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (frame != null && frame.isDisplayable()) {
                        addSystemMessage("❌ 发生异常: " + error.getMessage());
                        statusLabel.setText("发生异常");
                        generateButton.setEnabled(true);
                        cancelButton.setEnabled(false);
                        generating = false;
                        currentAgent.set(null);
                    }
                });
            }
        }.queue();
    }

    private void onCancel() {
        SiteRuleAgent agent = currentAgent.get();
        if (agent != null) {
            agent.cancel();
        }
        generating = false;
        generateButton.setEnabled(true);
        cancelButton.setEnabled(false);
        statusLabel.setText("已取消");
        addSystemMessage("生成已取消");
    }

    private void onCopy() {
        if (finalRuleJson == null || finalRuleJson.isEmpty()) {
            Messages.showInfoMessage("没有可复制的规则", "提示");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(finalRuleJson), null);
        Messages.showInfoMessage("已复制到剪贴板", "提示");
    }

    private void onImport() {
        if (finalRuleJson == null || finalRuleJson.trim().isEmpty()) {
            Messages.showInfoMessage("请先生成书源规则", "提示");
            return;
        }

        CustomSiteUtil customSiteUtil = CustomSiteUtil.getInstance(project);
        try {
            customSiteUtil.parseCustomSiteRule(finalRuleJson.trim(),
                    validationResult -> {
                        String groupName = Messages.showInputDialog(
                                frame,
                                "请输入书源分组名称:",
                                "导入书源规则",
                                Messages.getQuestionIcon()
                        );
                        if (groupName == null || groupName.trim().isEmpty()) return;

                        groupName = groupName.trim();
                        List<SiteBean> siteBeans = validationResult.getBeanList();

                        SiteRuleService siteRuleService = SiteRuleService.getInstance();

                        Map<String, List<SiteBean>> siteMap = siteRuleService.getCustomSiteRuleGroupMap();
                        if (siteMap == null || siteMap.isEmpty()) {
                            siteMap = customSiteUtil.getSiteMap();
                        }
                        siteMap.put(groupName, siteBeans);
                        siteRuleService.setCustomSiteRuleGroupMap(siteMap);

                        Map<String, String> originalStrMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
                        if (originalStrMap == null || originalStrMap.isEmpty()) {
                            originalStrMap = new HashMap<>();
                            String defaultSiteRuleJson = FileUtil.readResourcesJsonStr(CustomSiteUtil.DEFAULT_SITE_RULE_PATH);
                            originalStrMap.put(ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY, defaultSiteRuleJson);
                        }
                        originalStrMap.put(groupName, finalRuleJson.trim());
                        siteRuleService.setCustomSiteRuleOriginalStrMap(originalStrMap);

                        Messages.showInfoMessage("导入成功！书源分组 \"" + groupName + "\" 已添加。", "提示");
                    },
                    null
            );
        } catch (IllegalArgumentException e) {
            Messages.showErrorDialog("JSON 格式校验失败:\n" + e.getMessage(), "导入失败");
        } catch (Exception e) {
            Messages.showErrorDialog("导入失败: " + e.getMessage(), "错误");
        }
    }
}
