package com.wei.wreader.utils;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupedComboBox {

    public ComboBox<Object> buildGroupedComboBox(Map<String, List<String>> map) {
        // 创建分组数据
        List<Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            groups.add(new Group(entry.getKey(), entry.getValue()));
        }

        // 创建 DefaultComboBoxModel
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        for (Group group : groups) {
            model.addElement(group);
            for (String item : group.items) {
                model.addElement(item);
            }
        }

        // 创建 ComboBox 并设置模型和渲染器
        ComboBox<Object> comboBox = new ComboBox<>(model);
        comboBox.setRenderer(new GroupedComboBoxRenderer());

        return comboBox;
    }

    /**
     * 定义分组类
     */
    class Group {
        String groupName;
        List<String> items;

        public Group(String groupName, List<String> items) {
            this.groupName = groupName;
            this.items = items;
        }
    }

    /**
     * 自定义渲染器
     */
    class GroupedComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (c instanceof JLabel label) {
                if (value instanceof Group group) {
                    label.setText(group.groupName);
                    label.setBorder(JBUI.Borders.empty(5));
                    label.setBackground(JBColor.LIGHT_GRAY);
                    label.setForeground(JBColor.BLACK);
                } else if (value instanceof String) {
                    label.setText((String) value); // 添加缩进
                }
            }
            return c;
        }
    }

}
