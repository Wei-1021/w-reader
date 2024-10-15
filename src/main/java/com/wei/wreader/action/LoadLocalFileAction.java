package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
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
        JLabel tipLabel = new JLabel("请选择字符集");
        JLabel tip2Label = new JLabel("PS:字符集不正确会导致内容无法加载或乱码");

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

        Object[] objs = {tipLabel, tip2Label, charsetComboBox};
        MessageDialogUtil.showMessageDialog(project, "请选择字符集", objs, () -> {
            // 打开文件选择器，并处理文件
            OperateActionUtil.getInstance(project).loadLocalFile();
        });
    }
}
