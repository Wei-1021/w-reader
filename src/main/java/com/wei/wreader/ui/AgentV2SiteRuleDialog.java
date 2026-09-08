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
import com.wei.wreader.agent.v2.core.AgentResult;
import com.wei.wreader.agent.v2.event.AgentEvent;
import com.wei.wreader.agent.v2.runtime.AgentService;
import com.wei.wreader.agent.v2.siterule.SiteRuleAgent;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.CredentialService;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.CustomSiteUtil;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.file.FileUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

/**
 * AI Agent v2 生成书源规则 - 对话式界面
 * <p>
 * 使用新的 Agent v2 架构
 *
 * @author weizhanjie
 */
public class AgentV2SiteRuleDialog {

    private static final String TEMP_DIR = ".idea/w-reader/agent";
    private static final String TEMP_FILE_NAME = "agent-site-rule-temp.json";

    private final Project project;

    private JFrame frame;
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
    private AtomicReference<String> currentSessionId = new AtomicReference<>();
    private final AtomicReference<String> activeRunToken = new AtomicReference<>();
    private volatile String finalRuleJson;

    public AgentV2SiteRuleDialog(Project project) {
        this.project = project;
    }

    public void show() {
        ApplicationManager.getApplication().invokeLater(this::buildWindow);
    }

    private void buildWindow() {
        frame = new JFrame("AI Agent v2 生成书源规则");
        frame.setSize(950, 800);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                disposeEditor();
                onCancel();
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
        mainPanel.setBorder(JBUI.Borders.empty(8));

        mainPanel.add(createTopPanel(), BorderLayout.NORTH);

        // 中间区域：聊天/输出 + 规则显示（上下分割）
        JBSplitter mainSplitter = new JBSplitter(true, "w-reader.agent-v2-site-rule.main-splitter", 0.5f);
        mainSplitter.setFirstComponent(createChatArea());
        mainSplitter.setSecondComponent(createRuleDisplayArea());
        mainPanel.add(mainSplitter, BorderLayout.CENTER);

        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        // 检查 API 配置
        checkApiConfig();

        addSystemMessage("欢迎使用 AI Agent v2 书源规则生成器。\n" +
                "请输入小说网站地址，Agent 将自主分析网站结构并生成书源规则。\n\n" +
                "提示：请先在 Settings → W-Reader 中配置 LLM API Key。");
    }

    // ==================== UI 构建 ====================

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(0, 5));

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

        topPanel.add(urlPanel, BorderLayout.NORTH);

        // API 配置提示
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel configLabel = new JLabel("API 配置: 请在 Settings → W-Reader 中配置");
        configLabel.setForeground(JBColor.GRAY);
        configPanel.add(configLabel);
        topPanel.add(configPanel, BorderLayout.CENTER);

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
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JTextArea area = createMessageArea("「你」 " + text, 12f, Font.PLAIN);
        area.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        wrapper.add(area, BorderLayout.WEST);

        addMessageToChat(wrapper);
    }

    private void addAgentMessage(String text) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JTextArea area = createMessageArea("「Agent」 " + text, 12f, Font.PLAIN);
        area.setMaximumSize(new Dimension(650, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        wrapper.add(area, BorderLayout.EAST);

        addMessageToChat(wrapper);
    }

    private void addToolCallMessage(String toolName, String arguments) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        String toolLabel = getToolDisplayName(toolName);
        String argsSummary = summarizeArgs(toolName, arguments);
        JTextArea area = createMessageArea(">> " + toolLabel + ": " + argsSummary, 11f, Font.PLAIN);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        wrapper.add(area, BorderLayout.WEST);

        addMessageToChat(wrapper);
    }

    private void addToolResultMessage(String toolName, String result) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        String resultSummary = summarizeResult(toolName, result);
        JTextArea area = createMessageArea("OK " + getToolDisplayName(toolName) + ": " + resultSummary, 11f, Font.PLAIN);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        wrapper.add(area, BorderLayout.EAST);

        addMessageToChat(wrapper);
    }

    private void addSystemMessage(String text) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JTextArea area = createMessageArea(text, 11f, Font.ITALIC);
        area.setForeground(JBColor.GRAY);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        area.setMinimumSize(new Dimension(650, 20));
        wrapper.add(area, BorderLayout.CENTER);

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

    private void checkApiConfig() {
        CredentialService cs = CredentialService.getInstance();
        String apiKey = cs.getLlmApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            statusLabel.setText("⚠ 请先配置 API Key");
            statusLabel.setForeground(JBColor.YELLOW.darker());
        } else {
            statusLabel.setText("就绪");
            statusLabel.setForeground(JBColor.GREEN.darker());
        }
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

        // 检查 API 配置
        CredentialService cs = CredentialService.getInstance();
        String apiKey = cs.getLlmApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Messages.showInfoMessage("请先在 Settings → W-Reader 中配置 LLM API Key", "提示");
            return;
        }

        final String finalWebsiteUrl = websiteUrl;
        final String runToken = UUID.randomUUID().toString();
        activeRunToken.set(runToken);

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
        statusLabel.setForeground(JBColor.BLUE);

        try {
            AgentService service = AgentService.getInstance(project);
            String sessionId = service.startAgent(
                    SiteRuleAgent.ID,
                    "请为这个小说网站生成书源规则: " + finalWebsiteUrl,
                    event -> handleAgentEvent(runToken, event),
                    result -> handleAgentResult(runToken, result)
            );
            currentSessionId.set(sessionId);
        } catch (Exception e) {
            addSystemMessage("❌ 无法启动 Agent: " + e.getMessage());
            statusLabel.setText("启动失败");
            statusLabel.setForeground(JBColor.RED);
            generateButton.setEnabled(true);
            cancelButton.setEnabled(false);
            generating = false;
        }
    }

    /**
     * 处理 Agent 事件
     */
    private void handleAgentEvent(String runToken, AgentEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!runToken.equals(activeRunToken.get()) || frame == null || !frame.isDisplayable()) {
                return;
            }

            if (event instanceof AgentEvent.AssistantMessage msg) {
                addAgentMessage(msg.content());
                statusLabel.setText("Agent 思考中...");
            } else if (event instanceof AgentEvent.ToolCallStarted tc) {
                addToolCallMessage(tc.name(), tc.arguments().toString());
                statusLabel.setText("执行: " + getToolDisplayName(tc.name()));
            } else if (event instanceof AgentEvent.ToolCallFinished tc) {
                if (tc.result().isSuccess()) {
                    addToolResultMessage(tc.name(), tc.result().content());
                } else {
                    addSystemMessage("❌ 工具执行失败: " + tc.result().content());
                }
                statusLabel.setText("Agent 分析中...");
            }
        });
    }

    /**
     * 处理 Agent 结果
     */
    private void handleAgentResult(String runToken, AgentResult result) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!runToken.equals(activeRunToken.get()) || frame == null || !frame.isDisplayable()) {
                return;
            }

            if (result.isSuccess()) {
                finalRuleJson = result.content();
                addAgentMessage("✓ 规则生成完成！请查看下方「生成的书源规则」区域。");
                showRuleInDisplayArea(result.content());
                statusLabel.setText("生成完成");
                statusLabel.setForeground(JBColor.GREEN);
                copyButton.setEnabled(true);
                importButton.setEnabled(true);
            } else {
                addSystemMessage("❌ " + result.errorMessage());
                statusLabel.setText("生成失败");
                statusLabel.setForeground(JBColor.RED);
            }

            generateButton.setEnabled(true);
            cancelButton.setEnabled(false);
            generating = false;
            currentSessionId.set(null);
            activeRunToken.compareAndSet(runToken, null);
        });
    }

    private void onCancel() {
        String sessionId = currentSessionId.get();
        if (sessionId != null) {
            // 取消 Agent 执行
            AgentService service = AgentService.getInstance(project);
            if (service != null) {
                service.cancelAgent(sessionId);
            }
        }

        currentSessionId.set(null);
        activeRunToken.set(null);

        generating = false;
        generateButton.setEnabled(true);
        cancelButton.setEnabled(false);
        statusLabel.setText("已取消");
        statusLabel.setForeground(JBColor.GRAY);
        addSystemMessage("生成已取消");
    }

    private void onCopy() {
        String ruleJson = getCurrentRuleJson();
        if (ruleJson == null || ruleJson.isEmpty()) {
            Messages.showInfoMessage("没有可复制的规则", "提示");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(ruleJson), null);
        Messages.showInfoMessage("已复制到剪贴板", "提示");
    }

    private void onImport() {
        String ruleJson = getCurrentRuleJson();
        if (ruleJson == null || ruleJson.trim().isEmpty()) {
            Messages.showInfoMessage("请先生成书源规则", "提示");
            return;
        }

        CustomSiteUtil customSiteUtil = CustomSiteUtil.getInstance(project);
        try {
            customSiteUtil.parseCustomSiteRule(ruleJson.trim(),
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
                        originalStrMap.put(groupName, ruleJson.trim());
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

    private String getCurrentRuleJson() {
        if (ruleDocument != null) {
            return ruleDocument.getText();
        }
        return finalRuleJson;
    }
}
