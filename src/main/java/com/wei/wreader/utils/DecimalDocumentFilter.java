package com.wei.wreader.utils;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

public class DecimalDocumentFilter extends DocumentFilter {
    private int maxDecimalPlaces = 2; // 最大小数位数
    
    public DecimalDocumentFilter() {
        this.maxDecimalPlaces = 2;
    }
    
    public DecimalDocumentFilter(int maxDecimalPlaces) {
        this.maxDecimalPlaces = maxDecimalPlaces;
    }
    
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        String newText = getNewText(fb, offset, string);
        if (isValidNumber(newText)) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        String newText = getReplacedText(fb, offset, length, text);
        if (isValidNumber(newText)) {
            super.replace(fb, offset, length, text, attrs);
        }
    }

    private String getNewText(FilterBypass fb, int offset, String string) 
            throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.insert(offset, string);
        return sb.toString();
    }

    private String getReplacedText(FilterBypass fb, int offset, int length, String text)
            throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, text);
        return sb.toString();
    }

    private boolean isValidNumber(String text) {
        // 允许空字符串
        if (text.isEmpty()) {
            return true;
        }
        
        // 允许负号开头
        if (text.equals("-")) {
            return true;
        }
        
        // 允许小数点
        if (text.equals(".")) {
            return true;
        }
        
        // 允许负号加小数点开头
        if (text.equals("-.")) {
            return true;
        }
        
        // 检查小数位数
        if (text.contains(".")) {
            int decimalIndex = text.indexOf(".");
            // 如果小数点在最后，是有效的
            if (decimalIndex == text.length() - 1) {
                text = text.substring(0, text.length() - 1);
            } else {
                // 检查小数位数
                String decimalPart = text.substring(decimalIndex + 1);
                if (decimalPart.length() > maxDecimalPlaces) {
                    return false;
                }
            }
        }
        
        // 使用正则表达式验证数值格式
        if (text.matches("-?\\d*\\.?\\d*")) {
            // 确保不是以多个零开头（除非是0.xxx格式）
            if (text.startsWith("0") && text.length() > 1 && text.charAt(1) != '.') {
                return false;
            }
            if (text.startsWith("-0") && text.length() > 2 && text.charAt(2) != '.') {
                return false;
            }
            return true;
        }
        
        // 尝试解析为Double来最终验证
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
