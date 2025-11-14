package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.wei.wreader.pojo.SiteBean;
import com.wei.wreader.service.CustomSiteRuleCacheServer;
import com.wei.wreader.utils.CustomSiteUtil;
import com.wei.wreader.utils.data.ConstUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自定义书源规则Action
 *
 * @author weizhanjie
 */
public class CustomSiteRuleAction extends BaseAction {

    private CustomSiteUtil customSiteUtil;
    private CustomSiteRuleCacheServer customSiteRuleCacheServer;

    // 定义组件
    private JPanel mainPanel;
    private JPanel firstLayer;
    private JPanel secondLayer;
    private JPanel thirdLayer;
    private JPanel fourthLayer;
    private ComboBox<String> comboBox;
    private JButton loadButton;
    private JButton deleteButton;
    private JTextField groupNameTextField;
    private JBScrollPane scrollPane;
    private JTextArea textArea;
    private JButton verifyButton;
    private JButton confirmButton;

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        super.actionPerformed(anActionEvent);

        customSiteUtil = CustomSiteUtil.getInstance(project);
        customSiteRuleCacheServer = CustomSiteRuleCacheServer.getInstance();

        buildWindow();
    }


    private void buildWindow() {
        List<String> customSiteKeyGroupList = customSiteUtil.getCustomSiteKeyGroupList();
        String selectedKey = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();

        JFrame frame = new JFrame("自定义书源规则");
        frame.setSize(500, 700);
        // 让窗口处于屏幕中心
        frame.setLocationRelativeTo(null);

        // 创建主面板，使用垂直布局
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 第一层：下拉框和按钮
        firstLayer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboBox = new ComboBox<>();
        if (customSiteKeyGroupList != null && !customSiteKeyGroupList.isEmpty()) {
            for (String key : customSiteKeyGroupList) {
                comboBox.addItem(key);
            }
        }
        comboBox.setSelectedItem(selectedKey);
        loadButton = new JButton("加载");
        deleteButton = new JButton("删除");
        firstLayer.add(comboBox);
        firstLayer.add(Box.createHorizontalStrut(5)); // 水平间距
        firstLayer.add(loadButton);
        firstLayer.add(Box.createHorizontalStrut(5)); // 水平间距
        firstLayer.add(deleteButton);

        // 第二层：标签和文本框
        secondLayer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel groupNameLabel = new JLabel("分组名称:");
        groupNameTextField = new JTextField(30);
        secondLayer.add(groupNameLabel);
        secondLayer.add(Box.createHorizontalStrut(5));
        secondLayer.add(groupNameTextField);

        // 第三层：滚动区域包含文本编辑区
        thirdLayer = new JPanel(new BorderLayout());
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        // 按单词换行
        textArea.setWrapStyleWord(true);
        scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 500));
        scrollPane.setMinimumSize(new Dimension(350, 500));
        thirdLayer.add(new JLabel("书源规则:"), BorderLayout.NORTH);
        thirdLayer.add(scrollPane, BorderLayout.CENTER);

        // 第四层：校验和确定按钮
        fourthLayer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        verifyButton = new JButton("校验");
        confirmButton = new JButton("确定");
        fourthLayer.add(verifyButton);
        fourthLayer.add(Box.createHorizontalStrut(5));
        fourthLayer.add(confirmButton);

        // 添加各层到主面板，并设置层间距
        mainPanel.add(firstLayer);
        // 垂直间距
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(secondLayer);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(thirdLayer);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(fourthLayer);

        frame.add(mainPanel);
        frame.setVisible(true);

        addEventListeners();
    }

    /**
     * 添加事件监听器
     */
    private void addEventListeners() {
        // 添加窗口大小改变监听器
        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 重设内容面板的大小
                int width = mainPanel.getWidth() - 50;
                int height = mainPanel.getHeight() - 200;
                scrollPane.setPreferredSize(new Dimension(width, height));
                //
                firstLayer.setPreferredSize(new Dimension(width, 40));
                secondLayer.setPreferredSize(new Dimension(width, 40));
                thirdLayer.setPreferredSize(new Dimension(width, height));
                fourthLayer.setPreferredSize(new Dimension(width, 40));
            }
        });

        AtomicReference<String> sourceGroupKeyName = new AtomicReference<>("");
        // "加载"按钮监听器
        loadButton.addActionListener(e -> {
            String groupKeyName = (String) comboBox.getSelectedItem();
            if (groupKeyName == null || groupKeyName.isEmpty()) {
                Messages.showInfoMessage("请选择分组", "提示");
                return;
            }

            sourceGroupKeyName.set(groupKeyName);

            Map<String, String> customSiteRuleGroupMap = customSiteRuleCacheServer.getCustomSiteRuleOriginalStrMap();
            String siteBeanJson = customSiteRuleGroupMap.get(groupKeyName);

            groupNameTextField.setText(groupKeyName);
            textArea.setText(siteBeanJson);
        });
        // "删除"按钮监听器
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

            if (Messages.showYesNoDialog("确定要删除吗？", "提示", Messages.getQuestionIcon()) != Messages.YES) {
                return;
            }

            // 删除对应的缓存信息--原始JSON字符串
            Map<String, String> originalMap = customSiteRuleCacheServer.getCustomSiteRuleOriginalStrMap();
            originalMap.remove(groupKeyName);
            customSiteRuleCacheServer.setCustomSiteRuleOriginalStrMap(originalMap);
            // 删除对应的缓存信息--转换后续的列表
            Map<String, List<SiteBean>> siteMap = customSiteRuleCacheServer.getCustomSiteRuleGroupMap();
            siteMap.remove(groupKeyName);
            customSiteRuleCacheServer.setCustomSiteRuleGroupMap(siteMap);

            // 删除下拉框的下拉选项
            comboBox.removeItem(groupKeyName);
            // 清空文本框
            groupNameTextField.setText("");
            textArea.setText("");

            // 提示
            Messages.showInfoMessage("删除成功", "提示");
        });
        // "校验"按钮监听器
        verifyButton.addActionListener(e -> {
            // 获取输入的规则
            String rule = textArea.getText();
            if (rule == null || rule.isEmpty()) {
                Messages.showInfoMessage("请输入自定义书源规则", "提示");
                return;
            }

            // 调用校验规则
            customSiteUtil.parseCustomSiteRule(rule, successValidationResult -> {
                List<SiteBean> siteBeans = successValidationResult.getBeanList();
                for (SiteBean siteBean : siteBeans) {
                    System.out.println(siteBean);
                }
                Messages.showInfoMessage("校验成功", "提示");
            }, null);
        });
        // "确定"按钮监听器
        confirmButton.addActionListener(e -> {
            // 获取输入的分组名称
            String groupName = groupNameTextField.getText();
            if (groupName == null || groupName.isEmpty()) {
                Messages.showInfoMessage("请输入分组名称", "提示");
                return;
            }

            if (ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(sourceGroupKeyName.get()) &&
                    !ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(groupName)) {
                Messages.showInfoMessage("不能修改默认分组的名称", "提示");
                return;
            }

            // 获取输入的规则
            String rule = textArea.getText();
            if (rule == null || rule.isEmpty()) {
                Messages.showInfoMessage("请输入自定义书源规则", "提示");
                return;
            }

            // 调用校验规则
            customSiteUtil.parseCustomSiteRule(rule, successValidationResult -> {
                if (Messages.showYesNoDialog("确定保存？", "提示", Messages.getQuestionIcon()) != Messages.YES) {
                    return;
                }

                List<SiteBean> siteBeans = successValidationResult.getBeanList();

                // 将转化后的列表添加到缓存
                Map<String, List<SiteBean>> siteMap = customSiteUtil.getSiteMap();
                siteMap.put(groupName, siteBeans);
                customSiteRuleCacheServer.setCustomSiteRuleGroupMap(siteMap);
                // 将原json字符串添加到缓存
                Map<String, String> customSiteRuleOriginalStrMap = customSiteRuleCacheServer.getCustomSiteRuleOriginalStrMap();
                if (customSiteRuleOriginalStrMap == null) {
                    customSiteRuleOriginalStrMap = new HashMap<>();
                }
                customSiteRuleOriginalStrMap.put(groupName, rule);

                // 添加到下拉框
                comboBox.addItem(groupName);

                Messages.showInfoMessage("保存成功", "提示");
            }, null);
        });
    }
}
