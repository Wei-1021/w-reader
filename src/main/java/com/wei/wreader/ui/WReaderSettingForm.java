package com.wei.wreader.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.NumberDocument;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.wei.wreader.factory.WReaderStatusBarFactory;
import com.wei.wreader.factory.WReaderToolWindowFactory;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import org.apache.commons.lang3.StringUtils;
import org.dreamwork.tools.tts.VoiceRole;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.nio.charset.Charset;
import java.util.*;

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
        for (String key : stringCharsetSortedMap.keySet()) {
            charsetComboBox.addItem(key);
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
        VoiceRole[] VoiceRoles = VoiceRole.values();
        for (VoiceRole voiceRole : VoiceRoles) {
            voiceRoleComboBox.addItem(voiceRole.name());
        }
        voiceRoleComboBox.setSelectedItem(settings.getVoiceRole());
        // 音频超时
        timeoutTextField.setDocument(new NumberDocument());
        timeoutTextField.setText(String.valueOf(settings.getAudioTimeout()));

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

        Settings modifiedSettings = cacheService.getSettings();
        if (modifiedSettings == null) {
            return true;
        }

        String lineMaxNums = lineMaxNumsTextField.getText();
        boolean isShowLineNum = isShowLineNumCheckBox.isSelected();
        String autoReadTime = autoReadTimeTextField.getText();
        // 单行最大字数
        if (modifiedSettings.getSingleLineChars() != Integer.parseInt(lineMaxNums)) {
            return true;
        }
        // 是否显示行号
        if (modifiedSettings.isShowLineNum() != isShowLineNum) {
            return true;
        }
        // 字符集
        if (!modifiedSettings.getCharset().equals(charsetComboBox.getSelectedItem())) {
            return true;
        }
        // 自动阅读
        if (modifiedSettings.getAutoReadTime() != Integer.parseInt(autoReadTime)) {
            return true;
        }
        // 音色
        if (!modifiedSettings.getVoiceRole().equals(voiceRoleComboBox.getSelectedItem())) {
            return true;
        }
        // 音频超时
        if (modifiedSettings.getAudioTimeout() != Integer.parseInt(timeoutTextField.getText())) {
            return true;
        }

        return modifiedSettings.getDisplayType() != selectedDisplayType;
    }


    /**
     * 设置页面点击apply按钮事件
     *
     * @throws ConfigurationException
     */
    @Override
    public void apply() throws ConfigurationException {
        settings.setSingleLineChars(Integer.parseInt(lineMaxNumsTextField.getText()));
        settings.setShowLineNum(isShowLineNumCheckBox.isSelected());
        settings.setDisplayType(selectedDisplayType);
        settings.setCharset((String) charsetComboBox.getSelectedItem());
        settings.setAutoReadTime(Integer.parseInt(autoReadTimeTextField.getText()));
        settings.setVoiceRole((String) voiceRoleComboBox.getSelectedItem());
        settings.setAudioTimeout(Integer.parseInt(timeoutTextField.getText()));
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
