package com.wei.wreader.utils.ui.GroupedComboBoxs;

import com.intellij.ui.SimpleTextAttributes;

import javax.swing.*;
import java.awt.*;

/**
 * ComboBox分组渲染器
 *
 * @author weizhanjie
 */
public class GroupComboBoxRenderer implements ListCellRenderer<Object> {
    private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        if (value instanceof GroupTitle groupTitle) {
            // 标题组件
            JPanel panel = new JPanel(new BorderLayout());
            if (index != 0) {
                // 创建分隔线
                panel.add(new JSeparator(), BorderLayout.NORTH);
            }
            JLabel label = new JLabel(groupTitle.getTitle());
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setOpaque(true);
            label.setBackground(UIManager.getColor("Panel.background"));
            label.setForeground(SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());
            panel.add(label, BorderLayout.CENTER);
            return panel;
        }

        // 普通项使用默认渲染，但禁用选中状态
        Component comp = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        comp.setEnabled(true);
        return comp;
    }
}