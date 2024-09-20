package com.wei.wreader.ui;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.NumberDocument;
import com.wei.wreader.factory.WReaderStatusBarFactory;
import com.wei.wreader.factory.WReaderToolWindowFactory;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.ConfigYaml;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
    private JRadioButton sideBarRadioButton;
    private JRadioButton statusBarRadioButton;
    private JRadioButton terminalRadioButton;
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
    public Configurable @NotNull [] getConfigurables() {
        return new Configurable[0];
    }

    /**
     * 创建配置页面
     *
     * @return
     */
    @Override
    public @Nullable JComponent createComponent() {
        // 初始化配置页面
        // 单行最大字数
        lineMaxNumsTextField.setDocument(new NumberDocument());
        lineMaxNumsTextField.setText(String.valueOf(settings.getSingleLineChars()));

        // 是否显示行号
        isShowLineNumCheckBox.setSelected(settings.isShowLineNum());

        // 显示类型
        sideBarRadioButton.setText(Settings.DISPLAY_TYPE_SIDEBAR_STR);
        statusBarRadioButton.setText(Settings.DISPLAY_TYPE_STATUSBAR_STR);
        terminalRadioButton.setText(Settings.DISPLAY_TYPE_TERMINAL_STR);

        int displayTypeTemp = settings.getDisplayType();
        switch (displayTypeTemp) {
            case Settings.DISPLAY_TYPE_SIDEBAR:
                sideBarRadioButton.setSelected(true);
                break;
            case Settings.DISPLAY_TYPE_STATUSBAR:
                statusBarRadioButton.setSelected(true);
                break;
            case Settings.DISPLAY_TYPE_TERMINAL:
                terminalRadioButton.setSelected(true);
                break;
        }

        displayTypeRadioGroup = new ButtonGroup();
        displayTypeRadioGroup.add(sideBarRadioButton);
        displayTypeRadioGroup.add(statusBarRadioButton);
        displayTypeRadioGroup.add(terminalRadioButton);

        return settingPanel;
    }

    /**
     * 判断是否修改
     *
     * @return
     */
    @Override
    public boolean isModified() {
        Settings modifiedSettings = cacheService.getSettings();
        if (modifiedSettings == null) {
            return true;
        }

        String lineMaxNums = lineMaxNumsTextField.getText();
        boolean isShowLineNum = isShowLineNumCheckBox.isSelected();

        if (modifiedSettings.getSingleLineChars() != Integer.parseInt(lineMaxNums)) {
            return true;
        }

        if (modifiedSettings.isShowLineNum() != isShowLineNum) {
            return true;
        }

        selectedDisplayType = Settings.DISPLAY_TYPE_SIDEBAR;
        if (statusBarRadioButton.isSelected()) {
            selectedDisplayType = Settings.DISPLAY_TYPE_STATUSBAR;
        } else if (terminalRadioButton.isSelected()) {
            selectedDisplayType = Settings.DISPLAY_TYPE_TERMINAL;
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
