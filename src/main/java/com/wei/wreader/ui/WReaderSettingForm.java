package com.wei.wreader.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.*;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.factory.WReaderStatusBarFactory;
import com.wei.wreader.factory.WReaderToolWindowFactory;
import com.wei.wreader.model.Settings;
import com.wei.wreader.reader.FontManager;
import com.wei.wreader.service.AppConfigService;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.service.CredentialService;
import com.wei.wreader.util.SettingConstants;
import com.wei.wreader.util.WReaderIcons;
import com.wei.wreader.tts.edge.VoiceRoleStyle;
import com.wei.wreader.util.ui.GroupedComboBoxs.CharsetGroupComboBox;
import com.wei.wreader.util.ui.GroupedComboBoxs.GroupedComboBox;
import com.wei.wreader.util.ui.MessageDialogUtil;
import com.wei.wreader.util.ui.RadioButtonUtil;
import com.wei.wreader.util.yml.ConfigYaml;
import com.wei.wreader.util.ui.DecimalDocumentFilter;
import com.wei.wreader.util.data.ConstUtil;
import com.wei.wreader.util.data.NumberUtil;
import com.wei.wreader.tts.edge.VoiceStyle;
import com.wei.wreader.util.ui.GroupedComboBoxs.OptionItem;
import com.wei.wreader.widget.ReaderStatusBarWidget;
import org.apache.commons.lang3.StringUtils;
import com.wei.wreader.tts.edge.VoiceRole;
import com.wei.wreader.tts.enums.TtsEngineEnum;
import com.wei.wreader.tts.mimo.enums.MimoModel;
import com.wei.wreader.tts.mimo.enums.Voice;
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
    private JPanel footerTipPanel;
    private ButtonGroup selectIconStyleRadioButtonGroup;
    private ButtonGroup displayTypeRadioGroup;
    private ButtonGroup customSiteRuleTextAreaTypeRadioGroup;

    // TTS引擎选择
    private JLabel ttsEngineLabel;
    private JComboBox<String> ttsEngineComboBox;
    private JLabel mimoApiKeyLabel;
    private JBPasswordField mimoApiKeyTextField;
    private JLabel mimoApiKeyHintLabel;
    private ActionLink mimoApiKeyLink;
    private JLabel mimoModelTypeLabel;
    private JComboBox<String> mimoModelTypeComboBox;
    private JLabel mimoVoiceDescLabel;
    private JBTextArea mimoVoiceDescTextArea;
    private JButton mimoVoiceDescPresetButton;
    private JLabel mimoVoiceDescHintLabel;
    private JBScrollPane mimoVoiceDescTextAreaScroll;

    // 状态栏字体设置
    private JSpinner fontSizeSpinner;
    private JButton fontColorButton;
    private JLabel fontColorPreview;
    private JPanel statusBarFontPanel;

    private final ConfigYaml configYaml;
    private final CacheService cacheService;
    private final AppConfigService appConfig;
    private Settings settings;
    private final FontManager fontManager;
    private int selectedDisplayType;
    private int selectedIconStyle;
    private int selectedCustomSiteRuleTextAreaType;
    private String[][] changeVoiceDescPreset = {};
    private String changeVoiceDescriptionLabel = "";

    public WReaderSettingForm() {
        configYaml = ConfigYaml.getInstance();
        cacheService = CacheService.getInstance();
        appConfig = AppConfigService.getInstance();
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
        }
        fontManager = new FontManager(cacheService, appConfig);

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
        TitledBorder generalTitledBorder = new TitledBorder(border, SettingConstants.BORDER_TITLE_GENERAL);
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

        // *** 状态栏字体设置 ***
        createUIStatusBarFont();

        // *** 音频管理 ***
        TitledBorder audioManageTitledBorder = new TitledBorder(border, SettingConstants.BORDER_TITLE_AUDIO_MANAGE);
        audioManagePanel.setBorder(audioManageTitledBorder);
        // TTS引擎选择
        createUITtsEngine();
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

        // 底部提示
        createUIFooterTipPanel();

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

        // 状态栏字体大小
        int currentFontSize = cacheService.getFontSize();
        if (currentFontSize <= 0) {
            currentFontSize = (int) ConstUtil.DEFAULT_FONT_SIZE;
        }
        if (currentFontSize != (Integer) fontSizeSpinner.getValue()) {
            return true;
        }
        // 状态栏字体颜色
        String currentFontColorHex = cacheService.getFontColorHex();
        if (currentFontColorHex == null || currentFontColorHex.isEmpty()) {
            currentFontColorHex = ConstUtil.DEFAULT_FONT_COLOR_HEX;
        }
        Color previewColor = fontColorPreview.getBackground();
        String previewColorHex = String.format("#%02X%02X%02X", previewColor.getRed(), previewColor.getGreen(), previewColor.getBlue());
        if (!currentFontColorHex.equalsIgnoreCase(previewColorHex)) {
            return true;
        }

        // TTS引擎
        String selectedTtsEngine = (String) ttsEngineComboBox.getSelectedItem();
        String currentTtsEngine = settings.getTtsEngine() != null ? settings.getTtsEngine() : TtsEngineEnum.EDGE.getEngineId();
        if (!currentTtsEngine.equals(selectedTtsEngine)) {
            return true;
        }
        // MiMo API Key
        String mimoApiKey = new String(mimoApiKeyTextField.getPassword());
        String currentMimoApiKey = CredentialService.getInstance().getMimoApiKey();
        if (currentMimoApiKey == null) currentMimoApiKey = "";
        if (!currentMimoApiKey.equals(mimoApiKey)) {
            return true;
        }
        // MiMo 模型类型
        MimoModel selectedMimoModel = MimoModel.fromIndex(mimoModelTypeComboBox.getSelectedIndex());
        MimoModel currentMimoModel = MimoModel.fromModelId(settings.getMimoModelType());
        if (selectedMimoModel != currentMimoModel) {
            return true;
        }
        // 音色
        OptionItem voiceRoleSelectedItem = (OptionItem) voiceRoleGroupComboBox.getSelectedItem();
        if (voiceRoleSelectedItem != null && !settings.getVoiceRole().equals(voiceRoleSelectedItem.getText())) {
            return true;
        }
        // 音色描述
        String voiceRoleDescription = mimoVoiceDescTextArea.getText() == null ? "" : mimoVoiceDescTextArea.getText();
        String currentVoiceDescription = settings.getMimoVoiceDescription() == null ? "" : settings.getMimoVoiceDescription();
        if (!currentVoiceDescription.equals(voiceRoleDescription)) {
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
            selectedIconStyle = SettingConstants.ICON_STYLE_DEFAULT;
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

        // 状态栏字体大小
        int newFontSize = (Integer) fontSizeSpinner.getValue();
        cacheService.setFontSize(newFontSize);
        // 状态栏字体颜色
        Color newColor = fontColorPreview.getBackground();
        String newColorHex = String.format("#%02X%02X%02X", newColor.getRed(), newColor.getGreen(), newColor.getBlue());
        cacheService.setFontColorHex(newColorHex);

        // TTS引擎
        String selectedEngine = (String) ttsEngineComboBox.getSelectedItem();
        settings.setTtsEngine(selectedEngine);
        
        // MiMo API Key - 保存到 CredentialService
        CredentialService.getInstance().saveMimoApiKey(new String(mimoApiKeyTextField.getPassword()));

        // MiMo 模型类型和音色描述
        if (TtsEngineEnum.MIMO.getEngineId().equals(selectedEngine)) {
            MimoModel selectedModel = MimoModel.fromIndex(mimoModelTypeComboBox.getSelectedIndex());
            settings.setMimoModelType(selectedModel.getModelId());
            settings.setMimoVoiceDescription(mimoVoiceDescTextArea.getText());
        }

        // 保存音色 - 根据引擎类型处理
        OptionItem voiceRoleSelectedItem = (OptionItem) voiceRoleGroupComboBox.getSelectedItem();
        if (voiceRoleSelectedItem != null) {
            if (TtsEngineEnum.MIMO.getEngineId().equals(selectedEngine)) {
                // MiMo TTS: 将描述转换为值
                String description = voiceRoleSelectedItem.getText();
                Voice mimoVoice = findVoiceByDescription(description);
                settings.setVoiceRole(mimoVoice != null ? mimoVoice.getValue() : Voice.MIMO_DEFAULT.getValue());
            } else {
                // Edge TTS: 直接保存昵称
                settings.setVoiceRole(voiceRoleSelectedItem.getText());
            }
        }
        
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

        // 更新状态栏字体
        ReaderStatusBarWidget.updateFont(project);
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
        selectIconStyleRadioButtonGroup = new ButtonGroup();
        GridLayoutManager selectIconStylePanelLayoutManager = new GridLayoutManager(1, 3);
        selectIconStylePanel.setLayout(selectIconStylePanelLayoutManager);
        selectedIconStyle = settings.getMainIconStyle();
        for (int i = 0, len = SettingConstants.ICON_STYLE_NAMES.length; i < len; i++) {
            JBRadioButton radioButtons = new JBRadioButton();
            radioButtons.setText(SettingConstants.ICON_STYLE_NAMES[i]);
            radioButtons.setActionCommand(String.valueOf(SettingConstants.ICON_STYLE_VALUES[i]));
            if (selectedIconStyle <= 0 && i == 0) {
                radioButtons.setSelected(true);
            } else if (selectedIconStyle == SettingConstants.ICON_STYLE_VALUES[i]) {
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
        editorMessageWindosWidthComboBox.setModel(new DefaultComboBoxModel<>(SettingConstants.EDITOR_HINT_WIDTHS));
        editorMessageWindosWidthComboBox.setEditable(true);
        ComboBoxEditor editorMessageWindowWidthEditor = editorMessageWindosWidthComboBox.getEditor();
        editorMessageWindowWidthEditor.setItem(settings.getEditorHintWidth());
        // 编辑器消息窗口--高度
        editorMessageWindosHeightComboBox.setModel(new DefaultComboBoxModel<>(SettingConstants.EDITOR_HINT_HEIGHTS));
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
     * 创建TTS引擎选择UI
     */
    private void createUITtsEngine() {
        // TTS引擎选择
        ttsEngineComboBox.setModel(new DefaultComboBoxModel<>(TtsEngineEnum.getEngineIds()));
        ttsEngineComboBox.setSelectedItem(settings.getTtsEngine() != null ? settings.getTtsEngine() : TtsEngineEnum.EDGE.getEngineId());

        // MiMo API Key - 从 CredentialService 加载
        String savedApiKey = CredentialService.getInstance().getMimoApiKey();
        mimoApiKeyTextField.setText(savedApiKey != null ? savedApiKey : "");

        // API Key 提示图标和链接
        TtsEngineEnum mimoEngine = TtsEngineEnum.MIMO;
        mimoApiKeyHintLabel.setIcon(AllIcons.General.Information);
        mimoApiKeyHintLabel.setToolTipText(mimoEngine.getApiKeyHint());
        mimoApiKeyLink.setText("API Keys");
        mimoApiKeyLink.setExternalLinkIcon();
        mimoApiKeyLink.addActionListener(e -> {
            BrowserUtil.browse(mimoEngine.getApiKeyUrl());
        });

        // MiMo 模型类型选择
        MimoModel[] mimoModels = MimoModel.values();
        String[] modelDisplayNames = new String[mimoModels.length];
        for (int i = 0; i < mimoModels.length; i++) {
            modelDisplayNames[i] = mimoModels[i].getDisplayName();
        }
        mimoModelTypeComboBox.setModel(new DefaultComboBoxModel<>(modelDisplayNames));

        MimoModel currentModel = MimoModel.fromModelId(settings.getMimoModelType());
        mimoModelTypeComboBox.setSelectedIndex(currentModel.toIndex());

        // MiMo 音色描述
        // 更新音色描述label文字
        mimoVoiceDescLabel.setText(currentModel.getVoiceDescriptionLabel());
        String voiceDesc = settings.getMimoVoiceDescription();
        mimoVoiceDescTextArea.setText(voiceDesc != null ? voiceDesc : "");
        mimoVoiceDescTextArea.setLineWrap(true);
        mimoVoiceDescTextArea.setWrapStyleWord(true);

        // 音色描述预设按钮
        changeVoiceDescPreset = currentModel.getVoiceStylePresets().length > 0 ?
                currentModel.getVoiceStylePresets() : SettingConstants.VOICE_DESC_PRESETS;
        mimoVoiceDescPresetButton.addActionListener(e -> showVoiceDescPresetDialog());

        // 音色描述提示文字
        mimoVoiceDescHintLabel.setText(currentModel.getVoiceDescriptionTip());
        mimoVoiceDescHintLabel.setForeground(UIManager.getColor("Component.infoForeground"));
        mimoVoiceDescHintLabel.setBorder(JBUI.Borders.emptyLeft(0));

        // 模型类型切换监听器 - VoiceDesign 时隐藏预置音色选择
        mimoModelTypeComboBox.addActionListener(e -> {
            MimoModel selectedModel = MimoModel.fromIndex(mimoModelTypeComboBox.getSelectedIndex());
            boolean isVoiceDesign = (selectedModel == MimoModel.VOICE_DESIGN);
            // VoiceDesign 模型时隐藏预置音色选择
            voiceRoleGroupComboBox.setVisible(!isVoiceDesign);
            // 更新音色描述label文字
            mimoVoiceDescLabel.setText(selectedModel.getVoiceDescriptionLabel());
            // 更新音色描述提示文字
            mimoVoiceDescHintLabel.setText(selectedModel.getVoiceDescriptionTip());
            changeVoiceDescriptionLabel = selectedModel.getVoiceDescriptionLabel();
            if (selectedModel != MimoModel.VOICE_CLONE) {
                // 更新音色描述预设
                changeVoiceDescPreset = selectedModel.getVoiceStylePresets();
                mimoVoiceDescPresetButton.setVisible(true);
            } else {
                // 音色描述预设为空
                changeVoiceDescPreset = new String[][]{};
                mimoVoiceDescPresetButton.setVisible(false);
            }
        });

        // TTS引擎切换监听器
        ttsEngineComboBox.addActionListener(e -> {
            String selectedEngine = (String) ttsEngineComboBox.getSelectedItem();
            updateUIVisibilityForEngine(selectedEngine);
            updateVoiceRoleAndStyleForEngine(selectedEngine);
        });

        // 初始化时根据当前引擎更新音色和风格选项
        updateVoiceRoleAndStyleForEngine(settings.getTtsEngine() != null ? settings.getTtsEngine() : TtsEngineEnum.EDGE.getEngineId());
        
        // 初始化UI可见性
        updateUIVisibilityForEngine(settings.getTtsEngine() != null ? settings.getTtsEngine() : TtsEngineEnum.EDGE.getEngineId());
    }

    /**
     * 根据TTS引擎更新UI组件可见性
     */
    private void updateUIVisibilityForEngine(String engineType) {
        boolean isMiMo = TtsEngineEnum.MIMO.getEngineId().equals(engineType);
        MimoModel selectedModel = MimoModel.fromIndex(mimoModelTypeComboBox.getSelectedIndex());
        boolean isVoiceDesign = isMiMo && (selectedModel == MimoModel.VOICE_DESIGN);
        
        // MiMo 专属组件
        mimoApiKeyLabel.setVisible(isMiMo);
        mimoApiKeyTextField.setVisible(isMiMo);
        mimoApiKeyHintLabel.setVisible(isMiMo);
        mimoApiKeyLink.setVisible(isMiMo);
        mimoModelTypeLabel.setVisible(isMiMo);
        mimoModelTypeComboBox.setVisible(isMiMo);
        mimoVoiceDescLabel.setVisible(isMiMo);  // MiMo 两个模型都需要音色描述
        mimoVoiceDescTextAreaScroll.setVisible(isMiMo);
        mimoVoiceDescTextArea.setVisible(isMiMo);
        mimoVoiceDescPresetButton.setVisible(isMiMo);
        mimoVoiceDescHintLabel.setVisible(isMiMo);
        
        // Edge TTS 专属组件 - 无（音色和风格两个引擎共用）
        
        // 预置音色选择 - VoiceDesign 时隐藏
        if (voiceRoleGroupComboBox != null) {
            voiceRoleGroupComboBox.setVisible(!isVoiceDesign);
        }
    }

    /**
     * 显示音色描述预设对话框
     */
    private void showVoiceDescPresetDialog() {
        // 创建选择列表
        String[] names = new String[changeVoiceDescPreset.length];
        for (int i = 0; i < changeVoiceDescPreset.length; i++) {
            names[i] = changeVoiceDescPreset[i][0];
        }

        JBList<String> presetList = new JBList<>(names);
        presetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presetList.setSelectedIndex(0);

        // 预览区域
        JTextArea previewArea = new JTextArea();
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setEditable(false);
        previewArea.setRows(SettingConstants.PRESET_PREVIEW_ROWS);
        previewArea.setText(changeVoiceDescPreset[0][1]);
        previewArea.setBorder(JBUI.Borders.empty(SettingConstants.PRESET_PREVIEW_PADDING));

        // 列表选择监听器 - 更新预览
        presetList.addListSelectionListener(e -> {
            int idx = presetList.getSelectedIndex();
            if (idx >= 0 && idx < changeVoiceDescPreset.length) {
                previewArea.setText(changeVoiceDescPreset[idx][1]);
            }
        });

        // 使用 DialogBuilder 创建 IntelliJ 风格对话框
        DialogBuilder builder = new DialogBuilder(mimoVoiceDescTextArea.getParent());
        builder.setTitle(changeVoiceDescriptionLabel + SettingConstants.PRESET_DIALOG_TITLE);
        builder.setDimensionServiceKey(SettingConstants.PRESET_DIALOG_DIMENSION_KEY);

        // 构建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(SettingConstants.PRESET_PANEL_PADDING, SettingConstants.PRESET_PANEL_PADDING));
        mainPanel.setBorder(JBUI.Borders.empty(SettingConstants.PRESET_PANEL_PADDING));

        // 顶部提示
        JLabel hintLabel = new JLabel(SettingConstants.PRESET_DIALOG_HINT + changeVoiceDescriptionLabel);
        hintLabel.setBorder(JBUI.Borders.emptyBottom(SettingConstants.PRESET_HINT_BOTTOM_PADDING));
        mainPanel.add(hintLabel, BorderLayout.NORTH);

        // 左右分栏
        JBSplitter splitter = new JBSplitter(false, SettingConstants.PRESET_SPLITTER_PROPORTION);
        splitter.setFirstComponent(new JBScrollPane(presetList));
        splitter.setSecondComponent(new JBScrollPane(previewArea));
        mainPanel.add(splitter, BorderLayout.CENTER);

        // 底部提示
        JLabel tipLabel = new JLabel(SettingConstants.PRESET_DIALOG_TIP + changeVoiceDescriptionLabel + "内容。");
        tipLabel.setForeground(UIManager.getColor("Component.infoForeground"));
        tipLabel.setBorder(JBUI.Borders.emptyTop(SettingConstants.PRESET_TIP_TOP_PADDING));
        mainPanel.add(tipLabel, BorderLayout.SOUTH);

        mainPanel.setPreferredSize(new Dimension(SettingConstants.PRESET_DIALOG_WIDTH, SettingConstants.PRESET_DIALOG_HEIGHT));

        builder.setCenterPanel(mainPanel);

        // 设置确定按钮回调
        builder.setOkOperation(() -> {
            int idx = presetList.getSelectedIndex();
            if (idx >= 0 && idx < changeVoiceDescPreset.length) {
                mimoVoiceDescTextArea.setText(changeVoiceDescPreset[idx][1]);
            }
            builder.getDialogWrapper().close(0);
        });

        builder.show();
    }

    /**
     * 根据TTS引擎更新音色和风格选项
     */
    private void updateVoiceRoleAndStyleForEngine(String engineType) {
        if (TtsEngineEnum.MIMO.getEngineId().equals(engineType)) {
            // MiMo TTS 音色 - 使用描述性名称
            Voice[] mimoVoices = Voice.values();
            Map<String, List<String>> mimoNicknameByLocale = new HashMap<>();
            List<String> mimoVoicesList = new ArrayList<>();
            for (Voice voice : mimoVoices) {
                mimoVoicesList.add(voice.getDescription());
            }
            mimoNicknameByLocale.put(TtsEngineEnum.MIMO.getVoiceGroupName(), mimoVoicesList);

            GroupedComboBox mimoGroupedComboBox = new GroupedComboBox();
            voiceRoleGroupComboBox = mimoGroupedComboBox.buildGroupedComboBox(mimoNicknameByLocale);
            
            // 设置当前选中的音色
            String currentVoice = settings.getVoiceRole();
            if (currentVoice != null) {
                Voice voice = Voice.fromValue(currentVoice);
                mimoGroupedComboBox.setSelectedItem(voice.getDescription());
            }

            // 更新风格选项 - MiMo TTS 支持多种风格标签，可编辑
            com.wei.wreader.tts.mimo.enums.VoiceStyle[] mimoStyles = com.wei.wreader.tts.mimo.enums.VoiceStyle.values();
            String[] mimoStyleNames = new String[mimoStyles.length + 1];
            mimoStyleNames[0] = SettingConstants.STYLE_DEFAULT;
            for (int i = 0; i < mimoStyles.length; i++) {
                mimoStyleNames[i + 1] = mimoStyles[i].getValue();
            }
            audioStyleComboBox.setModel(new DefaultComboBoxModel<>(mimoStyleNames));
            audioStyleComboBox.setEditable(true); // MiMo 风格可编辑
            
            // 设置当前选中的风格
            String currentStyle = settings.getAudioStyle();
            if (currentStyle != null && !currentStyle.isEmpty()) {
                audioStyleComboBox.setSelectedItem(currentStyle);
            } else {
                audioStyleComboBox.setSelectedIndex(0);
            }
        } else {
            // Edge TTS 音色
            Map<String, List<String>> nicknameByLocale = VoiceRole.getNicknameByLocaleGender();
            GroupedComboBox voiceRoleGroupedComboBox = new GroupedComboBox();
            voiceRoleGroupComboBox = voiceRoleGroupedComboBox.buildGroupedComboBox(nicknameByLocale);
            voiceRoleGroupedComboBox.setSelectedItem(settings.getVoiceRole());

            // 添加音色切换监听器
            voiceRoleGroupComboBox.addActionListener(e1 -> {
                OptionItem voiceRoleSelectedItem = (OptionItem) voiceRoleGroupComboBox.getSelectedItem();
                if (voiceRoleSelectedItem != null) {
                    // 获取音色拥有的风格
                    VoiceStyle[] voiceStyles = VoiceRoleStyle.getByRoleNickName(voiceRoleSelectedItem.getText());
                    if (voiceStyles.length == 0) {
                        audioStyleComboBox.setModel(new DefaultComboBoxModel<>(new String[]{VoiceStyle.style_default.name}));
                    } else {
                        String[] voiceStyleStrs = new String[voiceStyles.length];
                        for (int i = 0; i < voiceStyles.length; i++) {
                            voiceStyleStrs[i] = voiceStyles[i].name;
                        }
                        audioStyleComboBox.setModel(new DefaultComboBoxModel<>(voiceStyleStrs));
                    }
                    audioStyleComboBox.setEditable(false); // Edge 风格不可编辑
                    audioStyleComboBox.setSelectedIndex(0);
                }
            });

            // 更新风格选项
            String voiceRole = settings.getVoiceRole();
            VoiceStyle[] voiceStyles = VoiceRoleStyle.getByRoleNickName(voiceRole);
            if (voiceStyles.length == 0) {
                audioStyleComboBox.setModel(new DefaultComboBoxModel<>(new String[]{VoiceStyle.style_default.name}));
            } else {
                String[] voiceStyleStrs = new String[voiceStyles.length];
                for (int i = 0; i < voiceStyles.length; i++) {
                    voiceStyleStrs[i] = voiceStyles[i].name;
                }
                audioStyleComboBox.setModel(new DefaultComboBoxModel<>(voiceStyleStrs));
            }
            audioStyleComboBox.setEditable(false); // Edge 风格不可编辑
            
            // 设置当前选中的风格
            String currentStyle = settings.getAudioStyle();
            if (currentStyle != null && !currentStyle.isEmpty()) {
                audioStyleComboBox.setSelectedItem(currentStyle);
            } else {
                audioStyleComboBox.setSelectedIndex(0);
            }
        }

        // 更新 voiceRoleJPanel
        voiceRoleJPanel.removeAll();
        GridConstraints voiceRoleGridConstraints = new GridConstraints();
        voiceRoleGridConstraints.setRow(0);
        voiceRoleGridConstraints.setColumn(0);
        voiceRoleJPanel.add(voiceRoleGroupComboBox, voiceRoleGridConstraints);
        voiceRoleJPanel.revalidate();
        voiceRoleJPanel.repaint();
    }

    /**
     * 根据描述查找 MiMo Voice
     */
    private Voice findVoiceByDescription(String description) {
        for (Voice voice : Voice.values()) {
            if (voice.getDescription().equals(description)) {
                return voice;
            }
        }
        return null;
    }

    /**
     * 创建音色设置UI
     */
    private void createUIVoiceRole() {
        // 音色组件已在 createUITtsEngine() 的 updateVoiceRoleAndStyleForEngine() 中初始化
        // 这里只需要为 Edge TTS 添加音色切换监听器
        String currentEngine = settings.getTtsEngine() != null ? settings.getTtsEngine() : TtsEngineEnum.EDGE.getEngineId();
        
        if (!TtsEngineEnum.MIMO.getEngineId().equals(currentEngine)) {
            // Edge TTS 音色切换监听器
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
                        audioStyleComboBox.setModel(new DefaultComboBoxModel<>(voiceStyleStrs));
                    }
                    audioStyleComboBox.setEditable(false);
                    audioStyleComboBox.setSelectedIndex(0);
                }
            });
        }
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
        rateComboBox.setModel(new DefaultComboBoxModel<>(SettingConstants.RATE_OPTIONS));
        rateComboBox.setEditable(true);
        ComboBoxEditor rateEditor = rateComboBox.getEditor();
        rateEditor.setItem(settings.getRate());
    }
    /**
     * 创建音频音量设置UI
     */
    private void createUIVolume() {
        volumeComboBox.setModel(new DefaultComboBoxModel<>(SettingConstants.VOLUME_OPTIONS));
        volumeComboBox.setEditable(true);
        ComboBoxEditor volumeEditor = volumeComboBox.getEditor();
        volumeEditor.setItem(settings.getVolume());
    }
    /**
     * 创建音频风格设置UI
     * 注意：风格选项已在 updateVoiceRoleAndStyleForEngine() 中初始化
     * 这里只确保选中正确的风格
     */
    private void createUIAudioStyle() {
        String currentEngine = settings.getTtsEngine() != null ? settings.getTtsEngine() : TtsEngineEnum.EDGE.getEngineId();
        
        if (TtsEngineEnum.MIMO.getEngineId().equals(currentEngine)) {
            audioStyleComboBox.setEditable(true);
        } else {
            audioStyleComboBox.setEditable(false);
        }
        
        // 选中当前保存的风格
        String currentStyle = settings.getAudioStyle();
        if (currentStyle != null && !currentStyle.isEmpty()) {
            audioStyleComboBox.setSelectedItem(currentStyle);
        } else {
            audioStyleComboBox.setSelectedIndex(0);
        }
    }

    /**
     * 创建状态栏字体设置UI
     */
    private void createUIStatusBarFont() {
        statusBarFontPanel = new JPanel();
        Border sbBorder = JBUI.Borders.customLine(JBUI.CurrentTheme.Popup.separatorColor(), 1, 0, 0, 0);
        TitledBorder sbFontTitledBorder = new TitledBorder(sbBorder, SettingConstants.BORDER_TITLE_STATUS_BAR_FONT);
        statusBarFontPanel.setBorder(sbFontTitledBorder);
        statusBarFontPanel.setLayout(new GridLayoutManager(1, 4));

        // 字体大小
        JLabel fontSizeLabel = new JLabel("字体大小");
        GridConstraints fontSizeLabelGrid = new GridConstraints();
        fontSizeLabelGrid.setRow(0);
        fontSizeLabelGrid.setColumn(0);
        fontSizeLabelGrid.setAnchor(GridConstraints.ANCHOR_WEST);
        statusBarFontPanel.add(fontSizeLabel, fontSizeLabelGrid);

        int currentFontSize = cacheService.getFontSize();
        if (currentFontSize <= 0) {
            currentFontSize = (int) ConstUtil.DEFAULT_FONT_SIZE;
        }
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(currentFontSize, 8, 72, 1));
        fontSizeSpinner.setPreferredSize(new Dimension(80, 30));
        GridConstraints fontSizeSpinnerGrid = new GridConstraints();
        fontSizeSpinnerGrid.setRow(0);
        fontSizeSpinnerGrid.setColumn(1);
        fontSizeSpinnerGrid.setAnchor(GridConstraints.ANCHOR_WEST);
        statusBarFontPanel.add(fontSizeSpinner, fontSizeSpinnerGrid);

        // 字体颜色
        JLabel fontColorLabel = new JLabel("字体颜色");
        GridConstraints fontColorLabelGrid = new GridConstraints();
        fontColorLabelGrid.setRow(0);
        fontColorLabelGrid.setColumn(2);
        fontColorLabelGrid.setAnchor(GridConstraints.ANCHOR_WEST);
        fontColorLabelGrid.setIndent(5);
        statusBarFontPanel.add(fontColorLabel, fontColorLabelGrid);

        JPanel colorChooserPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        String currentColorHex = cacheService.getFontColorHex();
        if (currentColorHex == null || currentColorHex.isEmpty()) {
            Color foreground = JBUI.CurrentTheme.StatusBar.Widget.FOREGROUND;
            currentColorHex = String.format(
                    "#%02x%02x%02x",
                    foreground.getRed(),
                    foreground.getGreen(),
                    foreground.getBlue()
            );
        }
        Color currentColor;
        try {
            currentColor = Color.decode(currentColorHex);
        } catch (NumberFormatException e) {
            currentColor = JBUI.CurrentTheme.StatusBar.Widget.FOREGROUND;
        }

        fontColorPreview = new JLabel("  ");
        fontColorPreview.setOpaque(true);
        fontColorPreview.setBackground(currentColor);
        fontColorPreview.setPreferredSize(new Dimension(24, 24));
        fontColorPreview.setBorder(BorderFactory.createLineBorder(JBUI.CurrentTheme.Popup.separatorColor()));
        colorChooserPanel.add(fontColorPreview);

        fontColorButton = new JButton("选择颜色");
        fontColorButton.addActionListener(e -> {
            Color chosen = fontManager.changeFontColor(statusBarFontPanel, fontColorPreview.getBackground());
            if (chosen != null) {
                fontColorPreview.setBackground(chosen);
                isModified();
            }
        });
        colorChooserPanel.add(fontColorButton);

        GridConstraints colorPanelGrid = new GridConstraints();
        colorPanelGrid.setRow(0);
        colorPanelGrid.setColumn(3);
        colorPanelGrid.setAnchor(GridConstraints.ANCHOR_WEST);
        statusBarFontPanel.add(colorChooserPanel, colorPanelGrid);

        // 插入到 settingPanel 中，位于 generalPanel 和 audioManagePanel 之间
        insertStatusBarFontPanel();
    }

    /**
     * 将状态栏字体设置面板插入到 settingPanel 中
     */
    private void insertStatusBarFontPanel() {
        // 保存现有子组件
        java.util.List<Component> children = new java.util.ArrayList<>();
        for (int i = 0; i < settingPanel.getComponentCount(); i++) {
            children.add(settingPanel.getComponent(i));
        }
        settingPanel.removeAll();

        // 重新设置布局（增加一行）
        settingPanel.setLayout(new GridLayoutManager(5, 2));

        // Row 0: generalPanel
        GridConstraints generalConstraints = new GridConstraints();
        generalConstraints.setRow(0);
        generalConstraints.setColumn(0);
        generalConstraints.setColSpan(2);
        generalConstraints.setFill(GridConstraints.FILL_BOTH);
        generalConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW);
        settingPanel.add(children.get(0), generalConstraints);

        // Row 1: statusBarFontPanel（新增）
        GridConstraints sbFontConstraints = new GridConstraints();
        sbFontConstraints.setRow(1);
        sbFontConstraints.setColumn(0);
        sbFontConstraints.setColSpan(2);
        sbFontConstraints.setFill(GridConstraints.FILL_BOTH);
        settingPanel.add(statusBarFontPanel, sbFontConstraints);

        // Row 2: audioManagePanel
        GridConstraints audioConstraints = new GridConstraints();
        audioConstraints.setRow(2);
        audioConstraints.setColumn(0);
        audioConstraints.setColSpan(2);
        audioConstraints.setFill(GridConstraints.FILL_BOTH);
        audioConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW);
        settingPanel.add(children.get(1), audioConstraints);

        // Row 3: footerTipPanel
        GridConstraints footerConstraints = new GridConstraints();
        footerConstraints.setRow(3);
        footerConstraints.setColumn(0);
        footerConstraints.setColSpan(2);
        footerConstraints.setFill(GridConstraints.FILL_BOTH);
        footerConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW);
        settingPanel.add(children.get(2), footerConstraints);

        // Row 4: vspacer
        GridConstraints vspacerConstraints = new GridConstraints();
        vspacerConstraints.setRow(4);
        vspacerConstraints.setColumn(0);
        vspacerConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_CAN_GROW);
        settingPanel.add(children.get(3), vspacerConstraints);
    }

    /**
     * 底部提示--联系方式
     */
    private void createUIFooterTipPanel() {
        ActionLink actionLink = new ActionLink();
        actionLink.setIcon(WReaderIcons.LINK_WAY);
        actionLink.setText("联系方式");
        actionLink.addActionListener(e -> {
            Project project = ProjectManager.getInstance().getDefaultProject();
            DialogBuilder dialogBuilder = MessageDialogUtil.showMessageHTML(project, SettingConstants.CONTACT_TITLE,
                    SettingConstants.CONTACT_HTML);
            dialogBuilder.setOkActionEnabled(false);
        });

        footerTipPanel.setLayout(new GridLayoutManager(1, 1));
        GridConstraints labelGrid = new GridConstraints();
        labelGrid.setRow(0);
        labelGrid.setColumn(0);
        // 设置水平位置--左边（ANCHOR_WEST：西，按照上北下南左西右东的顺序，西对应左边）
        labelGrid.setAnchor(GridConstraints.ANCHOR_WEST);
        footerTipPanel.add(actionLink, labelGrid);

    }
}
