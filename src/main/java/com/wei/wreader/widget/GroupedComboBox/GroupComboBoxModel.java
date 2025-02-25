package com.wei.wreader.widget.GroupedComboBox;


import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * ComboBox分组模型
 *
 * @author weizhanjie
 */
public class GroupComboBoxModel extends DefaultComboBoxModel<Object> {

    private static Map<String, List<String>> itemMap;

    public GroupComboBoxModel(Map<String, List<String>> map) {
        itemMap = map;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        // 阻止选择组标题
        if (anItem instanceof GroupTitle groupTitle) {
            OptionItem optionItem = getOptionItem(groupTitle);
            if (optionItem != null) {
                super.setSelectedItem(optionItem);
            }
            return;
        }
        super.setSelectedItem(anItem);
    }

    @Override
    public Object getSelectedItem() {
        Object selectedItem = super.getSelectedItem();
        if (selectedItem instanceof GroupTitle groupTitle) {
            OptionItem optionItem = getOptionItem(groupTitle);
            if (optionItem != null) {
                selectedItem = optionItem.getText();
            }
        } else if (selectedItem instanceof OptionItem optionItem) {
            selectedItem = optionItem.getText();
        }
        return selectedItem;
    }

//    @Override
//    public Object getElementAt(int index) {
//        Object elementAt = super.getElementAt(index);
//        if (elementAt instanceof GroupTitle groupTitle) {
//            OptionItem optionItem = getOptionItem(groupTitle);
//            if (optionItem != null) {
//                elementAt = optionItem.getText();
//            }
//        } else if (elementAt instanceof OptionItem optionItem) {
//            elementAt = optionItem.getText();
//        }
//
//        return elementAt;
//    }

    public OptionItem getOptionItem(Object item) {
        if (item == null) {
            return null;
        }

        if (item instanceof GroupTitle groupTitle) {
            List<String> itemList = itemMap.get(groupTitle.getTitle());
            if (itemList != null && !itemList.isEmpty()) {
                return new OptionItem(itemList.get(0));
            }
        } else if (item instanceof OptionItem) {
            return (OptionItem) item;
        }

        return null;
    }
}