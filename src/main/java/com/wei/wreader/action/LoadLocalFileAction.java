package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.wei.wreader.utils.MessageDialogUtil;
import com.wei.wreader.utils.OperateActionUtil;
import com.wei.wreader.widget.GroupedComboBox.CharsetGroupComboBox;
import com.wei.wreader.widget.GroupedComboBox.OptionItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.charset.Charset;
import java.util.SortedMap;

/**
 * 加载本地文件
 *
 * @author weizhanjie
 */
public class LoadLocalFileAction extends BaseAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
        // 创建提示标签
        JLabel charsetLabel = new JLabel("请选择字符集");
        JLabel charsetTipLabel = new JLabel("PS:字符集不正确会导致内容无法加载或乱码");
        // 创建编码选择框
        CharsetGroupComboBox charsetGroupComboBox = new CharsetGroupComboBox();
        ComboBox<String> charsetComboBox = charsetGroupComboBox.buildComboBox();
        charsetGroupComboBox.setSelectedItem(settings.getCharset());
        charsetComboBox.addActionListener(e1 -> {
            OptionItem selectedItem = (OptionItem) charsetComboBox.getSelectedItem();
            if (selectedItem != null) {
                settings.setCharset(selectedItem.getText());
            }
            cacheService.setSettings(settings);
        });

        JLabel isShowImgLabel = new JLabel("是否显示图片");
        JBCheckBox isShowImgCheckBox = new JBCheckBox();
        isShowImgCheckBox.setSelected(settings.isShowLocalImg());
        isShowImgCheckBox.addActionListener(e1 -> {
            settings.setShowLocalImg(isShowImgCheckBox.isSelected());
            cacheService.setSettings(settings);
        });
        // isShowImgLabel 和 isShowImgCheckBox同一行展示
        JPanel isShowImgPanel = new JPanel();
        isShowImgPanel.add(isShowImgLabel);
        isShowImgPanel.add(isShowImgCheckBox);
        isShowImgPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        isShowImgPanel.setLayout(new BoxLayout(isShowImgPanel, BoxLayout.X_AXIS));
        isShowImgPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        Object[] objs = {charsetLabel, charsetTipLabel, charsetComboBox, isShowImgPanel};
        MessageDialogUtil.showMessageDialog(project, "请选择字符集", objs, () -> {
            // 打开文件选择器，并处理文件
            OperateActionUtil.getInstance(project).loadLocalFile();
        });
    }
}
