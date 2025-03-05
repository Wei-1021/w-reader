package com.wei.wreader.utils;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * List集合工具类
 *
 * @author weizhanjie
 */
public class ListUtil {

    /**
     * 判断当前类似集合是否不为空
     *
     * @param list
     * @return
     */
    public static boolean isNotEmpty(List<?> list) {
        return !isEmpty(list);
    }

    /**
     * 判断当前类似集合是否为空
     *
     * @param list
     * @return
     */
    public static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 判断当前Set集合是否不为空
     *
     * @param set
     * @return
     */
    public static boolean isNotEmpty(Set<?> set) {
        return !isEmpty(set);
    }

    /**
     * 判断当前类似集合是否为空
     *
     * @param set
     * @return
     */
    public static boolean isEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }

    public static int length(List<?> list) {
        return list == null ? 0 : list.size();
    }

    public static int length(Set<?> set) {
        return set == null ? 0 : set.size();
    }

    /**
     * 将List转化成String，各个元素之间以指定的字符进行分隔
     *
     * @param list
     * @param regex
     * @return
     */
    public static String listToStringRegex(List<?> list, String regex) {
        String str = listToString(list);
        if (",".equals(regex)) {
            return str;
        }

        return str.replaceAll(",", regex);
    }

    /**
     * 将List转化成String
     *
     * @param list
     * @return
     */
    public static String listToString(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        String str = list.toString().replaceAll(" ", "")
                .replaceAll("\\[", "")
                .replaceAll("\\]", "");
        str = StringUtil.trim(str);

        // 如果字符串以‘,’开头，则去除开头逗号
        if (!"".equals(str) && str.charAt(0) == ',') {
            str = str.substring(1);
        }

        return str;
    }

    /**
     * 将Set转化成String
     *
     * @param set
     * @return
     */
    public static String setToString(Set<?> set) {
        if (set == null || set.isEmpty()) {
            return "";
        }

        String str = set.toString().replaceAll(" ", "")
                .replaceAll("\\[", "")
                .replaceAll("\\]", "");
        str = StringUtil.trim(str);

        // 如果字符串以‘,’开头，则去除开头逗号
        if (!"".equals(str) && str.charAt(0) == ',') {
            str = str.substring(1);
        }

        return str;
    }

    /**
     * 数组转化成String，其中每个元素都被单引号包裹<br>
     * 如：["a","b","c"] --> "'a', 'b', 'c'"
     *
     * @param array
     * @param <T>
     * @return
     */
    public static <T> String arrayToMarkString(T[] array) {
        return listToMarkString(arraysToList(array), "", "'", "'");
    }

    /**
     * 数组转化成String，其中每个元素都被单引号包裹<br>
     * 如：["a","b","c"] --> "'a', 'b', 'c'"
     *
     * @param array
     * @param <T>
     * @return
     */
    public static <T> String arrayToMarkString(T[] array, String emptyStr) {
        return listToMarkString(arraysToList(array), emptyStr, "'", "'");
    }


    /**
     * 将List转化成String,其中每个元素都被单引号包裹<br>
     * 如：{"a","b","c"} --> "'a', 'b', 'c'"
     *
     * @param list
     * @return
     */
    public static String listToMarkString(List<?> list) {
        return listToMarkString(list, "", "'", "'");
    }

    /**
     * 将List转化成String,其中每个元素都被单引号包裹<br>
     * 如：{"a","b","c"} --> "'a', 'b', 'c'"
     *
     * @param list
     * @return
     */
    public static String listToMarkString(List<?> list, String emptyStr) {
        return listToMarkString(list, emptyStr, "'", "'");
    }

    /**
     * 将List转化成String,其中每个元素都被单引号包裹<br>
     * 如：{"a","b","c"} --> "'a', 'b', 'c'"
     *
     * @param list
     * @return
     */
    public static String listToMarkString(List<?> list, String emptyStr, String startStr, String endStr) {
        if (list == null || list.isEmpty()) {
            return emptyStr;
        }

        return list.stream()
                .map(item -> {
                    if (item instanceof String) {
                        return startStr + ((String) item).trim() + endStr;
                    }
                    return startStr + item + endStr;
                })
                .collect(Collectors.joining(","));
    }

    /**
     * 将List转化成String,其中每个元素都被单引号包裹<br>
     * 如：{"a","b","c"} --> "'a', 'b', 'c'"
     *
     * @param set
     * @return
     */
    public static String setToMarkString(Set<?> set) {
        return setToMarkString(set, "");
    }

    /**
     * 将List转化成String,其中每个元素都被单引号包裹<br>
     * 如：{"a","b","c"} --> "'a', 'b', 'c'"
     *
     * @param set
     * @return
     */
    public static String setToMarkString(Set<?> set, String emptyStr) {
        if (set == null || set.isEmpty()) {
            return emptyStr;
        }

        String res = set.stream()
                .map(item -> "'" + item + "'")
                .collect(Collectors.joining(","));

        return StringUtils.isBlank(res) ? emptyStr : res;
    }

    /**
     * 数组转String
     *
     * @param arr
     * @returnT
     */
    public static <T> String arraysToString(T[] arr) {
        return listToString(arraysToList(arr));
    }

    /**
     * 数组转String，并以指定的分隔符进行分隔
     *
     * @param arr   数组
     * @param regex 分隔符
     * @param <T>
     * @return
     */
    public static <T> String arraysToStringRegex(T[] arr, String regex) {
        return listToStringRegex(arraysToList(arr), regex);
    }

    /**
     * 数组转List集合
     *
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> List<T> arraysToList(T[] arr) {
        if (arr == null || arr.length == 0) {
            return new ArrayList<>();
        }

        List<T> arrayList = new ArrayList<>(arr.length);
        Collections.addAll(arrayList, arr);
        return removeNull(arrayList);
    }


    /**
     * 数组转Set集合
     *
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> Set<T> arraysToSet(T[] arr) {
        if (arr == null || arr.length == 0) {
            return new HashSet<>();
        }

        Set<T> arraySet = new HashSet<>(arr.length);
        Collections.addAll(arraySet, arr);
        return removeSetNull(arraySet);
    }

    /**
     * 去除集合中null值
     *
     * @param list 集合
     * @return
     */
    public static <T> List<T> removeNull(List<? extends T> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        list.removeAll(Collections.singleton(null));
        return (List<T>) list;
    }

    /**
     * 去除集合中null值
     *
     * @param set 集合
     * @return
     */
    public static <T> Set<T> removeSetNull(Set<? extends T> set) {
        if (set == null || set.isEmpty()) {
            return new HashSet<>();
        }

        set.removeAll(Collections.singleton(null));
        return (Set<T>) set;
    }

    /**
     * 将List中某个字段的值抽取出来，并转化成String，其中每个元素都被单引号包裹<br>
     * 如：{"a","b","c"} --> "'a', 'b', 'c'"
     *
     * @param list   实体对象的集合
     * @param mapper 对象属性的getter实例方法引用，如User::getName;
     *               即想要提取的字段的getter方法引用。
     * @return
     */
    public static <T, R> String getOneFieldValueMarkString(List<T> list,
                                                           Function<? super T, ? extends R> mapper) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        return listToMarkString(getFieldValueList(list, mapper));
    }

    /**
     * 将List中某个字段的值抽取出来，并转字符串
     *
     * @param list   实体对象的集合
     * @param mapper 对象属性的getter实例方法引用，如User::getName;
     *               即想要提取的字段的getter方法引用。
     * @return
     */
    public static <T, R> String getOneFieldValueListString(List<T> list,
                                                           Function<? super T, ? extends R> mapper) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        return listToString(getFieldValueList(list, mapper));
    }

    /**
     * 将List中某个字段的值抽取出来，组成另一个List集合
     *
     * @param list   实体对象的集合
     * @param mapper 对象属性的getter实例方法引用，如User::getName;
     *               即想要提取的字段的getter方法引用。
     * @return
     */
    public static <T, R> List<R> getFieldValueList(List<T> list,
                                                   Function<? super T, ? extends R> mapper) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        return removeNull(list.stream().map(mapper).collect(Collectors.toList()));
    }

    /**
     * 将List中某个字段的值抽取出来，组成另一个Set集合
     *
     * @param list   实体对象的集合
     * @param mapper 对象属性的getter实例方法引用，如User::getName;
     *               即想要提取的字段的getter方法引用。
     * @return
     */
    public static <T, R> Set<R> getFieldValueSet(List<T> list,
                                                 Function<? super T, ? extends R> mapper) {
        if (list == null || list.isEmpty()) {
            return new HashSet<>();
        }

        return new HashSet<>(removeNull(list.stream().map(mapper).collect(Collectors.toList())));
    }

    /**
     * 将List中某个字段的值抽取出来，组成另一个Set集合,并转化成String
     *
     * @param list   实体对象的集合
     * @param mapper 对象属性的getter实例方法引用，如User::getName;
     *               即想要提取的字段的getter方法引用。
     * @return
     */
    public static <T, R> String getFieldValueSetString(List<T> list,
                                                       Function<? super T, ? extends R> mapper) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        return setToString(getFieldValueSet(list, mapper));
    }

    /**
     * 将List中某个字段的值抽取出来，组成另一个Set集合,并转化成String，其中每个元素都被单引号包裹<br>
     * 如：{"a","b","c"} --> "'a', 'b', 'c'"
     *
     * @param list   实体对象的集合
     * @param mapper 对象属性的getter实例方法引用，如User::getName;
     *               即想要提取的字段的getter方法引用。
     * @return
     */
    public static <T, R> String getOneFieldValueSetMarkString(List<T> list,
                                                              Function<? super T, ? extends R> mapper) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        return new HashSet<>(getFieldValueList(list, mapper)).stream()
                .map(item -> "'" + item + "'")
                .collect(Collectors.joining(","));
    }

    /**
     * 根据某个字段去重
     *
     * @param list   实体对象的集合
     * @param mapper 对象属性的getter实例方法引用，如User::getName;
     * @return
     */
    public static <T, R extends Comparable<? super R>> List<T> deduplicate(List<T> list,
                                                                           Function<? super T, ? extends R> mapper) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        return deduplicate(list, Comparator.comparing(mapper));
    }

    /**
     * 根据comparator的组合方式去重。<br>
     * 如：{@code Comparator.comparing(o -> o.getUserName() + ";" + o.getMobile())}，
     * 将userName和mobile两个字段组合起来，以此来判断进行去重操作，
     * 若集合中有多个对象的userName和mobile一致，则判断彼此为重复。
     *
     * @param list       实体对象的集合
     * @param comparator Comparator比较器
     * @return
     */
    public static <T> List<T> deduplicate(List<T> list, Comparator<? super T> comparator) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        return list.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(comparator)), ArrayList::new));
    }

}
