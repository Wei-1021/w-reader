# OperateActionUtilRefactored 使用说明

## 概述

`OperateActionUtilRefactored` 是对原有 `OperateActionUtil` 类的重构版本，旨在解决原类存在的代码混乱问题，提升代码质量和可维护性。

## 重构改进点

### 1. 结构优化
- **模块化设计**：按照功能职责将代码分为多个逻辑模块
- **消除God Class**：原类约2000+行代码，重构后结构更清晰
- **单一职责原则**：每个方法只负责一个特定功能

### 2. 代码质量提升
- **详细注释**：添加了全面的中文注释说明
- **命名规范**：方法和变量名更具描述性
- **异常处理**：统一的异常处理机制
- **资源管理**：正确的资源释放和任务取消

### 3. 设计模式应用
- **依赖注入**：通过构造函数注入依赖服务
- **状态管理**：集中管理应用状态和配置
- **后台任务**：使用内部类封装后台任务逻辑

## 主要功能模块

### 1. 数据初始化模块
```java
// 核心初始化方法
private void initData()
// 分别初始化各个子模块
private void initializeSettings()
private void initializeFontSettings()
private void initializeSiteInfo()
private void initializeCachedData()
private void initializeCurrentState()
```

### 2. 目录和章节管理模块
```java
// 目录显示
public void showBookDirectory(BookDirectoryListener listener)
// 章节切换
public void prevPageChapter(BiConsumer<ChapterInfo, Element> runnable)
public void nextPageChapter(BiConsumer<ChapterInfo, Element> runnable)
// 远程/本地内容加载
public void loadBookDirectoryRemote(JBList<String> chapterListJBList, BookDirectoryListener listener)
public void loadBookDirectoryLocal(JBList<String> chapterListJBList, BookDirectoryListener listener)
```

### 3. 内容加载和处理模块
```java
// 内容获取
public void searchBookContentRemote(String url, Consumer<SearchBookCallParam> callback)
// 内容处理
public String handleContent(String content) throws Exception
// 下一页内容加载
public void loadThisChapterNextContent(String chapterUrl, String bodyElementStr)
```

### 4. 本地文件处理模块
```java
// 文件加载入口
public void loadLocalFile(String regex)
// TXT格式处理
public void loadFileTypeTxt(File file, String regex)
// EPUB格式处理
public void loadFileTypeEpub(File file)
```

### 5. 自动阅读模块
```java
// 自动阅读控制
public void autoReadNextLine()
// 定时器管理
public void executorServiceShutdown()
```

### 6. TTS语音模块
```java
// 文本转语音
public void ttsChapterContent() throws Exception
// 停止语音
public void stopTTS()
```

### 7. 字体样式管理模块
```java
// 字体大小调整
public void fontSizeSub()
public void fontSizeAdd()
// 字体颜色更改
public void changeFontColor()
// 内容显示更新
public void updateContentText()
```

## 使用示例

### 基本使用
```java
// 获取实例
OperateActionUtilRefactored util = OperateActionUtilRefactored.getInstance(project);

// 显示目录
util.showBookDirectory((selectedIndex, chapterList, chapterInfo, bodyElement) -> {
    // 处理目录项点击
});

// 切换章节
util.nextPageChapter((chapterInfo, bodyElement) -> {
    // 处理章节切换完成
});

// 加载本地文件
util.loadLocalFile(""); // 空字符串使用默认正则表达式
```

### 高级功能
```java
// 启动自动阅读
util.autoReadNextLine();

// 文本转语音
try {
    util.ttsChapterContent();
} catch (Exception e) {
    e.printStackTrace();
}

// 调整字体
util.fontSizeAdd();  // 放大
util.fontSizeSub();  // 缩小
util.changeFontColor();  // 更改颜色
```

## 与原版本的兼容性

### 接口保持一致
- 公共方法签名完全相同
- 返回值类型保持一致
- 异常处理方式相同

### 功能完整性
- 所有原有功能都得到保留
- 性能表现相当或更好
- 用户体验无差异

## 迁移指南

### 逐步替换策略
1. **测试阶段**：先在测试环境中验证重构版本
2. **并行运行**：可以同时保留两个版本进行对比
3. **渐进替换**：逐个模块替换调用代码
4. **完全切换**：确认稳定后移除旧版本

### 注意事项
- 确保所有依赖注入正确配置
- 测试各种边界情况和异常处理
- 验证多线程环境下的稳定性
- 检查内存使用和资源释放

## 性能优化

### 内存管理
- 改进了缓存机制
- 优化了对象创建和销毁
- 减少了不必要的数据复制

### 并发处理
- 合理使用线程池
- 改进任务调度机制
- 优化锁竞争情况

## 未来扩展建议

### 可考虑的改进方向
1. **插件化架构**：将不同功能模块设计为可插拔组件
2. **配置中心**：集中管理所有配置参数
3. **监控统计**：添加使用统计和性能监控
4. **国际化支持**：支持多语言界面

### 扩展点设计
- 内容处理器接口
- 文件格式解析器接口  
- TTS引擎抽象层
- 主题样式管理器

## 维护建议

### 代码维护
- 定期审查和更新注释
- 保持命名规范一致性
- 及时处理技术债务

### 版本管理
- 遵循语义化版本控制
- 维护详细的变更日志
- 建立回归测试套件

这个重构版本在保持原有功能完整性的同时，显著提升了代码的可读性、可维护性和扩展性，为项目的长期发展奠定了良好基础。