package com.wei.wreader.widget.GroupedComboBox;


import com.intellij.openapi.ui.ComboBox;

import java.nio.charset.Charset;
import java.util.*;

public class CharsetGroupComboBox extends GroupedComboBox {
    // 常用字符集
    private static final String[] COMMON_CHARSET = {"UTF-8", "UTF-16", "GBK", "ISO-8859-1", "US-ASCII"};

    public <T> ComboBox<T> buildComboBox() {
        // 数组转List集合
        List<String> commonCharsetList = Arrays.asList(COMMON_CHARSET);
        // 字符集
        SortedMap<String, Charset> stringCharsetSortedMap = Charset.availableCharsets();
        // 遍历字符集，将字符集分成"常用"和"其它"两个分组
        Map<String, List<String>> charsetMap = new LinkedHashMap<>();
        charsetMap.put("common", new ArrayList<>());
        charsetMap.put("other", new ArrayList<>());
        for (Map.Entry<String, Charset> entry : stringCharsetSortedMap.entrySet()) {
            String charset = entry.getKey().toUpperCase();
            if (commonCharsetList.contains(charset)) {
                List<String> charsetList = charsetMap.get("common");
                charsetList.add(entry.getKey());
                charsetMap.put("common", charsetList);
            } else {
                List<String> charsetList = charsetMap.get("other");
                charsetList.add(entry.getKey());
                charsetMap.put("other", charsetList);
            }
        }

        return (ComboBox<T>) buildGroupedComboBox(charsetMap);
    }
}
