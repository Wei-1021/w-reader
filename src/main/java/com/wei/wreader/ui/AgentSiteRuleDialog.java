package com.wei.wreader.ui;

import com.intellij.formatting.Alignment;
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
import com.intellij.openapi.ui.ComboBox;
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
import com.wei.wreader.agent.CLIAgentRunner;
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
    private ComboBox<String> aiModeComboBox;
    private JPanel apiConfigPanel;
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

        // 中间区域：聊天/输出 + 规则显示（上下分割）
        JBSplitter mainSplitter = new JBSplitter(true, "w-reader.agent-site-rule.main-splitter", 0.5f);
        mainSplitter.setFirstComponent(createChatArea());
        mainSplitter.setSecondComponent(createRuleDisplayArea());
        mainPanel.add(mainSplitter, BorderLayout.CENTER);

        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        loadSavedConfig();
        addSystemMessage("欢迎使用 AI Agent 书源规则生成器。请输入小说网站地址，Agent 将自主分析网站结构并生成书源规则。");
    }

    // ==================== UI 构建 ====================

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(0, 5));

        // AI 模式选择面板
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        modePanel.add(new JLabel("AI 模式:"));
        aiModeComboBox = new ComboBox<>(new String[]{
                "API 接口（自定义 OpenAI 兼容）",
                CLIAgentRunner.CLIType.CLAUDE.displayName + "（本地 CLI）",
                CLIAgentRunner.CLIType.OPENCODE.displayName + "（本地 CLI）",
                CLIAgentRunner.CLIType.MIMOCODE.displayName + "（本地 CLI）"
        });
        aiModeComboBox.setToolTipText("选择 AI 生成模式：API 接口需要配置 Base URL/Key/Model，CLI 模式需要本地安装对应工具");
        aiModeComboBox.addActionListener(e -> onAiModeChanged());
        modePanel.add(aiModeComboBox);
        topPanel.add(modePanel, BorderLayout.NORTH);

        // API 配置面板（可隐藏）
        apiConfigPanel = new JPanel(new GridBagLayout());
        apiConfigPanel.setBorder(BorderFactory.createTitledBorder("API 配置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(3);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        apiConfigPanel.add(new JLabel("Base URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        baseUrlField = new JTextField();
        baseUrlField.setToolTipText("OpenAI 格式，如 https://api.deepseek.com/v1");
        apiConfigPanel.add(baseUrlField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        apiConfigPanel.add(new JLabel("  API Key:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.8;
        apiKeyField = new JPasswordField(20);
        apiConfigPanel.add(apiKeyField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        apiConfigPanel.add(new JLabel("  Model:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.6;
        modelField = new JTextField(15);
        apiConfigPanel.add(modelField, gbc);

        gbc.gridx = 6; gbc.weightx = 0;
        JButton saveBtn = new JButton("保存");
        saveBtn.setToolTipText("保存 API 配置到 IDE 凭证管理器");
        saveBtn.addActionListener(e -> onSaveConfig());
        apiConfigPanel.add(saveBtn, gbc);

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

        // 将 API 配置和 URL 面板组合到 centerPanel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(apiConfigPanel);
        centerPanel.add(urlPanel);

        topPanel.add(centerPanel, BorderLayout.CENTER);

        // 事件绑定
        generateButton.addActionListener(e -> onGenerate());
        cancelButton.addActionListener(e -> onCancel());

        return topPanel;
    }

    /**
     * AI 模式切换事件
     */
    private void onAiModeChanged() {
        int mode = aiModeComboBox.getSelectedIndex();
        // API 模式（index=0）显示 API 配置面板，CLI 模式隐藏
        apiConfigPanel.setVisible(mode == 0);
        apiConfigPanel.revalidate();
        apiConfigPanel.repaint();
    }

    /**
     * 根据下拉框索引获取 CLI 类型
     * 0=API, 1=Claude Code, 2=OpenCode, 3=mimo
     */
    private static CLIAgentRunner.CLIType getCLIType(int aiMode) {
        return switch (aiMode) {
            case 1 -> CLIAgentRunner.CLIType.CLAUDE;
            case 2 -> CLIAgentRunner.CLIType.OPENCODE;
            case 3 -> CLIAgentRunner.CLIType.MIMOCODE;
            default -> CLIAgentRunner.CLIType.CLAUDE;
        };
    }

    private JScrollPane createChatArea() {
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBorder(JBUI.Borders.empty(5));

        chatScrollPane = new JBScrollPane(chatPanel);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("对话 / 输出"));

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

    /**
     * 添加用户消息
     * @param text
     */
    private void addUserMessage(String text) {
        JPanel wrapper = createMessageWrapper(Component.LEFT_ALIGNMENT);
        JTextArea area = createMessageArea("「你」 " + text, 12f, Font.PLAIN);
        area.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(area);
        // 右侧填充，让消息靠左
        wrapper.add(Box.createHorizontalGlue());
        addMessageToChat(wrapper);
    }

    /**
     * 添加代理消息
     * @param text
     */
    private void addAgentMessage(String text) {
        JPanel wrapper = createMessageWrapper(Component.RIGHT_ALIGNMENT);
        JTextArea area = createMessageArea("「Agent」 " + text, 12f, Font.PLAIN);
        area.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        area.setAlignmentX(Component.RIGHT_ALIGNMENT);
        wrapper.add(Box.createHorizontalGlue());
        // 左侧填充，让消息靠右
        wrapper.add(area);
        addMessageToChat(wrapper);
    }

    /**
     * 添加代理消息
     * @param agentName 智能体名称
     * @param text 代理消息内容
     */
    public void addAgentMessage(String agentName, String text) {
        JPanel wrapper = createMessageWrapper(Component.RIGHT_ALIGNMENT);
        JTextArea area = createMessageArea("「" + agentName + "」 " + text, 12f, Font.PLAIN);
        area.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        area.setAlignmentX(Component.RIGHT_ALIGNMENT);
        wrapper.add(Box.createHorizontalGlue());
        wrapper.add(area);
        addMessageToChat(wrapper);
    }

    /**
     * 添加工具调用消息
     * @param toolName
     * @param arguments
     */
    private void addToolCallMessage(String toolName, String arguments) {
        JPanel wrapper = createMessageWrapper(Component.RIGHT_ALIGNMENT);
        String toolLabel = getToolDisplayName(toolName);
        String argsSummary = summarizeArgs(toolName, arguments);
        JTextArea area = createMessageArea(">> " + toolLabel + ": " + argsSummary, 12f, Font.PLAIN);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        area.setAlignmentX(Component.RIGHT_ALIGNMENT);
        wrapper.add(Box.createHorizontalGlue());
        wrapper.add(area);
        addMessageToChat(wrapper);
    }

    /**
     * 添加工具结果消息
     * @param toolName
     * @param result
     */
    private void addToolResultMessage(String toolName, String result) {
        JPanel wrapper = createMessageWrapper(Component.RIGHT_ALIGNMENT);
        String resultSummary = summarizeResult(toolName, result);
        JTextArea area = createMessageArea("OK " + getToolDisplayName(toolName) + ": " + resultSummary, 12f, Font.PLAIN);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        area.setAlignmentX(Component.RIGHT_ALIGNMENT);
        wrapper.add(Box.createHorizontalGlue());
        wrapper.add(area);
        addMessageToChat(wrapper);
    }

    /**
     * 添加系统消息
     * @param text
     */
    private void addSystemMessage(String text) {
        JPanel wrapper = createMessageWrapper(Component.RIGHT_ALIGNMENT);
        JTextArea area = createMessageArea(text, 12f, Font.ITALIC);
        area.setForeground(JBColor.GRAY);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        area.setAlignmentX(Component.RIGHT_ALIGNMENT);
        wrapper.add(Box.createHorizontalGlue());
        wrapper.add(area);
        addMessageToChat(wrapper);
    }

    /**
     * 向聊天区域追加 CLI 输出
     */
    private void addCLIOutputMessage(String text, String cliName) {
        if (text == null || text.isEmpty() || text.startsWith("system")) {
            return;
        }

        JPanel wrapper = createMessageWrapper(Component.RIGHT_ALIGNMENT);
        JTextArea area = createMessageArea(">>【" + cliName + "】 " + text, 12f, Font.PLAIN);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        area.setForeground(JBColor.BLACK);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        area.setAlignmentX(Component.RIGHT_ALIGNMENT);
        wrapper.add(area);
        wrapper.add(Box.createHorizontalGlue());
        addMessageToChat(wrapper);
    }

    /**
     * 创建消息文本区域（不可编辑，可选择复制，自动换行）
     */
    private JTextArea createMessageArea(String text, float fontSize, int style) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFocusable(true);
        area.setFont(area.getFont().deriveFont(style, fontSize));
        area.setBorder(JBUI.Borders.empty(4, 10));
        area.setMargin(JBUI.emptyInsets());
        return area;
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


    private JPanel createMessageWrapper(float align) {
        // 使用 BoxLayout 以支持 alignmentX 对齐
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(align);
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

        // 初始化 AI 模式面板可见性
        onAiModeChanged();
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
        String websiteUrl = websiteUrlField.getText().trim();

        if (websiteUrl.isEmpty()) {
            Messages.showInfoMessage("请输入网站 URL", "提示");
            return;
        }

        if (!websiteUrl.startsWith("http://") && !websiteUrl.startsWith("https://")) {
            websiteUrl = "https://" + websiteUrl;
            websiteUrlField.setText(websiteUrl);
        }

        final String finalWebsiteUrl = websiteUrl;
        final int aiMode = aiModeComboBox.getSelectedIndex();
        final boolean isCLIMode = aiMode > 0;

        // API 模式需要验证 API 配置
        if (aiMode == 0) {
            String baseUrl = baseUrlField.getText().trim();
            String apiKey = new String(apiKeyField.getPassword()).trim();
            String model = modelField.getText().trim();
            if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
                Messages.showInfoMessage("请填写完整的 API 配置（base URL / API key / model）", "提示");
                return;
            }
        } else {
            // CLI 模式检测 CLI 是否可用
            CLIAgentRunner.CLIType cliType = getCLIType(aiMode);
            if (!CLIAgentRunner.detectCLI(cliType)) {
                Messages.showInfoMessage(cliType.displayName + " 未安装或不可用，请确保 "
                        + cliType.command + " 命令可在终端中执行", "提示");
                return;
            }
        }

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
        statusLabel.setText(isCLIMode ? "CLI Agent 正在工作..." : "Agent 正在工作...");

        SiteRuleAgent agent;
        if (aiMode == 0) {
            String baseUrl = baseUrlField.getText().trim();
            String apiKey = new String(apiKeyField.getPassword()).trim();
            String model = modelField.getText().trim();
            agent = new SiteRuleAgent(project, baseUrl, apiKey, model);
        } else {
            agent = new SiteRuleAgent(project, "", "", "");
        }
        currentAgent.set(agent);

        // 构建回调（根据模式传递参数）
        AgentCallback agentCallback = createAgentCallback(isCLIMode);

        new Task.Backgroundable(project, "AI Agent 生成书源规则", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                if (aiMode == 0) {
                    agent.generate(finalWebsiteUrl, agentCallback);
                } else {
                    CLIAgentRunner.CLIType cliType = getCLIType(aiMode);
                    agent.generateWithCLI(cliType, finalWebsiteUrl, agentCallback);
                }
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

    /**
     * 创建 Agent 回调（API 和 CLI 模式共用）
     */
    private AgentCallback createAgentCallback(boolean isCLIMode) {
        return new AgentCallback() {
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
                        if (isCLIMode) {
                            // CLI 模式：直接显示在聊天区域
                            addCLIOutputMessage(arguments, toolName);
                            statusLabel.setText("CLI 输出中...");
                        } else {
                            // API 模式：显示在聊天区域
                            addToolCallMessage(toolName, arguments);
                            statusLabel.setText("执行: " + getToolDisplayName(toolName));
                        }
                    }
                });
            }

            @Override
            public void onToolResult(String toolName, String result) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (frame != null && frame.isDisplayable()) {
                        if (isCLIMode) {
                            // CLI 模式：不显示工具结果（已经在 onToolCall 中显示了）
                            statusLabel.setText("CLI 分析中...");
                        } else {
                            // API 模式：显示在聊天区域
                            addToolResultMessage(toolName, result);
                            statusLabel.setText("Agent 分析中...");
                        }
                    }
                });
            }

            @Override
            public void onComplete(String siteRuleJson) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (frame != null && frame.isDisplayable()) {
                        finalRuleJson = siteRuleJson;
                        addAgentMessage("✓ 规则生成完成！请查看下方「生成的书源规则」区域。");
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
        };
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
