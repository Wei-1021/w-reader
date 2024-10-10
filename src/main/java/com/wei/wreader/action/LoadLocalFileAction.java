package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.MessageDialogUtil;
import com.wei.wreader.utils.OperateActionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.charset.Charset;
import java.util.SortedMap;

/**
 * 加载本地文件
 * @author weizhanjie
 */
public class LoadLocalFileAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
        // 创建提示标签
        JLabel tipLabel = new JLabel("请选择字符集：");

        // 创建编码选择框
        ComboBox<String> charsetComboBox = new ComboBox<>();
        SortedMap<String, Charset> stringCharsetSortedMap = Charset.availableCharsets();
        for (String key : stringCharsetSortedMap.keySet()) {
            charsetComboBox.addItem(key);
        }
        charsetComboBox.setSelectedItem(settings.getCharset());
        // 监听编码选择框
        charsetComboBox.addActionListener(e1 -> {
            settings.setCharset((String) charsetComboBox.getSelectedItem());
            cacheService.setSettings(settings);
        });

        Object[] objs = {tipLabel, charsetComboBox};
        MessageDialogUtil.showMessageDialog(project, "请选择字符集", objs, () -> {
            // 打开文件选择器，并处理文件
            OperateActionUtil.getInstance(project).loadLocalFile();
        });
    }
}
