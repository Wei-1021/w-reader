package com.wei.wreader.widget.GroupedComboBox;

/**
 * 组选项实体对象
 * @author weizhanjie
 */
public class OptionItem {
    private final String text;

    public OptionItem(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}