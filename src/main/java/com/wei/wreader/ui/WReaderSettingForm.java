package com.wei.wreader.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.*;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.factory.WReaderStatusBarFactory;
import com.wei.wreader.factory.WReaderToolWindowFactory;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.tts.VoiceRoleStyle;
import com.wei.wreader.utils.ui.GroupedComboBoxs.CharsetGroupComboBox;
import com.wei.wreader.utils.ui.GroupedComboBoxs.GroupedComboBox;
import com.wei.wreader.utils.ui.RadioButtonUtil;
import com.wei.wreader.utils.yml.ConfigYaml;
import com.wei.wreader.utils.ui.DecimalDocumentFilter;
import com.wei.wreader.utils.data.NumberUtil;
import com.wei.wreader.utils.tts.VoiceStyle;
import com.wei.wreader.utils.ui.GroupedComboBoxs.OptionItem;
import org.apache.commons.lang3.StringUtils;
import com.wei.wreader.utils.tts.VoiceRole;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 设置窗口
 *
 * @author weizhanjie
 */
public class WReaderSettingForm implements Configurable, Configurable.Composite {
    private JPanel settingPanel;
    private JTextField lineMaxNumsTextField;
    private JCheckBox isShowLineNumCheckBox;
    private JLabel lineMaxNumsLabel;
    private JLabel displayTypeLabel;
    private JBRadioButton sideBarRadioButton;
    private JBRadioButton statusBarRadioButton;
    private JBRadioButton editorBannerRadioButton;
    private ComboBox charsetComboBox;
    private JLabel charsetLabel;
    private JPanel charsetPanel;
    /**
     * 显示类型Panel
     */
    private JPanel displayTypeRadioPanel;
    private JTextField autoReadTimeTextField;
    private JLabel autoReadTimeLabel;
    private JPanel generalPanel;
    private JPanel audioManagePanel;
    private JLabel voiceRoleLabel;
    private JTextField timeoutTextField;
    private JLabel timeoutLabel;
    private ComboBox<Object> voiceRoleGroupComboBox;
    private JLabel rateLabel;
    private JLabel volumeLabel;
    private JComboBox rateComboBox;
    private JComboBox volumeComboBox;
    private JComboBox audioStyleComboBox;
    private JLabel audioStyleLabel;
    private JPanel voiceRoleJPanel;
    private JLabel selectIconStyleLabel;
    private JPanel selectIconStylePanel;
    private JComboBox editorMessageWindosWidthComboBox;
    private JComboBox editorMessageWindosHeightComboBox;
    private JLabel editorMessageWindosLebel;
    private JLabel editorMessageWindosWidthLabel;
    private JLabel editorMessageWindosHeightLabel;
    private JPanel customSiteRuleTextAreaTypePanel;
    private JPanel customSiteRuleTextAreaTypeLabelPanel;
    private ButtonGroup selectIconStyleRadioButtonGroup;
    private ButtonGroup displayTypeRadioGroup;
    private ButtonGroup customSiteRuleTextAreaTypeRadioGroup;

    private final ConfigYaml configYaml;
    private final CacheService cacheService;
    private Settings settings;
    private int selectedDisplayType;
    private int selectedIconStyle;
    private int selectedCustomSiteRuleTextAreaType;

    public WReaderSettingForm() {
        configYaml = ConfigYaml.getInstance();
        cacheService = CacheService.getInstance();
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }

        if (StringUtils.isBlank(settings.getCharset())) {
            settings.setCharset(configYaml.getSettings().getCharset());
        }
        // 主图标风格
        if (settings.getMainIconStyle() <= 0) {
            settings.setMainIconStyle(configYaml.getSettings().getMainIconStyle());
        }
        // 编辑器窗口信息
        if (settings.getEditorHintWidth() <= 0) {
            settings.setEditorHintWidth(configYaml.getSettings().getEditorHintWidth());
        }
        if (settings.getEditorHintHeight() <= 0) {
            settings.setEditorHintHeight(configYaml.getSettings().getEditorHintHeight());
        }
        // 音色
        if (StringUtils.isBlank(settings.getVoiceRole())) {
            settings.setVoiceRole(configYaml.getSettings().getVoiceRole());
        }
        // 音频超时时间
        if (settings.getAudioTimeout() <= 0) {
            settings.setAudioTimeout(configYaml.getSettings().getAudioTimeout());
        }
        // 语速
        if (settings.getRate() == null || settings.getRate() <= 0) {
            settings.setRate(configYaml.getSettings().getRate());
        }
        // 音量
        if (settings.getVolume() == null || settings.getVolume() < 0) {
            settings.setVolume(configYaml.getSettings().getVolume());
        }
        // 语音风格
        if (StringUtils.isBlank(settings.getAudioStyle())) {
            settings.setAudioStyle(configYaml.getSettings().getAudioStyle());
        }

    }

    /**
     * 配置页面名称
     *
     * @return
     */
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return configYaml.getName();
    }

    /**
     * 获取所有配置页面
     *
     * @return
     */
    @Override
    public Configurable [] getConfigurables() {
        return new Configurable[0];
    }

    /**
     * 创建配置页面
     *
     * @return
     */
    @Override
    public @Nullable JComponent createComponent() {
        Border border = JBUI.Borders.customLine(JBUI.CurrentTheme.Popup.separatorColor(), 1, 0, 0, 0);
        // *** 通用设置 ***
        TitledBorder generalTitledBorder = new TitledBorder(border, "general");
        generalPanel.setBorder(generalTitledBorder);

        // 初始化配置页面
        // 单行最大字数
        lineMaxNumsTextField.setDocument(new NumberDocument());
        lineMaxNumsTextField.setText(String.valueOf(settings.getSingleLineChars()));
        // 是否显示行号
        isShowLineNumCheckBox.setSelected(settings.isShowLineNum());
        // 显示类型
        createUIDisplayType();
        // 字符集
        createUICharset();
        // 自动阅读
        createUIAutoReadTime();
        // 设置主图标风格
        createUIMainIconStyle();
        // 编辑器消息窗口
        createUIEditorMessageWindow();
        // 自定义书源规则本文区域类型
        createUICustomSiteRuleTextAreaType();

        // *** 音频管理 ***
        TitledBorder audioManageTitledBorder = new TitledBorder(border, "Audio Manage");
        audioManagePanel.setBorder(audioManageTitledBorder);
        // 音色
        createUIVoiceRole();
        // 音频超时
        createUITimeoutText();
        // 语速
        createUIRate();
        // 音量
        createUIVolume();
        // 音频风格
        createUIAudioStyle();

        return settingPanel;
    }

    /**
     * 判断是否修改
     *
     * @return
     */
    @Override
    public boolean isModified() {
        // 显示类型
        int displayTypeTemp = settings.getDisplayType();
        ButtonModel displayTypeSelection = displayTypeRadioGroup.getSelection();
        if (displayTypeSelection == null) {
            return true;
        }
        selectedDisplayType = NumberUtil.parseInt(displayTypeSelection.getActionCommand());
        if (displayTypeTemp != selectedDisplayType) {
            return true;
        }

        // 单行最大字数
        String lineMaxNums = lineMaxNumsTextField.getText();
        if (settings.getSingleLineChars() != NumberUtil.parseInt(lineMaxNums)) {
            return true;
        }
        // 是否显示行号
        boolean isShowLineNum = isShowLineNumCheckBox.isSelected();
        if (settings.isShowLineNum() != isShowLineNum) {
            return true;
        }
        // 字符集
        OptionItem charsetSelectedItem = (OptionItem) charsetComboBox.getSelectedItem();
        if (charsetSelectedItem != null && !settings.getCharset().equals(charsetSelectedItem.getText())) {
            return true;
        }
        // 自动阅读
        String autoReadTime = autoReadTimeTextField.getText();

        if (!NumberUtil.parseFloat(autoReadTime).equals(settings.getAutoReadTime())) {
            return true;
        }
        // 主图标风格
        int mainIconStyle = settings.getMainIconStyle();
        ButtonModel mainIconStyleSelection = selectIconStyleRadioButtonGroup.getSelection();
        if (mainIconStyleSelection == null) {
            return true;
        }
        selectedIconStyle = NumberUtil.parseInt(mainIconStyleSelection.getActionCommand());
        if (mainIconStyle != selectedIconStyle) {
            return true;
        }
        // 编辑器消息窗口--宽度
        ComboBoxEditor editorMessageWindowWidthEditor = editorMessageWindosWidthComboBox.getEditor();
        int editorHintWidth = (Integer) editorMessageWindowWidthEditor.getItem();
        if (settings.getEditorHintWidth() != editorHintWidth) {
            return true;
        }
        // 编辑器消息窗口--高度
        ComboBoxEditor editorMessageWindowHeightEditor = editorMessageWindosHeightComboBox.getEditor();
        int editorHintHeight = (Integer) editorMessageWindowHeightEditor.getItem();
        if (settings.getEditorHintHeight() != editorHintHeight) {
            return true;
        }
        // 自定义书源规则本文区域类型
        int customSiteRuleTextAreaType = settings.getCustomSiteRuleTextAreaType();
        ButtonModel customSiteRuleTextAreaTypeSelection = customSiteRuleTextAreaTypeRadioGroup.getSelection();
        if (customSiteRuleTextAreaTypeSelection == null) {
            return true;
        }
        selectedCustomSiteRuleTextAreaType = NumberUtil.parseInt(customSiteRuleTextAreaTypeSelection.getActionCommand());
        if (customSiteRuleTextAreaType != selectedCustomSiteRuleTextAreaType) {
            return true;
        }

        // 音色
        OptionItem voiceRoleSelectedItem = (OptionItem) voiceRoleGroupComboBox.getSelectedItem();
        if (voiceRoleSelectedItem != null && !settings.getVoiceRole().equals(voiceRoleSelectedItem.getText())) {
            return true;
        }
        // 音频超时
        if (settings.getAudioTimeout() != NumberUtil.parseInt(timeoutTextField.getText())) {
            return true;
        }
        // 语速
        ComboBoxEditor rateEditor = rateComboBox.getEditor();
        if (!settings.getRate().equals(rateEditor.getItem())) {
            return true;
        }
        // 音量
        ComboBoxEditor volumeEditor = volumeComboBox.getEditor();
        if (!settings.getVolume().equals(volumeEditor.getItem())) {
            return true;
        }
        // 音频风格
        if (!settings.getAudioStyle().equals(audioStyleComboBox.getSelectedItem())) {
            return true;
        }

        return false;
    }


    /**
     * 设置页面点击apply按钮事件
     *
     * @throws ConfigurationException
     */
    @Override
    public void apply() throws ConfigurationException {
        // 单行最大字数
        settings.setSingleLineChars(NumberUtil.parseInt(lineMaxNumsTextField.getText()));
        // 是否显示行号
        settings.setShowLineNum(isShowLineNumCheckBox.isSelected());
        // 显示类型
        ButtonModel displayTypeSelection = displayTypeRadioGroup.getSelection();
        if (displayTypeSelection == null) {
            selectedDisplayType = Settings.DISPLAY_TYPE_SIDEBAR;
        } else {
            selectedDisplayType = NumberUtil.parseInt(displayTypeSelection.getActionCommand());
        }
        settings.setDisplayType(selectedDisplayType);
        // 字符集
        OptionItem charsetSelectedItem = (OptionItem) charsetComboBox.getSelectedItem();
        settings.setCharset(charsetSelectedItem == null ? settings.getCharset() : charsetSelectedItem.getText());
        // 自动阅读
        settings.setAutoReadTime(NumberUtil.parseFloat(autoReadTimeTextField.getText()));
        // 主图标风格
        ButtonModel mainIconStyleSelection = selectIconStyleRadioButtonGroup.getSelection();
        if (mainIconStyleSelection == null) {
            selectedIconStyle = 1;
        } else {
            selectedIconStyle = NumberUtil.parseInt(mainIconStyleSelection.getActionCommand());
        }
        settings.setMainIconStyle(selectedIconStyle);
        // 编辑器消息窗口--宽高
        ComboBoxEditor editorMessageWindowWidthEditor = editorMessageWindosWidthComboBox.getEditor();
        settings.setEditorHintWidth((Integer) editorMessageWindowWidthEditor.getItem());
        ComboBoxEditor editorMessageWindowHeightEditor = editorMessageWindosHeightComboBox.getEditor();
        settings.setEditorHintHeight((Integer) editorMessageWindowHeightEditor.getItem());
        // 自定义书源规则本文区域类型
        ButtonModel customSiteRuleTextAreaTypeSelection = customSiteRuleTextAreaTypeRadioGroup.getSelection();
        if (customSiteRuleTextAreaTypeSelection == null) {
            selectedCustomSiteRuleTextAreaType = Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR;
        } else {
            selectedCustomSiteRuleTextAreaType = NumberUtil.parseInt(customSiteRuleTextAreaTypeSelection.getActionCommand());
        }
        settings.setCustomSiteRuleTextAreaType(selectedCustomSiteRuleTextAreaType);

        OptionItem voiceRoleSelectedItem = (OptionItem) voiceRoleGroupComboBox.getSelectedItem();
        settings.setVoiceRole(voiceRoleSelectedItem == null ? settings.getVoiceRole() : voiceRoleSelectedItem.getText());
        settings.setAudioStyle((String) audioStyleComboBox.getSelectedItem());
        settings.setAudioTimeout(NumberUtil.parseInt(timeoutTextField.getText()));
        // 语速
        ComboBoxEditor rateEditor = rateComboBox.getEditor();
        settings.setRate((Float) rateEditor.getItem());
        // 音量
        ComboBoxEditor volumeEditor = volumeComboBox.getEditor();
        settings.setVolume((Integer) volumeEditor.getItem());
        cacheService.setSettings(settings);

        ProjectManager projectManager = ProjectManager.getInstance();
        Project[] openProjects = projectManager.getOpenProjects();
        Project project = openProjects[0];

        WReaderToolWindowFactory wReaderToolWindowFactory = new WReaderToolWindowFactory();
        wReaderToolWindowFactory.setEnabled(project);

        WReaderStatusBarFactory wReaderStatusBarFactory = new WReaderStatusBarFactory();
        wReaderStatusBarFactory.setEnabled(project, false);
    }

    /**
     * 创建页面显示类型UI
     */
    private void createUIDisplayType() {
        String[] displayTypeStrs = new String[]{Settings.DISPLAY_TYPE_SIDEBAR_STR, Settings.DISPLAY_TYPE_STATUSBAR_STR};
        int[] displayTypeValues = new int[]{Settings.DISPLAY_TYPE_SIDEBAR, Settings.DISPLAY_TYPE_STATUSBAR};
        displayTypeRadioGroup = new ButtonGroup();
        selectedDisplayType = settings.getDisplayType();
        // 设置显示类型Panel布局
        GridLayoutManager displayTypeRadioPanelLayoutManager = new GridLayoutManager(1, 3);
        displayTypeRadioPanel.setLayout(displayTypeRadioPanelLayoutManager);
        for (int i = 0; i < displayTypeStrs.length; i++) {
            JBRadioButton radioButton = new JBRadioButton();
            radioButton.setText(displayTypeStrs[i]);
            if (selectedDisplayType <= 0 && i == 0) {
                radioButton.setSelected(true);
            } else if (selectedDisplayType == displayTypeValues[i]) {
                radioButton.setSelected(true);
            }
            radioButton.setActionCommand(String.valueOf(displayTypeValues[i]));
            GridConstraints radioGridConstraints = new GridConstraints();
            radioGridConstraints.setRow(0);
            radioGridConstraints.setColumn(i);
            displayTypeRadioPanel.add(radioButton, radioGridConstraints);
            displayTypeRadioGroup.add(radioButton);
        }
    }

    /**
     * 创建字符集下拉框UI
     */
    private void createUICharset() {
        CharsetGroupComboBox charsetGroupComboBox = new CharsetGroupComboBox();
        charsetComboBox = charsetGroupComboBox.buildComboBox();
        charsetGroupComboBox.setSelectedItem(settings.getCharset());
        GridConstraints charsetGridConstraints = new GridConstraints();
        charsetGridConstraints.setRow(0);
        charsetGridConstraints.setColumn(0);
        charsetPanel.add(charsetComboBox, charsetGridConstraints);
    }

    /**
     * 创建自动阅读时间UI
     */
    private void createUIAutoReadTime() {
        ((AbstractDocument) autoReadTimeTextField.getDocument()).setDocumentFilter(new DecimalDocumentFilter(2));
        float autoReadTime = settings.getAutoReadTime();
        if (autoReadTime <= 0f) {
            autoReadTime = 5f;
        }
        autoReadTimeTextField.setText(String.valueOf(autoReadTime));
    }

    /**
     * 创建主图标风格UI
     */
    private void createUIMainIconStyle() {
        String[] selectIconStyleRadioButtonTexts = {"默认", "浅色"};
        int[] mainIconStyleRadioButtonValues = {1, 2};
        selectIconStyleRadioButtonGroup = new ButtonGroup();
        GridLayoutManager selectIconStylePanelLayoutManager = new GridLayoutManager(1, 3);
        selectIconStylePanel.setLayout(selectIconStylePanelLayoutManager);
        selectedIconStyle = settings.getMainIconStyle();
        for (int i = 0, len = selectIconStyleRadioButtonTexts.length; i < len; i++) {
            JBRadioButton radioButtons = new JBRadioButton();
            radioButtons.setText(selectIconStyleRadioButtonTexts[i]);
            radioButtons.setActionCommand(String.valueOf(mainIconStyleRadioButtonValues[i]));
            if (selectedIconStyle <= 0 && i == 0) {
                radioButtons.setSelected(true);
            } else if (selectedIconStyle == mainIconStyleRadioButtonValues[i]) {
                radioButtons.setSelected(true);
            }
            GridConstraints mainIconStyleRadioButtonGridConstraints = new GridConstraints();
            mainIconStyleRadioButtonGridConstraints.setRow(0);
            mainIconStyleRadioButtonGridConstraints.setColumn(i);
            selectIconStylePanel.add(radioButtons, mainIconStyleRadioButtonGridConstraints);
            selectIconStyleRadioButtonGroup.add(radioButtons);
        }
    }

    /**
     * 创建编辑器提示窗口尺寸
     */
    private void createUIEditorMessageWindow() {
        // 编辑器消息窗口--宽度
        Integer[] editorHintWidths = {100, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800};
        editorMessageWindosWidthComboBox.setModel(new DefaultComboBoxModel<>(editorHintWidths));
        editorMessageWindosWidthComboBox.setEditable(true);
        ComboBoxEditor editorMessageWindowWidthEditor = editorMessageWindosWidthComboBox.getEditor();
        editorMessageWindowWidthEditor.setItem(settings.getEditorHintWidth());
        // 编辑器消息窗口--高度
        Integer[] editorHintHeights = {100, 150, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800};
        editorMessageWindosHeightComboBox.setModel(new DefaultComboBoxModel<>(editorHintHeights));
        editorMessageWindosHeightComboBox.setEditable(true);
        ComboBoxEditor editorMessageWindowHeightEditor = editorMessageWindosHeightComboBox.getEditor();
        editorMessageWindowHeightEditor.setItem(settings.getEditorHintHeight());
    }

    /**
     * 创建自定义书源规则文本框类型UI
     */
    private void createUICustomSiteRuleTextAreaType() {
        // Label
        JTextArea areaTypeLabel = new JTextArea("自定义书源规则\n本文区域类型");
        areaTypeLabel.setSize(new Dimension(120, 40));
        areaTypeLabel.setLineWrap(true);        // 开启自动换行
        areaTypeLabel.setWrapStyleWord(true);   // 按单词边界换行
        areaTypeLabel.setEditable(false);       // 设置为只读
        areaTypeLabel.setOpaque(false);         // 背景透明
        areaTypeLabel.setBorder(null);          // 去掉边框
        customSiteRuleTextAreaTypeLabelPanel.setLayout(new GridLayoutManager(1, 1));
        GridConstraints areaTypeLabelGrid = new GridConstraints();
        areaTypeLabelGrid.setRow(0);
        areaTypeLabelGrid.setColumn(0);
        // 设置水平位置--右边（ANCHOR_EAST：东，按照上北下南左西右东的顺序，东对应右边）
        areaTypeLabelGrid.setAnchor(GridConstraints.ANCHOR_EAST);
        customSiteRuleTextAreaTypeLabelPanel.add(areaTypeLabel, areaTypeLabelGrid);

        // 单选
        String[] customSiteRuleTextAreaTypeStrs = new String[]{
                Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR_TEXT,
                Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA_TEXT
        };
        String[] customSiteRuleTextAreaTypeHint = new String[]{
                Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR_HINT,
                Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA_HINT
        };
        int[] customSiteRuleTextAreaTypeValues = new int[]{
                Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_EDITOR,
                Settings.CUSTOM_SITE_RULE_TEXT_AREA_TYPE_TEXTAREA
        };
        customSiteRuleTextAreaTypeRadioGroup = new ButtonGroup();
        selectedCustomSiteRuleTextAreaType = settings.getCustomSiteRuleTextAreaType();
        GridLayoutManager customSiteRuleTextAreaTypePanelLayoutManager = new GridLayoutManager(customSiteRuleTextAreaTypeStrs.length, 1);
        customSiteRuleTextAreaTypePanel.setLayout(customSiteRuleTextAreaTypePanelLayoutManager);
        for (int i = 0; i < customSiteRuleTextAreaTypeStrs.length; i++) {
            boolean isSelected = false;
            if (selectedCustomSiteRuleTextAreaType <= 0 && i == 0) {
                isSelected = true;
            } else if (selectedCustomSiteRuleTextAreaType == customSiteRuleTextAreaTypeValues[i]) {
                isSelected = true;
            }
            RadioButtonUtil.RadioButtonWithHintResult radioButtonWithHint = RadioButtonUtil.createRadioButtonWithHint(
                    customSiteRuleTextAreaTypeStrs[i],
                    String.valueOf(customSiteRuleTextAreaTypeValues[i]),
                    customSiteRuleTextAreaTypeHint[i],
                    isSelected, 300, 20);
            GridConstraints radioGridConstraints = new GridConstraints();
            radioGridConstraints.setRow(i);
            radioGridConstraints.setColumn(0);
            // 设置水平位置--左边（ANCHOR_WEST：西，按照上北下南左西右东的顺序，西对应左边）
            radioGridConstraints.setAnchor(GridConstraints.ANCHOR_WEST);
            customSiteRuleTextAreaTypePanel.add(radioButtonWithHint.getPanel(), radioGridConstraints);
            customSiteRuleTextAreaTypeRadioGroup.add(radioButtonWithHint.getRadioButton());
        }
    }

    /**
     * 创建音色设置UI
     */
    private void createUIVoiceRole() {
        Map<String, List<String>> nicknameByLocale = VoiceRole.getNicknameByLocaleGender();
        GroupedComboBox voiceRoleGroupedComboBox = new GroupedComboBox();
        voiceRoleGroupComboBox = voiceRoleGroupedComboBox.buildGroupedComboBox(nicknameByLocale);
        voiceRoleGroupedComboBox.setSelectedItem(settings.getVoiceRole());
        voiceRoleGroupComboBox.addActionListener(e1 -> {
            OptionItem voiceRoleSelectedItem = (OptionItem) voiceRoleGroupComboBox.getSelectedItem();
            if (voiceRoleSelectedItem != null) {
                // 获取音色拥有的风格
                VoiceStyle[] voiceStyles = VoiceRoleStyle.getByRoleNickName(voiceRoleSelectedItem.getText());
                if (voiceStyles.length == 0) {
                    audioStyleComboBox.setModel(new DefaultComboBoxModel<>(new String[]{VoiceStyle.style_default.name}));
                } else {
                    String[] voiceStyleStrs = new String[voiceStyles.length];
                    for (int i = 0, len = voiceStyles.length; i < len; i++) {
                        voiceStyleStrs[i] = voiceStyles[i].name;
                    }
                    // 更新音频风格下拉框选项
                    audioStyleComboBox.setModel(new DefaultComboBoxModel<>(voiceStyleStrs));
                }
                audioStyleComboBox.setSelectedIndex(0);
            } else {
                audioStyleComboBox.setModel(new DefaultComboBoxModel<>(new String[]{VoiceStyle.style_default.name}));
                audioStyleComboBox.setSelectedIndex(0);
            }
        });
        GridConstraints voiceRoleGridConstraints = new GridConstraints();
        voiceRoleGridConstraints.setRow(0);
        voiceRoleGridConstraints.setColumn(0);
        voiceRoleJPanel.add(voiceRoleGroupComboBox, voiceRoleGridConstraints);
    }
    /**
     * 创建音频超时设置UI
     */
    private void createUITimeoutText() {
        timeoutTextField.setDocument(new NumberDocument());
        timeoutTextField.setText(String.valueOf(settings.getAudioTimeout()));
    }

    /**
     * 创建音频语速设置UI
     */
    private void createUIRate() {
        Float[] rates = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
        rateComboBox.setModel(new DefaultComboBoxModel<>(rates));
        rateComboBox.setEditable(true);
        ComboBoxEditor rateEditor = rateComboBox.getEditor();
        rateEditor.setItem(settings.getRate());
    }
    /**
     * 创建音频音量设置UI
     */
    private void createUIVolume() {
        Integer[] volumes = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        volumeComboBox.setModel(new DefaultComboBoxModel<>(volumes));
        volumeComboBox.setEditable(true);
        ComboBoxEditor volumeEditor = volumeComboBox.getEditor();
        volumeEditor.setItem(settings.getVolume());
    }
    /**
     * 创建音频风格设置UI
     */
    private void createUIAudioStyle() {
        String voiceRole = settings.getVoiceRole();
        VoiceStyle[] voiceStyles = VoiceRoleStyle.getByRoleNickName(voiceRole);
        if (voiceStyles.length == 0) {
            audioStyleComboBox.setModel(new DefaultComboBoxModel<>(new String[]{VoiceStyle.style_default.name}));
            audioStyleComboBox.setSelectedIndex(0);
        } else {
            String[] voiceStyleStrs = new String[voiceStyles.length];
            for (int i = 0, len = voiceStyles.length; i < len; i++) {
                voiceStyleStrs[i] = voiceStyles[i].name;
            }
            // 音频风格下拉框选项
            audioStyleComboBox.setModel(new DefaultComboBoxModel<>(voiceStyleStrs));
            audioStyleComboBox.setSelectedItem(settings.getAudioStyle());
        }
    }

}

