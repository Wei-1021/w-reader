package com.wei.wreader.utils;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.*;
import java.util.Map;

/**
 * YAML 文件读取工具类
 * 基于 SnakeYAML 2.x 版本库实现 YAML 文件的解析和读取
 */
public class YamlReader {
    private final Yaml yaml;

    /**
     * 默认构造函数，使用基本配置
     */
    public YamlReader() {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(options);
    }

    /**
     * 使用指定类型构造函数的构造函数
     *
     * @param type 要解析的目标类型
     */
    public YamlReader(Class<?> type) {
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new Constructor(type, options));
    }

    /**
     * 使用自定义配置的构造函数
     *
     * @param options 自定义加载选项
     */
    public YamlReader(LoaderOptions options) {
        this.yaml = new Yaml(options);
    }

    /**
     * 使用自定义配置和指定类型的构造函数
     *
     * @param type    要解析的目标类型
     * @param options 自定义加载选项
     */
    public YamlReader(Class<?> type, LoaderOptions options) {
        this.yaml = new Yaml(new Constructor(type, options));
    }

    /**
     * 从文件路径读取 YAML 并解析为指定类型
     *
     * @param filePath 文件路径
     * @param type     目标类型
     * @param <T>      泛型类型
     * @return 解析后的对象
     * @throws IOException   当文件读取失败时抛出
     * @throws YAMLException 当 YAML 解析失败时抛出
     */
    public <T> T readFromFile(String filePath, Class<T> type) throws IOException {
        try (InputStream inputStream = YamlReader.class.getClassLoader().getResourceAsStream(filePath)) {
            return yaml.loadAs(inputStream, type);
        }
    }

    /**
     * 从文件路径读取 YAML 并解析为 Map
     *
     * @param filePath 文件路径
     * @return 包含 YAML 数据的 Map
     * @throws IOException   当文件读取失败时抛出
     * @throws YAMLException 当 YAML 解析失败时抛出
     */
    public Map<String, Object> readFromFile(String filePath) throws IOException {
        try (InputStream inputStream = YamlReader.class.getClassLoader().getResourceAsStream(filePath)) {
            return yaml.load(inputStream);
        }
    }

    /**
     * 从输入流读取 YAML 并解析为指定类型
     *
     * @param inputStream 输入流
     * @param type        目标类型
     * @param <T>         泛型类型
     * @return 解析后的对象
     * @throws YAMLException 当 YAML 解析失败时抛出
     */
    public <T> T readFromStream(InputStream inputStream, Class<T> type) {
        return yaml.loadAs(inputStream, type);
    }

    /**
     * 从输入流读取 YAML 并解析为 Map
     *
     * @param inputStream 输入流
     * @return 包含 YAML 数据的 Map
     * @throws YAMLException 当 YAML 解析失败时抛出
     */
    public Map<String, Object> readFromStream(InputStream inputStream) {
        return yaml.load(inputStream);
    }

    /**
     * 从字符串内容读取 YAML 并解析为指定类型
     *
     * @param content YAML 格式的字符串内容
     * @param type    目标类型
     * @param <T>     泛型类型
     * @return 解析后的对象
     * @throws YAMLException 当 YAML 解析失败时抛出
     */
    public <T> T readFromString(String content, Class<T> type) {
        return yaml.loadAs(content, type);
    }

    /**
     * 从字符串内容读取 YAML 并解析为 Map
     *
     * @param content YAML 格式的字符串内容
     * @return 包含 YAML 数据的 Map
     * @throws YAMLException 当 YAML 解析失败时抛出
     */
    public Map<String, Object> readFromString(String content) {
        return yaml.load(content);
    }

    /**
     * 将 YAML 内容转换为美观的字符串格式（用于调试）
     *
     * @param data 要格式化的数据
     * @return 格式化的 YAML 字符串
     */
    public String dumpAsString(Object data) {
        return yaml.dump(data);
    }

    /**
     * 安全地读取 YAML 文件，防止某些安全漏洞（如 Billion Laughs 攻击）
     *
     * @param filePath 文件路径
     * @param type     目标类型
     * @param <T>      泛型类型
     * @return 解析后的对象
     * @throws IOException 当文件读取失败时抛出
     */
    public <T> T safeReadFromFile(String filePath, Class<T> type) throws IOException {
        // 创建安全的加载选项
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(10 * 1024 * 1024); // 设置字符限制为10MB
        options.setMaxAliasesForCollections(50); // 设置集合别名最大数量
        options.setNestingDepthLimit(50); // 设置嵌套深度限制

        Yaml safeYaml = new Yaml(new Constructor(type, options));

        try (InputStream inputStream = YamlReader.class.getClassLoader().getResourceAsStream(filePath)) {
            return safeYaml.loadAs(inputStream, type);
        }
    }
}