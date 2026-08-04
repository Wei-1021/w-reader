package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.ui.MessageDialogUtil;

import com.wei.wreader.util.ui.GroupedComboBoxs.CharsetGroupComboBox;
import com.wei.wreader.util.ui.GroupedComboBoxs.OptionItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 加载本地文件
 *
 * @author weizhanjie
 */
public class LoadLocalFileAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);

        // ========== 主面板 ==========
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // ========== 分组1: 文件编码 ==========
        JPanel encodingPanel = new JPanel(new GridBagLayout());
        encodingPanel.setBorder(BorderFactory.createTitledBorder("文件编码"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // 字符集
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel charsetLabel = new JLabel("字符集:");
        charsetLabel.setPreferredSize(new Dimension(70, 28));
        encodingPanel.add(charsetLabel, gbc);

        CharsetGroupComboBox charsetGroupComboBox = new CharsetGroupComboBox();
        ComboBox<String> charsetComboBox = charsetGroupComboBox.buildComboBox();
        charsetGroupComboBox.setSelectedItem(settings.getCharset());
        charsetComboBox.addActionListener(e1 -> {
            OptionItem selectedItem = (OptionItem) charsetComboBox.getSelectedItem();
            if (selectedItem != null) {
                settings.setCharset(selectedItem.getText());
            }
            cacheService.setSettings(settings);
        });
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        encodingPanel.add(charsetComboBox, gbc);

        // 字符集提示
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JLabel charsetTipLabel = new JLabel("字符集不正确会导致内容无法加载或乱码");
        charsetTipLabel.setFont(charsetTipLabel.getFont().deriveFont(Font.PLAIN, 11f));
        charsetTipLabel.setForeground(UIManager.getColor("Component.infoForeground"));
        encodingPanel.add(charsetTipLabel, gbc);

        // 显示图片
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel isShowImgLabel = new JLabel("显示图片:");
        isShowImgLabel.setPreferredSize(new Dimension(70, 28));
        encodingPanel.add(isShowImgLabel, gbc);

        JBCheckBox isShowImgCheckBox = new JBCheckBox("仅EPUB文件");
        isShowImgCheckBox.setSelected(settings.isShowLocalImg());
        isShowImgCheckBox.addActionListener(e1 -> {
            settings.setShowLocalImg(isShowImgCheckBox.isSelected());
            cacheService.setSettings(settings);
        });
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        encodingPanel.add(isShowImgCheckBox, gbc);

        // ========== 分组2: TXT文件解析 ==========
        JPanel txtPanel = new JPanel(new GridBagLayout());
        txtPanel.setBorder(BorderFactory.createTitledBorder("TXT文件解析"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel textRegexLabel = new JLabel("章节正则:");
        textRegexLabel.setPreferredSize(new Dimension(70, 28));
        txtPanel.add(textRegexLabel, gbc);

        JTextField textRegexTextField = new JTextField(ConstUtil.TEXT_FILE_DIR_REGEX, 30);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtPanel.add(textRegexTextField, gbc);

        // ========== 分组3: 文本内容规则 ==========
        JPanel rulePanel = new JPanel(new GridBagLayout());
        rulePanel.setBorder(BorderFactory.createTitledBorder("文本内容规则（可选）"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        JLabel contentRuleLabel = new JLabel("替换规则:");
        contentRuleLabel.setPreferredSize(new Dimension(70, 28));
        rulePanel.add(contentRuleLabel, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        String savedRules = settings.getLocalFileContentRules() != null ? settings.getLocalFileContentRules() : "";
        JTextArea contentRuleTextArea = new JTextArea(savedRules, 4, 30);
        contentRuleTextArea.setLineWrap(true);
        contentRuleTextArea.setWrapStyleWord(true);
        JBScrollPane ruleScrollPane = new JBScrollPane(contentRuleTextArea);
        ruleScrollPane.setPreferredSize(new Dimension(0, 80));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        rulePanel.add(ruleScrollPane, gbc);

        JButton contentRuleHelpBtn = new JButton("?");
        contentRuleHelpBtn.setMargin(JBUI.insets(2, 6));
        contentRuleHelpBtn.setPreferredSize(new Dimension(28, 28));
        contentRuleHelpBtn.setMaximumSize(new Dimension(28, 28));
        contentRuleHelpBtn.setToolTipText("查看规则格式帮助");
        contentRuleHelpBtn.addActionListener(e1 -> {
            JOptionPane.showMessageDialog(null, ConstUtil.CONTENT_RULE_HELP,
                    "文本内容规则帮助", JOptionPane.INFORMATION_MESSAGE);
        });
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        rulePanel.add(contentRuleHelpBtn, gbc);

        // 规则格式提示
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel contentRuleTipLabel = new JLabel("格式: s/正则/替换/标志    每行一条规则");
        contentRuleTipLabel.setFont(contentRuleTipLabel.getFont().deriveFont(Font.PLAIN, 11f));
        contentRuleTipLabel.setForeground(UIManager.getColor("Component.infoForeground"));
        rulePanel.add(contentRuleTipLabel, gbc);
        gbc.gridwidth = 1;

        // ========== 组装主面板 ==========
        mainPanel.add(encodingPanel);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(txtPanel);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(rulePanel);

        // ========== 显示对话框 ==========
        MessageDialogUtil.showMessageDialog(project, "加载本地文件", mainPanel, () -> {
            // 保存内容规则设置
            settings.setLocalFileContentRules(contentRuleTextArea.getText());
            cacheService.setSettings(settings);
            // 打开文件选择器，并处理文件
            orchestrator.loadLocalFile(textRegexTextField.getText(), contentRuleTextArea.getText());
        });
    }
}
