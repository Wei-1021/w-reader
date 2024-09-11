package com.wei.wreader.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.wei.wreader.utils.ConfigYaml;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 设置窗口
 * @author weizhanjie
 */
public class WReaderSettingForm implements Configurable, Configurable.Composite {
    ConfigYaml configYaml;
    private JPanel settingPanel;
    private JTextField textField1;
    private JTextField textField2;

    public WReaderSettingForm() {
        configYaml = ConfigYaml.getInstance();
    }

    /**
     * 配置页面名称
     * @return
     */
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return configYaml.getName();
    }

    /**
     * 获取所有配置页面
     * @return
     */
    @Override
    public Configurable @NotNull [] getConfigurables() {
        return new Configurable[0];
    }

    /**
     * 创建配置页面
     * @return
     */
    @Override
    public @Nullable JComponent createComponent() {
        return settingPanel;
    }

    /**
     * 判断是否修改
     * @return
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * 设置页面点击apply按钮事件
     * @throws ConfigurationException
     */
    @Override
    public void apply() throws ConfigurationException {

    }
}
