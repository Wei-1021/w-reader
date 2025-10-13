package com.wei.wreader.utils.data;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wei.wreader.pojo.SiteBean;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 语法校验工具
 */
public final class JsonValidator {

    private static final JsonFactory FACTORY = new JsonFactory();

    /**
     * 快速判断一段字符串是否是合法 JSON
     *
     * @param json 待校验字符串
     * @return true=合法；false=非法
     */
    public static boolean isValid(String json) {
        return validate(json).isEmpty();
    }

    /**
     * 校验并返回校验结果
     *
     * @param json  待校验字符串
     * @param clazz 泛型类
     */
    public static <T> ValidationResult validate(String json,
                                                Class<T> clazz) {
        List<ErrorDetail> errorDetailList = validate(json);
        boolean isValid = errorDetailList.isEmpty();
        T bean = null;
        if (isValid) {
            try {
                // 转换
                ObjectMapper mapper = new ObjectMapper();
                bean = mapper.readValue(json, clazz);
            } catch (JsonProcessingException e) {
                errorDetailList.add(new ErrorDetail(0, 0, "List conversion failed."));
            }
        }
        return new ValidationResult(isValid, errorDetailList, bean);
    }

    /**
     * 校验并返回校验结果
     *
     * @param json            待校验字符串
     * @param collectionClass 泛型集合类
     * @param clazz           泛型类
     */
    public static ValidationResult validate(String json,
                                            Class<?> collectionClass,
                                            Class<?> clazz) {
        List<ErrorDetail> errorDetailList = validate(json);
        boolean isValid = errorDetailList.isEmpty();
        List<?> beanList = new ArrayList<>();
        if (isValid) {
            try {
                // 使用jackson转换为集合
                beanList = getCollectionType(json, collectionClass, clazz);
            } catch (JsonProcessingException e) {
                isValid = false;
                errorDetailList.add(new ErrorDetail(0, 0, "List conversion failed."));
            }
        }
        return new ValidationResult(isValid, errorDetailList, beanList);
    }

    /**
     * 校验并返回校验结果（包含字符串转换后的集合）
     *
     * @param json  待校验字符串
     * @param clazz 泛型类
     */
    public static ValidationResult validateList(String json,
                                                Class<?> clazz) {
        List<ErrorDetail> errorDetailList = validate(json);
        boolean isValid = errorDetailList.isEmpty();
        List<?> beanList = new ArrayList<>();
        if (isValid) {
            try {
                // 使用jackson转换为集合
                beanList = getCollectionType(json, List.class, clazz);
            } catch (JsonProcessingException e) {
                isValid = false;
                errorDetailList.add(new ErrorDetail(0, 0, "List conversion failed."));
            }
        }
        return new ValidationResult(isValid, errorDetailList, beanList);
    }

    /**
     * 校验并返回所有错误信息
     *
     * @param json 待校验字符串
     * @return 错误列表；空列表表示合法
     */
    public static List<ErrorDetail> validate(String json) {
        List<ErrorDetail> errors = new ArrayList<>();
        if (json == null) {
            errors.add(new ErrorDetail(0, 0, "输入为 null"));
            return errors;
        }

        try (JsonParser parser = FACTORY.createParser(new StringReader(json))) {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                // 只需驱动解析器往下走，Jackson 会自动检查结构
            }
        } catch (Exception e) {
            // Jackson 抛出的异常里已经带了位置信息
            String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage();
            int line = -1, col = -1;
            if (e instanceof JsonProcessingException) {
                JsonLocation loc =
                        ((JsonProcessingException) e).getLocation();
                line = loc.getLineNr();
                col = loc.getColumnNr();
            }
            errors.add(new ErrorDetail(line, col, msg));
        }

        return errors;
    }


    /**
     * 获取泛型的Collection Type
     *
     * @param collectionClass 泛型的Collection
     * @param elementClasses  实体bean
     * @return 集合
     */
    public static <T> List<T> getCollectionType(String json,
                                                Class<?> collectionClass,
                                                Class<T> elementClasses) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JavaType collectionType = mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
        return mapper.readValue(json, collectionType);
    }

    /* ---------------- 错误模型 ---------------- */
    public static class ErrorDetail {
        private final int line;
        private final int column;
        private final String reason;

        public ErrorDetail(int line, int column, String reason) {
            this.line = line;
            this.column = column;
            this.reason = reason;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return String.format("Line %d, Column %d: %s", line, column, reason);
        }
    }

    /**
     * 带转换的校验结果
     */
    public static class ValidationResult {
        /**
         * 是否校验通过，true表示通过，false表示失败
         */
        private final boolean valid;
        /**
         * 错误列表
         */
        private final List<ErrorDetail> errors;
        /**
         * 转换后的对象集合
         */
        private final List<?> beanList;
        /**
         * 转换后的对象
         */
        private final Object bean;

        public ValidationResult(boolean valid, List<ErrorDetail> errors, List<?> beanList) {
            this.valid = valid;
            this.errors = errors;
            this.beanList = beanList;
            this.bean = null;
        }

        public ValidationResult(boolean valid, List<ErrorDetail> errors, Object bean) {
            this.valid = valid;
            this.errors = errors;
            this.beanList = null;
            this.bean = bean;

        }

        public boolean isValid() {
            return valid;
        }

        public List<ErrorDetail> getErrors() {
            return errors;
        }

        /**
         * 转换后的对象集合
         */
        public <T> List<T> getBeanList() {
            return (List<T>) beanList;
        }

        public Object getBean() {
            return bean;
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors +
                    ", beanList=" + beanList +
                    ", bean=" + bean +
                    '}';
        }
    }

}