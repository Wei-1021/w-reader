package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.ui.MessageDialogUtil;

import com.wei.wreader.utils.ui.GroupedComboBoxs.CharsetGroupComboBox;
import com.wei.wreader.utils.ui.GroupedComboBoxs.OptionItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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

        // 是否显示图片
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

        // txt文件解析正则表达式
        JLabel textRegexLabel = new JLabel("txt文件解析正则表达式");
        JTextField textRegexTextField = new JTextField(ConstUtil.TEXT_FILE_DIR_REGEX, 30);
        JPanel textRegexPanel = new JPanel();
        textRegexPanel.add(textRegexLabel);
        textRegexPanel.add(textRegexTextField);
        textRegexPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        textRegexPanel.setLayout(new BoxLayout(textRegexPanel, BoxLayout.Y_AXIS));
        textRegexPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        Object[] objs = {charsetLabel, charsetTipLabel, charsetComboBox, isShowImgPanel, textRegexPanel};
        MessageDialogUtil.showMessageDialog(project, "请选择字符集", objs, () -> {
            // 打开文件选择器，并处理文件
            operateAction.loadLocalFile(textRegexTextField.getText());
        });
    }
}
