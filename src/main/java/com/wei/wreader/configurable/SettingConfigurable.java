package com.wei.wreader.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.wei.wreader.ui.WReaderSettingForm;
import com.wei.wreader.utils.yml.ConfigYaml;

import javax.swing.*;

/**
 * 设置窗口
 * @author weizhanjie
 */
public class SettingConfigurable implements Configurable, Configurable.Composite {

    public final String PROJECT_NAME;
    public WReaderSettingForm wReaderSettingForm;

    public SettingConfigurable() {
        PROJECT_NAME = ConfigYaml.getInstance().getName();
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return PROJECT_NAME;
    }

    @Override
    public JComponent createComponent() {
        wReaderSettingForm = new WReaderSettingForm();
        return wReaderSettingForm.createComponent();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public Configurable [] getConfigurables() {
        return new Configurable[0];
    }
}
