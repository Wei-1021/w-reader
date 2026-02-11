package com.wei.wreader.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.pojo.SiteBean;
import com.wei.wreader.service.CustomSiteRuleCacheServer;
import com.wei.wreader.utils.CustomSiteUtil;
import com.wei.wreader.utils.data.ConstUtil;
import org.apache.commons.lang3.StringUtils;
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
    private TextEditor editor;

    // 参数
    /**
     * 加载书源规则的分组名称
     */
    private String loadSourceGroupKeyName = "";

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        super.actionPerformed(anActionEvent);

        customSiteUtil = CustomSiteUtil.getInstance(project);
        customSiteRuleCacheServer = CustomSiteRuleCacheServer.getInstance();

        ApplicationManager.getApplication().invokeLater(() -> {
            buildWindow();
        });
    }


    private void buildWindow() {
        List<String> customSiteKeyGroupList = customSiteUtil.getCustomSiteKeyGroupList();
        String selectedKey = customSiteRuleCacheServer.getSelectedCustomSiteRuleKey();

        JFrame frame = new JFrame("自定义书源规则(Beta)");
        frame.setSize(850, 700);
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
        resetButton = new JButton("重置");
        deleteButton = new JButton("删除");
        firstLayer.add(comboBox);
        firstLayer.add(Box.createHorizontalStrut(5)); // 水平间距
        firstLayer.add(loadButton);
        firstLayer.add(Box.createHorizontalStrut(5)); // 水平间距
        firstLayer.add(resetButton);
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
        // 代码编辑器
        if (settings.getCustomSiteRuleTextAreaType() == 0 ||
                settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR) {
            // 创建一个 JSON 文件类型对象
            FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
            // 创建一个内存中的虚拟文件
            LightVirtualFile virtualFile = new LightVirtualFile("w-reader-custom-rule.json", jsonFileType, "");
            virtualFile.setWritable(true);
            // 使用 TextEditorProvider 创建完整功能的编辑器
            TextEditorProvider provider = TextEditorProvider.getInstance();
            editor = (TextEditor) provider.createEditor(project, virtualFile);
            document = editor.getEditor().getDocument();
            // 放入滚动面板
            scrollPane = new JBScrollPane(editor.getComponent());
        }
        // 普通文本框
        else if (settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA) {
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            // 按单词换行
            textArea.setWrapStyleWord(true);
            // 放入滚动面板
            scrollPane = new JBScrollPane(textArea);
        }
        scrollPane.setPreferredSize(new Dimension(800, 500));
        scrollPane.setMinimumSize(new Dimension(600, 500));
        thirdLayer.add(new JLabel("书源规则:"), BorderLayout.NORTH);
        thirdLayer.add(scrollPane, BorderLayout.CENTER);

        // 第四层：提示文本区域
        fourthLayer = new JPanel(new BorderLayout());
        JTextArea noticeTextArea = new JTextArea();
        noticeTextArea.setLineWrap(true);
        noticeTextArea.setWrapStyleWord(true);
        noticeTextArea.setEditable(false);
        noticeTextArea.setBorder(JBUI.Borders.empty());
        noticeTextArea.setText("提示：本功能规则比较简陋，目前只适合获取相对简单的书源，部分包括但不限于需要登录权限、字体加密等复杂的书源暂时是没法获取的。" +
                "如您有更好的想法，欢迎email或github留言。“书源规则说明”请前往Gitee/GitHub仓库Wiki页查看，或者点击下方按钮跳转。QQ群: 1060150904");
        // 设置背景色为主题背景色
        noticeTextArea.setBackground(UIManager.getColor("Panel.background"));
        fourthLayer.add(noticeTextArea, BorderLayout.CENTER);

        // 第五层：规则教程放左边，校验和确定按钮放右边
        fifthLayer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        guideButton = new JButton("规则教程");
        verifyButton = new JButton("校验");
        confirmButton = new JButton("确定");
        fifthLayer.add(guideButton);
        fifthLayer.add(Box.createHorizontalStrut(30));
        fifthLayer.add(verifyButton);
        fifthLayer.add(Box.createHorizontalStrut(5));
        fifthLayer.add(confirmButton);

        // 添加各层到主面板，并设置层间距
        mainPanel.add(firstLayer);
        // 垂直间距
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
        // 添加窗口大小改变监听器
        this.addMainPanelComponentListener();
        // "加载"按钮监听器
        this.addLoadBtnEventListeners();
        // "重置"按钮监听器
        this.addResetBtnEventListeners();
        // "删除"按钮监听器
        this.addDeleteBtnEventListeners();
        // "规则教程"按钮监听器
        this.addGuideBtnEventListeners();
        // "校验"按钮监听器
        this.addVerifyBtnEventListeners();
        // "确定"按钮监听器
        this.addConfirmBtnEventListeners();
    }

    /**
     * 添加主面板的组件监听器
     */
    private void addMainPanelComponentListener() {
        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 重设内容面板的大小
                int width = mainPanel.getWidth() - 50;
                int height = mainPanel.getHeight() - 220;
                scrollPane.setPreferredSize(new Dimension(width, height));
                //
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
        // "加载"按钮监听器
        loadButton.addActionListener(e -> {
            String groupKeyName = (String) comboBox.getSelectedItem();
            if (groupKeyName == null || groupKeyName.isEmpty()) {
                Messages.showInfoMessage("请选择分组", "提示");
                return;
            }

            loadSourceGroupKeyName = groupKeyName;

            Map<String, String> customSiteRuleGroupMap = customSiteRuleCacheServer.getCustomSiteRuleOriginalStrMap();
            String siteBeanJson = customSiteRuleGroupMap.get(groupKeyName);
            if (StringUtils.isEmpty(siteBeanJson)) {
                Messages.showInfoMessage("分组不存在", "提示");
                return;
            }

            siteBeanJson = siteBeanJson.replace("\r\n", "\n").replace("\r", "\n");

            groupNameTextField.setText(groupKeyName);

            // 代码编辑器
            if (settings.getCustomSiteRuleTextAreaType() == 0 ||
                    settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR) {
                // 如果文本不大，直接写入
                if (siteBeanJson.length() <= 8000) {
                    String finalSiteBeanJson = siteBeanJson;
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        document.replaceString(0, document.getTextLength(), finalSiteBeanJson);
                    });
                    return;
                }
                // 分块写入：先清空，再追加
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.replaceString(0, document.getTextLength(), "");
                });
                final int CHUNK_SIZE = 5000;
                for (int i = 0; i < siteBeanJson.length(); i += CHUNK_SIZE) {
                    final int start = i;
                    final int end = Math.min(i + CHUNK_SIZE, siteBeanJson.length());
                    final String chunk = siteBeanJson.substring(start, end);
                    // 每块单独一个 WriteCommand（可合并 Undo）
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        document.insertString(document.getTextLength(), chunk);
                    });
                }
            }
            // 普通文本框
            else if (settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA) {
                setCustomRuleTextArea(siteBeanJson);
            }
        });
    }

    /**
     * 添加"重置"按钮监听器
     */
    private void addResetBtnEventListeners() {
        resetButton.addActionListener(e -> {
            comboBox.setSelectedIndex(0);
            setCustomRuleTextArea("");
            groupNameTextField.setText("");
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
            setCustomRuleTextArea("");

            // 提示
            Messages.showInfoMessage("删除成功", "提示");
        });
    }

    /**
     * 添加"规则教程"按钮监听器
     */
    private void addGuideBtnEventListeners() {
        guideButton.addActionListener(e -> {
            // 跳转到规则教程页面
            BrowserUtil.browse("https://gitee.com/weizhanjie/w-reader/wikis/%E8%87%AA%E5%AE%9A%E4%B9%89%E4%B9%A6%E6%BA%90%E8%A7%84%E5%88%99%E8%AF%B4%E6%98%8E");
        });
    }

    /**
     * 添加"验证"按钮监听器
     */
    private void addVerifyBtnEventListeners() {
        verifyButton.addActionListener(e -> {
            // 获取输入的规则
            String rule = getCustomRuleTextAreaContent();
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
                Messages.showInfoMessage("校验通过", "提示");
            }, null);
        });
    }

    /**
     * 添加"保存"按钮监听器
     */
    private void addConfirmBtnEventListeners() {
        confirmButton.addActionListener(e -> {
            // 获取输入的分组名称
            String groupName = groupNameTextField.getText();
            if (groupName == null || groupName.isEmpty()) {
                Messages.showInfoMessage("请输入分组名称", "提示");
                return;
            }

            if (ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(loadSourceGroupKeyName) &&
                    !ConstUtil.WREADER_DEFAULT_SITE_MAP_KEY.equals(groupName)) {
                Messages.showInfoMessage("不能修改默认分组的名称", "提示");
                return;
            }

            // 获取输入的规则
            String rule = getCustomRuleTextAreaContent();
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


    /**
     * 设置自定义书源规则文本域的内容
     * @param content
     */
    private void setCustomRuleTextArea(String content) {
        // 代码编辑器
        if (settings.getCustomSiteRuleTextAreaType() == 0 ||
                settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(0, document.getTextLength(), content);
            });
        }
        // 普通文本框
        else if (settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA) {
            textArea.setText(content);
        }
    }

    /**
     * 获取自定义书源规则文本域的内容
     * @return
     */
    private String getCustomRuleTextAreaContent() {
        // 代码编辑器
        if (settings.getCustomSiteRuleTextAreaType() == 0 ||
                settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR) {
            return document.getText();
        }
        // 普通文本框
        else if (settings.getCustomSiteRuleTextAreaType() == Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA) {
            return textArea.getText();
        }
        return "";
    }
}
