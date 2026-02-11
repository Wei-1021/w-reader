package com.wei.wreader.pojo;

public class LabelValueItem {
    private final String label;
    private final Object value;

    public LabelValueItem(String label, Object value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "LabelValueItem{" +
                "label='" + label + '\'' +
                ", value=" + value +
                '}';
    }
}