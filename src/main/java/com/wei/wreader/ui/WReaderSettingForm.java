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
import com.wei.wreader.utils.ConfigYaml;
import com.wei.wreader.widget.GroupedComboBox.CharsetGroupComboBox;
import com.wei.wreader.widget.GroupedComboBox.GroupedComboBox;
import com.wei.wreader.utils.NumberUtil;
import com.wei.wreader.utils.tts.VoiceStyle;
import com.wei.wreader.widget.GroupedComboBox.OptionItem;
import org.apache.commons.lang3.StringUtils;
import com.wei.wreader.utils.tts.VoiceRole;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
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
    private ButtonGroup selectIconStyleRadioButtonGroup;
    private ButtonGroup displayTypeRadioGroup;

    private final ConfigYaml configYaml;
    private final CacheService cacheService;
    private Settings settings;
    private int selectedDisplayType;
    private int selectedIconStyle;

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

        // 字符集
        CharsetGroupComboBox charsetGroupComboBox = new CharsetGroupComboBox();
        charsetComboBox = charsetGroupComboBox.buildComboBox();
        charsetGroupComboBox.setSelectedItem(settings.getCharset());
        GridConstraints charsetGridConstraints = new GridConstraints();
        charsetGridConstraints.setRow(0);
        charsetGridConstraints.setColumn(0);
        charsetPanel.add(charsetComboBox, charsetGridConstraints);

        // 自动阅读
        autoReadTimeTextField.setDocument(new NumberDocument());
        int autoReadTime = settings.getAutoReadTime();
        if (autoReadTime <= 0) {
            autoReadTime = 5;
        }
        autoReadTimeTextField.setText(String.valueOf(autoReadTime));

        // 设置主图标风格
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

        // *** 音频管理 ***
        TitledBorder audioManageTitledBorder = new TitledBorder(border, "Audio Manage");
        audioManagePanel.setBorder(audioManageTitledBorder);
        // 音色
        Map<String, List<String>> nicknameByLocale = VoiceRole.getNicknameByLocale();
        GroupedComboBox voiceRoleGroupedComboBox = new GroupedComboBox();
        voiceRoleGroupComboBox = voiceRoleGroupedComboBox.buildGroupedComboBox(nicknameByLocale);
        voiceRoleGroupedComboBox.setSelectedItem(settings.getVoiceRole());
        GridConstraints voiceRoleGridConstraints = new GridConstraints();
        voiceRoleGridConstraints.setRow(0);
        voiceRoleGridConstraints.setColumn(0);
        voiceRoleJPanel.add(voiceRoleGroupComboBox, voiceRoleGridConstraints);

        // 音频超时
        timeoutTextField.setDocument(new NumberDocument());
        timeoutTextField.setText(String.valueOf(settings.getAudioTimeout()));
        // 语速
        Float[] rates = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
        rateComboBox.setModel(new DefaultComboBoxModel<>(rates));
        rateComboBox.setEditable(true);
        ComboBoxEditor rateEditor = rateComboBox.getEditor();
        rateEditor.setItem(settings.getRate());
        // 音量
        Integer[] volumes = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        volumeComboBox.setModel(new DefaultComboBoxModel<>(volumes));
        volumeComboBox.setEditable(true);
        ComboBoxEditor volumeEditor = volumeComboBox.getEditor();
        volumeEditor.setItem(settings.getVolume());
        // 音频风格
        List<String> names = VoiceStyle.getNames();
        for (String name : names) {
            audioStyleComboBox.addItem(name);
        }
        audioStyleComboBox.setSelectedItem(settings.getAudioStyle());

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
        if (settings.getAutoReadTime() != NumberUtil.parseInt(autoReadTime)) {
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
        settings.setSingleLineChars(NumberUtil.parseInt(lineMaxNumsTextField.getText()));
        settings.setShowLineNum(isShowLineNumCheckBox.isSelected());
        ButtonModel displayTypeSelection = displayTypeRadioGroup.getSelection();
        if (displayTypeSelection == null) {
            selectedDisplayType = Settings.DISPLAY_TYPE_SIDEBAR;
        } else {
            selectedDisplayType = NumberUtil.parseInt(displayTypeSelection.getActionCommand());
        }
        settings.setDisplayType(selectedDisplayType);
        OptionItem charsetSelectedItem = (OptionItem) charsetComboBox.getSelectedItem();
        settings.setCharset(charsetSelectedItem == null ? settings.getCharset() : charsetSelectedItem.getText());
        settings.setAutoReadTime(NumberUtil.parseInt(autoReadTimeTextField.getText()));
        OptionItem voiceRoleSelectedItem = (OptionItem) voiceRoleGroupComboBox.getSelectedItem();
        settings.setVoiceRole(voiceRoleSelectedItem == null ? settings.getVoiceRole() : voiceRoleSelectedItem.getText());
        settings.setAudioStyle((String) audioStyleComboBox.getSelectedItem());
        settings.setAudioTimeout(NumberUtil.parseInt(timeoutTextField.getText()));
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
}
