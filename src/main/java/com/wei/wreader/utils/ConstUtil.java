package com.wei.wreader.utils;

/**
 * 常量工具类
 * @author weizhanjie
 */
public class ConstUtil {
    public static final String WREADER_ID = "WReader";
    public static final String WREADER_STATUS_BAR_WIDGET_ID = "wreader.WReaderStatusBarWidget";
    public static final String WREADER_STATUS_BAR_ID = "wreader.WReaderStatusBar";
    /**
     * 配置文件id--设置
     */
    public static final String WREADER_SETTINGS_ID = "wreader.WReaderSetting";
    /**
     * 配置文件id--搜索
     */
    public static final String WREADER_SEARCH_ID = "wreader.SearchBookName";
    /**
     * 配置文件id--目录
     */
    public static final String WREADER_CHAPTER_LIST_ID = "wreader.ChapterListAction";
    /**
     * 配置文件id--上一章
     */
    public static final String WREADER_PREV_CHAPTER_ID = "wreader.PrevChapterAction";
    /**
     * 配置文件id--下一章
     */
    public static final String WREADER_NEXT_CHAPTER_ID = "wreader.NextChapterAction";
    /**
     * 配置文件id--上一行
     */
    public static final String WREADER_PREV_LINE_ID = "wreader.PrevLineAction";
    /**
     * 配置文件id--下一行
     */
    public static final String WREADER_NEXT_LINE_ID = "wreader.NextLineAction";
    /**
     * 配置文件id--字体缩小
     */
    public static final String WREADER_FONT_SIZE_SUB_ID = "wreader.FontSizeSubAction";
    /**
     * 配置文件id--字体放大
     */
    public static final String WREADER_FONT_SIZE_ADD_ID = "wreader.FontSizeAddAction";
    /**
     * 配置文件id--改变字体颜色
     */
    public static final String WREADER_CHANGE_FONT_COLOR_ID = "wreader.ChangeFontColorAction";
    /**
     * 配置文件id--加载本地文件
     */
    public static final String WREADER_LOAD_LOCAL_FILE_ID = "wreader.LoadLocalFileAction";
    /**
     * 工具窗口id
     */
    public static final String WREADER_TOOL_WINDOW_TOOL_BAR_ID = "wreader.toolWindowToolBar";
    /**
     * 工具窗口id
     */
    public static final String WREADER_TOOL_WINDOW_ID = "wreader";
    /**
     * 配置文件id--组--WReader
     */
    public static final String WREADER_GROUP_ID = "wreader.group.WReader";
    /**
     * 配置文件id--组--远程
     */
    public static final String WREADER_GROUP_TOOLMENU_ID = "wreader.group.toolMenu";
    /**
     * 配置文件id--组--工具窗口的工具栏
     */
    public static final String WREADER_GROUP_TOOL_WINDOW_BAR_ID = "wreader.group.toolWindowBar";
    /**
     * 配置文件id--组--状态栏
     */
    public static final String WREADER_GROUP_STATUS_BAR_ID = "wreader.group.statusBar";
    /**
     * 工具窗口标题
     */
    public static final String WREADER_TOOL_WINDOW_TITLE = "WReader";
    /**
     * 工具窗口初始化内容
     */
    public static final String WREADER_TOOL_WINDOW_CONTENT_INIT_TEXT =
            """
            __      _____    ___    ___    ___    ___    ___  \s
            \\ \\    / /| _ \\  | __|  /   \\  |   \\  | __|  | _ \\ \s
             \\ \\/\\/ / |   /  | _|   | - |  | |) | | _|   |   / \s
              \\_/\\_/  |_|_\\  |___|  |_|_|  |___/  |___|  |_|_\\ \s
            _|""\"""_|""\"""_|""\"""_|""\"""_|""\"""_|""\"""_|""\"""|\s
            "`-0-0-"`-0-0-"`-0-0-"`-0-0-"`-0-0-"`-0-0-"`-0-0-'\s
            """;

    public static final String HTTP_SCHEME = "http://";
    public static final String HTTPS_SCHEME = "https://";
    public static final String WREADER_SEARCH_BOOK_TITLE = "搜索小说";
    public static final String WREADER_SEARCH_BOOK_TIP_TEXT = "请输入书名进行搜索";
    public static final String WREADER_LOAD_SUCCESS = "加载成功";
    public static final String WREADER_SEARCH_EMPTY = "书名不能为空";
    public static final String WREADER_SEARCH_NETWORK_ERROR = "网络请求异常，请重试！";
    public static final String WREADER_SEARCH_BOOK_ERROR = "没有找到相关书籍";
    public static final String WREADER_SEARCH_BOOK_CONTENT_ERROR = "未找到书本内容";
    public static final String WREADER_ERROR = "出现异常，请重试！";
    public static final String WREADER_UNSUPPORTED_FILE_TYPE = "不支持此类型的文件！";
    public static final String WREADER_ONLY_SUPPORTED_FILE_TYPE = "只支持%s类型的文件！";
    public static final String WREADER_LOAD_FAIL = "文件加载出现异常，请重试！";
    public static final String WREADER_INIT_ERROR = "W-Reader加载失败！";
    public static final String WREADER_LOADING = "loading...";
    public static final String WREADER_SEARCH_LOADING = "search loading...";
    public static final String WREADER_BOOK = "BOOK";
    public static final String WREADER_DIRECTORY = "directory";
    public static final String WREADER_CONTENT = "content";
    public static final String WREADER_LOAD_LOCAL_TIP = "若目录未加载成功或文字乱码，请尝试修改字符集，然后重新加载文件";
    public static final String HEADER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36";
    public static final String STR_ONE = "1";
    public static final String DEFAULT_FONT_FAMILY = "JetBrains Mono";
    public static final int DEFAULT_FONT_SIZE = 14;
    public static final String DEFAULT_FONT_COLOR_HEX = "#FFFFFF";
    public static final String ELEMENT_CLASS_STR = "class";
    public static final String ELEMENT_ID_STR = "id";
    /**
     * 正则表达式--文本文件内容小说目录匹配
     */
    public static final String TEXT_FILE_DIR_REGEX = "(^\\s*第)(.{1,9})[章节卷集部篇回](\\s{1})(.*)($\\s*)";
    /**
     * 正则表达式--匹配HTML标签和空白符
     */
    public static final String HTML_TAG_REGEX = "<[^>]+>|\\s|\\p{Zs}";
    /**
     * 文件类型--txt
     */
    public static final String FILE_TYPE_TXT = "txt";
    /**
     * 文件类型--epub
     */
    public static final String FILE_TYPE_EPUB = "epub";
    /**
     * 注释文字颜色
     */
    public static final String LINE_COMMENT_COLOR = "#7A7E85";

}
