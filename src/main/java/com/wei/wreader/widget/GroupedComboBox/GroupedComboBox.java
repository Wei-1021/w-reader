package com.wei.wreader.widget.GroupedComboBox;

import com.intellij.openapi.ui.ComboBox;

import java.util.List;
import java.util.Map;

/**
 * 分组组合下拉选择框
 *
 * @author weizhanjie
 */
public class GroupedComboBox {

    private static GroupComboBoxModel model;
    /**
     * 构建分组组合下拉选择框
     * @param map
     * @return
     */
    public ComboBox<Object> buildGroupedComboBox(Map<String, List<String>> map) {
        // 创建 DefaultComboBoxModel
        model = new GroupComboBoxModel(map);
        // 创建分组数据
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            model.addElement(new GroupTitle(entry.getKey()));
            for (String item : entry.getValue()) {
                model.addElement(new OptionItem(item));
            }
        }

        // 创建 ComboBox 并设置模型和渲染器
        ComboBox<Object> comboBox = new ComboBox<>(model);
        comboBox.setRenderer(new GroupComboBoxRenderer());

        return comboBox;
    }

    public void setSelectedItem(String item) {
        for (int i = 0; i < model.getSize(); i++) {
            Object obj = model.getElementAt(i);
            if (obj instanceof OptionItem optionItem) {
                if (optionItem.getText().equals(item)) {
                    model.setSelectedItem(optionItem);
                }
            } else if (obj instanceof GroupTitle groupTitle) {
                if (groupTitle.getTitle().equals(item)) {
                    model.setSelectedItem(groupTitle);
                }
            }
        }
    }

    /**
     * 定义分组类
     */
    public static class Group {
        String groupName;
        List<String> items;

        public Group(String groupName, List<String> items) {
            this.groupName = groupName;
            this.items = items;
        }
    }


}
