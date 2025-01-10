package com.wei.wreader.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.ListSeparator;
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
import com.wei.wreader.utils.NumberUtil;
import com.wei.wreader.utils.tts.VoiceStyle;
import org.apache.commons.lang3.StringUtils;
import com.wei.wreader.utils.tts.VoiceRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.charset.Charset;
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
    private JComboBox charsetComboBox;
    private JLabel charsetLabel;
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
    private JComboBox voiceRoleComboBox;
    private JLabel rateLabel;
    private JLabel volumeLabel;
    private JComboBox rateComboBox;
    private JComboBox volumeComboBox;
    private JComboBox audioStyleComboBox;
    private JLabel audioStyleLabel;
    private ButtonGroup displayTypeRadioGroup;

    private final ConfigYaml configYaml;
    private final CacheService cacheService;
    private Settings settings;
    private int selectedDisplayType;

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
        // 风格
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
        TitledBorder generalTitledBorder = new TitledBorder(border, "general");
        generalPanel.setBorder(generalTitledBorder);

        // 初始化配置页面
        // 单行最大字数
        lineMaxNumsTextField.setDocument(new NumberDocument());
        lineMaxNumsTextField.setText(String.valueOf(settings.getSingleLineChars()));

        // 是否显示行号
        isShowLineNumCheckBox.setSelected(settings.isShowLineNum());

        // 显示类型
        // 创建单选按钮--侧边栏
        sideBarRadioButton = new JBRadioButton();
        sideBarRadioButton.setText(Settings.DISPLAY_TYPE_SIDEBAR_STR);
        // 创建单选按钮--底部状态栏
        statusBarRadioButton = new JBRadioButton();
        statusBarRadioButton.setText(Settings.DISPLAY_TYPE_STATUSBAR_STR);

        // 设置显示类型Panel布局
        GridLayoutManager displayTypeRadioPanelLayoutManager = new GridLayoutManager(1, 3);
        displayTypeRadioPanel.setLayout(displayTypeRadioPanelLayoutManager);

        // 添加单选按钮至布局中
        GridConstraints sideBarRadioGridConstraints = new GridConstraints();
        sideBarRadioGridConstraints.setRow(0);
        sideBarRadioGridConstraints.setColumn(0);
        displayTypeRadioPanel.add(sideBarRadioButton, sideBarRadioGridConstraints);
        GridConstraints statusBarRadioGridConstraints = new GridConstraints();
        statusBarRadioGridConstraints.setRow(0);
        statusBarRadioGridConstraints.setColumn(1);
        displayTypeRadioPanel.add(statusBarRadioButton, statusBarRadioGridConstraints);

        // 字符集
        SortedMap<String, Charset> stringCharsetSortedMap = Charset.availableCharsets();
        for (Map.Entry<String, Charset> entry : stringCharsetSortedMap.entrySet()) {
            charsetComboBox.addItem(entry.getKey());
        }
        charsetComboBox.setSelectedItem(settings.getCharset());

        // 自动阅读
        autoReadTimeTextField.setDocument(new NumberDocument());
        int autoReadTime = settings.getAutoReadTime();
        if (autoReadTime <= 0) {
            autoReadTime = 5;
        }
        autoReadTimeTextField.setText(String.valueOf(autoReadTime));

        int displayTypeTemp = settings.getDisplayType();
        switch (displayTypeTemp) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                sideBarRadioButton.setSelected(true);
                break;
            case Settings.DISPLAY_TYPE_STATUSBAR:
                statusBarRadioButton.setSelected(true);
                break;
            case Settings.DISPLAY_TYPE_EDITOR_BANNER:
//                editorBannerRadioButton.setSelected(true);
                break;
            default:
                sideBarRadioButton.setSelected(true);
                break;
        }

        displayTypeRadioGroup = new ButtonGroup();
        displayTypeRadioGroup.add(sideBarRadioButton);
        displayTypeRadioGroup.add(statusBarRadioButton);
//        displayTypeRadioGroup.add(editorBannerRadioButton);

        // 音频管理
        TitledBorder audioManageTitledBorder = new TitledBorder(border, "Audio Manage");
        audioManagePanel.setBorder(audioManageTitledBorder);
        // 音色
        List<String> voiceRoleNickNames = VoiceRole.getNickNames();
        for (String nickName : voiceRoleNickNames) {
            voiceRoleComboBox.addItem(nickName);
        }
        voiceRoleComboBox.setSelectedItem(settings.getVoiceRole());
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
        selectedDisplayType = Settings.DISPLAY_TYPE_SIDEBAR;
        if (statusBarRadioButton.isSelected()) {
            selectedDisplayType = Settings.DISPLAY_TYPE_STATUSBAR;
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
        if (!settings.getCharset().equals(charsetComboBox.getSelectedItem())) {
            return true;
        }
        // 自动阅读
        String autoReadTime = autoReadTimeTextField.getText();
        if (settings.getAutoReadTime() != NumberUtil.parseInt(autoReadTime)) {
            return true;
        }
        // 音色
        if (!settings.getVoiceRole().equals(voiceRoleComboBox.getSelectedItem())) {
            return true;
        }
        // 音频超时
        if (settings.getAudioTimeout() != NumberUtil.parseInt(timeoutTextField.getText())) {
            return true;
        }
        // 语速
        if (!settings.getRate().equals(rateComboBox.getSelectedItem())) {
            return true;
        }
        // 音量
        if (!settings.getVolume().equals(volumeComboBox.getSelectedItem())) {
            return true;
        }
        // 音频风格
        if (!settings.getAudioStyle().equals(audioStyleComboBox.getSelectedItem())) {
            return true;
        }

        return settings.getDisplayType() != selectedDisplayType;
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
        settings.setDisplayType(selectedDisplayType);
        settings.setCharset((String) charsetComboBox.getSelectedItem());
        settings.setAutoReadTime(NumberUtil.parseInt(autoReadTimeTextField.getText()));
        settings.setVoiceRole((String) voiceRoleComboBox.getSelectedItem());
        settings.setAudioStyle((String) audioStyleComboBox.getSelectedItem());
        settings.setAudioTimeout(NumberUtil.parseInt(timeoutTextField.getText()));

        ComboBoxEditor rateEditor = rateComboBox.getEditor();
        settings.setRate((Float) rateEditor.getItem());

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
