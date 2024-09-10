package com.wei.wreader.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.wei.wreader.utils.ConfigYaml;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingConfigurable implements Configurable {

    public final String PROJECT_NAME;

    public SettingConfigurable() {
        PROJECT_NAME = ConfigYaml.getInstance().getName();
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return PROJECT_NAME;
    }

    @Override
    public @Nullable JComponent createComponent() {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }
}
