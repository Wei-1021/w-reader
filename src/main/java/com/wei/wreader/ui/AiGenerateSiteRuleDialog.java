package com.wei.wreader.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBSplitter;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.model.SiteBean;
import com.wei.wreader.service.CredentialService;
import com.wei.wreader.service.SiteRuleGenerator;
import com.wei.wreader.service.SiteRuleService;
import com.wei.wreader.util.CustomSiteUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 生成书源规则对话框
 */
public class AiGenerateSiteRuleDialog {

    private final Project project;
    private JFrame frame;
    private JTextField baseUrlField;
    private JPasswordField apiKeyField;
    private JTextField modelField;
    private JTextField websiteUrlField;
    private JButton generateButton;
    private JButton cancelButton;
    private JTextArea resultArea;
    private JLabel statusLabel;
    private JButton copyButton;
    private JButton importButton;

    private volatile boolean generating = false;

    public AiGenerateSiteRuleDialog(Project project) {
        this.project = project;
    }

    public void show() {
        ApplicationManager.getApplication().invokeLater(this::buildWindow);
    }

    private void buildWindow() {
        frame = new JFrame("AI生成书源规则");
        frame.setSize(900, 700);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
        mainPanel.setBorder(JBUI.Borders.empty(8));

        mainPanel.add(createTopPanel(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));
        centerPanel.add(createWebsitePanel(), BorderLayout.NORTH);
        centerPanel.add(createResultPanel(), BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        loadSavedConfig();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("API配置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Base URL
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Base URL (OpenAI 格式) :"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        baseUrlField = new JTextField();
        baseUrlField.setToolTipText("例如: https://api.deepseek.com/v1 或 https://api.openai.com/v1");
        panel.add(baseUrlField, gbc);

        // API Key
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("API Key:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        apiKeyField = new JPasswordField();
        panel.add(apiKeyField, gbc);

        // Model
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Model:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        modelField = new JTextField();
        modelField.setToolTipText("例如: deepseek-v4-flash 或 gpt-4o-mini");
        panel.add(modelField, gbc);

        // Save button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JButton saveConfigButton = new JButton("保存API配置");
        saveConfigButton.setToolTipText("将 Base URL、API Key、Model 安全保存到 IDE 凭证管理器");
        saveConfigButton.addActionListener(e -> onSaveConfig());
        panel.add(saveConfigButton, gbc);

        return panel;
    }

    private JPanel createWebsitePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createTitledBorder("网站地址"));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(new JLabel("URL:"), BorderLayout.WEST);
        websiteUrlField = new JTextField();
        websiteUrlField.setToolTipText("输入小说网站地址，例如: https://www.example.com");
        inputPanel.add(websiteUrlField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        generateButton = new JButton("生成");
        cancelButton = new JButton("取消");
        cancelButton.setEnabled(false);
        buttonPanel.add(generateButton);
        buttonPanel.add(cancelButton);

        inputPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.CENTER);

        return panel;
    }

    private JScrollPane createResultPanel() {
        resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultArea.setEditable(true);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        return new JScrollPane(resultArea);
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(JBUI.Borders.empty(2, 5));
        panel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        copyButton = new JButton("复制");
        importButton = new JButton("导入到自定义书源");
        JButton closeButton = new JButton("关闭");

        buttonPanel.add(copyButton);
        buttonPanel.add(importButton);
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.EAST);

        // 事件绑定
        generateButton.addActionListener(e -> onGenerate());
        cancelButton.addActionListener(e -> onCancel());
        copyButton.addActionListener(e -> onCopy());
        importButton.addActionListener(e -> onImport());
        closeButton.addActionListener(e -> frame.dispose());

        return panel;
    }

    private void loadSavedConfig() {
        CredentialService cs = CredentialService.getInstance();
        String savedBaseUrl = cs.getLlmBaseUrl();
        String savedApiKey = cs.getLlmApiKey();
        String savedModel = cs.getLlmModel();

        if (savedBaseUrl != null && !savedBaseUrl.isEmpty()) {
            baseUrlField.setText(savedBaseUrl);
        }
        if (savedApiKey != null && !savedApiKey.isEmpty()) {
            apiKeyField.setText(savedApiKey);
        }
        if (savedModel != null && !savedModel.isEmpty()) {
            modelField.setText(savedModel);
        }
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

        Messages.showInfoMessage("API配置已保存", "提示");
    }

    private void onGenerate() {
        String baseUrl = baseUrlField.getText().trim();
        String apiKey = new String(apiKeyField.getPassword()).trim();
        String model = modelField.getText().trim();
        String websiteUrl = websiteUrlField.getText().trim();

        if (baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty() || websiteUrl.isEmpty()) {
            Messages.showInfoMessage("请填写完整的API配置和网站URL", "提示");
            return;
        }

        // 确保 URL 有协议前缀
        if (!websiteUrl.startsWith("http://") && !websiteUrl.startsWith("https://")) {
            websiteUrl = "https://" + websiteUrl;
            websiteUrlField.setText(websiteUrl);
        }

        final String finalWebsiteUrl = websiteUrl;
        final String finalBaseUrl = baseUrl;
        final String finalApiKey = apiKey;
        final String finalModel = model;

        generating = true;
        generateButton.setEnabled(false);
        cancelButton.setEnabled(true);
        resultArea.setText("");
        statusLabel.setText("准备中...");

        SiteRuleGenerator generator = new SiteRuleGenerator(project);

        new Task.Backgroundable(project, "AI生成书源规则", true) {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                generator.generate(finalWebsiteUrl, finalBaseUrl, finalApiKey, finalModel,
                        // progress callback
                        progress -> ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                statusLabel.setText(progress);
                            }
                        }),
                        // success callback
                        json -> ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                resultArea.setText(json);
                                resultArea.setCaretPosition(0);
                                statusLabel.setText("生成完成");
                                generateButton.setEnabled(true);
                                cancelButton.setEnabled(false);
                                generating = false;
                            }
                        }),
                        // error callback
                        error -> ApplicationManager.getApplication().invokeLater(() -> {
                            if (frame != null && frame.isDisplayable()) {
                                statusLabel.setText("生成失败: " + error);
                                Messages.showErrorDialog(error, "生成失败");
                                generateButton.setEnabled(true);
                                cancelButton.setEnabled(false);
                                generating = false;
                            }
                        })
                );
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (frame != null && frame.isDisplayable()) {
                        statusLabel.setText("发生异常");
                        Messages.showErrorDialog(error.getMessage(), "错误");
                        generateButton.setEnabled(true);
                        cancelButton.setEnabled(false);
                        generating = false;
                    }
                });
            }
        }.queue();
    }

    private void onCancel() {
        generating = false;
        generateButton.setEnabled(true);
        cancelButton.setEnabled(false);
        statusLabel.setText("已取消");
    }

    private void onCopy() {
        String text = resultArea.getText();
        if (text == null || text.isEmpty()) {
            Messages.showInfoMessage("没有可复制的内容", "提示");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
        Messages.showInfoMessage("已复制到剪贴板", "提示");
    }

    private void onImport() {
        String jsonText = resultArea.getText();
        if (jsonText == null || jsonText.trim().isEmpty()) {
            Messages.showInfoMessage("请先生成书源规则", "提示");
            return;
        }

        CustomSiteUtil customSiteUtil = CustomSiteUtil.getInstance(project);

        try {
            customSiteUtil.parseCustomSiteRule(jsonText.trim(),
                    validationResult -> {
                        String groupName = Messages.showInputDialog(
                                frame,
                                "请输入书源分组名称:",
                                "导入书源规则",
                                Messages.getQuestionIcon()
                        );

                        if (groupName == null || groupName.trim().isEmpty()) {
                            return;
                        }

                        groupName = groupName.trim();

                        List<SiteBean> siteBeans = validationResult.getBeanList();
                        SiteRuleService siteRuleService = SiteRuleService.getInstance();

                        // 保存到规则组
                        Map<String, List<SiteBean>> siteMap = siteRuleService.getCustomSiteRuleGroupMap();
                        if (siteMap == null) {
                            siteMap = new HashMap<>();
                        }
                        siteMap.put(groupName, siteBeans);
                        siteRuleService.setCustomSiteRuleGroupMap(siteMap);

                        // 保存原始JSON
                        Map<String, String> originalStrMap = siteRuleService.getCustomSiteRuleOriginalStrMap();
                        if (originalStrMap == null) {
                            originalStrMap = new HashMap<>();
                        }
                        originalStrMap.put(groupName, jsonText.trim());
                        siteRuleService.setCustomSiteRuleOriginalStrMap(originalStrMap);

                        Messages.showInfoMessage("导入成功！书源分组 \"" + groupName + "\" 已添加。", "提示");
                    },
                    null
            );
        } catch (IllegalArgumentException e) {
            Messages.showErrorDialog("JSON格式校验失败:\n" + e.getMessage(), "导入失败");
        } catch (Exception e) {
            Messages.showErrorDialog("导入失败: " + e.getMessage(), "错误");
        }
    }
}
