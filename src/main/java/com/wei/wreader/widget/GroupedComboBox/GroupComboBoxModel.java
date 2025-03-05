package com.wei.wreader.widget.GroupedComboBox;


import com.intellij.openapi.util.Comparing;

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
    private Object mySelectedItem;

    public GroupComboBoxModel(Map<String, List<String>> map) {
        itemMap = map;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        // 阻止选择组标题
        if (anItem instanceof GroupTitle groupTitle) {
            anItem = getOptionItem(groupTitle);
        } else if (anItem instanceof String itemString) {
            anItem = new OptionItem(itemString);
        }

        if (!Comparing.equal(this.mySelectedItem, anItem)) {
            this.mySelectedItem = anItem;
            this.fireContentsChanged(this, -1, -1);
        }
    }

    @Override
    public Object getSelectedItem() {
        return this.mySelectedItem;
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