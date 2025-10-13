package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.wei.wreader.pojo.SiteBean;
import com.wei.wreader.utils.CustomSiteUtil;
import com.wei.wreader.utils.data.JsonValidator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 自定义书源规则Action
 * @author weizhanjie
 */
public class CustomSiteRuleAction extends BaseAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        super.actionPerformed(anActionEvent);

        CustomSiteUtil customSiteUtil = CustomSiteUtil.getInstance(project);

        // 创建Swing窗口
        JFrame frame = new JFrame("自定义书源规则");
        frame.setSize(500, 620);
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.addItem("百度");
        comboBox.addItem("起点");
        comboBox.addItem("腾讯");
        comboBox.addItem("京东");
        panel.add(comboBox);

        JBTextArea textArea = new JBTextArea();
        textArea.setLineWrap(true);
        JBScrollPane scrollPane = new JBScrollPane();
        scrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
        scrollPane.setViewportView(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 500));
        panel.add(scrollPane);

        JButton verifyButton = new JButton("校验");
        verifyButton.addActionListener(e -> {
            // 获取选中分组名称
            String siteName = comboBox.getSelectedItem().toString();
            // 获取输入的规则
            String rule = textArea.getText();
            // 调用校验规则
            customSiteUtil.parseCustomSiteRule(rule);
        });
        panel.add(verifyButton, FlowLayout.CENTER);
        JButton okButton = new JButton("确定");
        panel.add(okButton);
        frame.add(panel);
        frame.setVisible(true);
    }
}
