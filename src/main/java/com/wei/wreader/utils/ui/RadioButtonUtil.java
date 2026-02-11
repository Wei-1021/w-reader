package com.wei.wreader.utils.ui;

import com.intellij.ui.Gray;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * 单选按钮工具类
 */
public class RadioButtonUtil {
    /**
     * 创建带自动换行提示文本的单选按钮面板
     */
    public static RadioButtonWithHintResult createRadioButtonWithHint(String title, String value, String hint, boolean selected,
                                                                      int width, int height) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 主单选按钮
        JBRadioButton radioButton = new JBRadioButton(title);
        radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioButton.setSelected(selected);
        radioButton.setActionCommand(value);
        radioButton.setSize(new Dimension(width, height));
        radioButton.setMaximumSize(new Dimension(width, height));
        radioButton.setPreferredSize(new Dimension(width, height));

        // 提示文本（使用JTextArea实现自动换行）
        JBTextArea hintArea = new JBTextArea(hint);
        hintArea.setLineWrap(true);              // 开启自动换行
        hintArea.setWrapStyleWord(true);         // 按单词边界换行
        hintArea.setEditable(false);             // 只读
        hintArea.setOpaque(false);               // 背景透明
        hintArea.setBorder(null);                // 去掉边框
        hintArea.setFont(JBUI.Fonts.label()); // 小字体
        hintArea.setForeground(Gray._147); // 灰色
        hintArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintArea.setSize(new Dimension(width, height));
        hintArea.setMaximumSize(new Dimension(width, height));
        hintArea.setPreferredSize(new Dimension(width, height));

        panel.add(radioButton);
        panel.add(hintArea);


        return new RadioButtonWithHintResult(panel, radioButton, hintArea);
    }

    public static class RadioButtonWithHintResult {
        private JPanel panel;
        private JBRadioButton radioButton;
        private JTextArea hintArea;

        public RadioButtonWithHintResult(JPanel panel, JBRadioButton radioButton, JTextArea hintArea) {
            this.panel = panel;
            this.radioButton = radioButton;
            this.hintArea = hintArea;
        }

        public JPanel getPanel() {
            return panel;
        }

        public void setPanel(JPanel panel) {
            this.panel = panel;
        }

        public JRadioButton getRadioButton() {
            return radioButton;
        }

        public void setRadioButton(JBRadioButton radioButton) {
            this.radioButton = radioButton;
        }

        public JTextArea getHintArea() {
            return hintArea;
        }

        public void setHintArea(JTextArea hintArea) {
            this.hintArea = hintArea;
        }
    }
}
