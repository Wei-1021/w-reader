package com.wei.wreader.utils.jcef;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.*;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * JCEF 工具类，提供 Java 与 JavaScript 双向通信功能
 */
public class JCEFHelper {
    private static final Logger LOG = Logger.getInstance(JCEFHelper.class);

    private final Project project;
    private final JBCefBrowser browser;
    private final Map<String, JBCefJSQuery> jsQueries = new HashMap<>();
    private final Map<String, Function<Object, Object>> javaHandlers = new HashMap<>();

    private final List<String> jsCodeList = new ArrayList<>();

    private boolean isBridgeInitialized = false;

    public JCEFHelper(Project project) {
        this.project = project;

        // 检查 JCEF 是否支持
        if (!JBCefApp.isSupported()) {
            throw new RuntimeException("JCEF is not supported in this environment");
        }

        // 创建浏览器实例
        this.browser = new JBCefBrowser();

        // 添加加载处理器
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame.isMain()) {
                    // 页面加载完成后初始化桥接
                    initializeJSBridge();
                }
            }
        }, browser.getCefBrowser());
    }

    /**
     * 获取浏览器组件
     */
    public JBCefBrowser getBrowser() {
        return browser;
    }

    /**
     * 加载 URL
     */
    public void loadURL(String url) {
        browser.loadURL(url);
    }

    /**
     * 加载 HTML 内容
     */
    public void loadHTML(String html) {
        browser.loadHTML(html);
    }

    /**
     * 初始化 JavaScript 桥接
     */
    private void initializeJSBridge() {
        if (isBridgeInitialized) {
            return;
        }

        try {
            // 创建全局桥接对象
            String bridgeCode = "window.javaBridge = {";

            // 为每个注册的处理器创建函数
            for (String methodName : javaHandlers.keySet()) {
                JBCefJSQuery jsQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);
                jsQueries.put(methodName, jsQuery);

                // 添加处理器
                jsQuery.addHandler(params -> {
                    try {
                        Function<Object, Object> handler = javaHandlers.get(methodName);
                        if (handler != null) {
                            Object result = handler.apply(params);
                            return new JBCefJSQuery.Response((String) result);
                        } else {
                            return new JBCefJSQuery.Response(null, 404, "Method not found");
                        }
                    } catch (Exception e) {
                        LOG.error("Error executing Java method from JS", e);
                        return new JBCefJSQuery.Response(null, 500, "Internal error: " + e.getMessage());
                    }
                });

                // 添加方法到桥接对象
                bridgeCode += methodName + ": function(params) { " +
                        "return " + jsQuery.inject("JSON.stringify(params)") + ";" +
                        " },";
            }

            // 完成桥接对象定义
            bridgeCode = bridgeCode.substring(0, bridgeCode.length() - 1) + "};";

            // 添加自定义 JS 代码
            for (String jsCode : jsCodeList) {
                bridgeCode += jsCode;
            }

            // 注入桥接代码
            executeJavaScript(bridgeCode, null);

            isBridgeInitialized = true;
            LOG.info("JavaScript bridge initialized successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize JavaScript bridge", e);
        }
    }

    /**
     * 注册 Java 方法供 JavaScript 调用
     */
    public void registerJavaMethod(String methodName, Function<Object, Object> handler) {
        javaHandlers.put(methodName, handler);

        // 如果桥接已初始化，需要重新初始化
        if (isBridgeInitialized) {
            isBridgeInitialized = false;
            initializeJSBridge();
        }
    }

    /**
     * 注册 Java 方法供 JavaScript 调用
     */
    public void registerJsCode(String jsCode) {
        jsCodeList.add(jsCode);
        // 如果桥接已初始化，需要重新初始化
        if (isBridgeInitialized) {
            isBridgeInitialized = false;
            initializeJSBridge();
        }
    }

    /**
     * 执行 JavaScript 代码（无返回值）
     * 适用于不需要获取执行结果的场景，如触发某个操作、修改样式等。
     *
     * @param jsCode 要执行的 JavaScript 代码字符串
     */
    public void executeJavaScript(String jsCode) {
        if (browser.getCefBrowser() == null) {
            LOG.warn("Browser is not ready, cannot execute JavaScript.");
            return;
        }


        // 注意：此版本的 executeJavaScript 方法只有三个参数
        browser.getCefBrowser().executeJavaScript(jsCode, browser.getCefBrowser().getURL(), 0);
    }

    /**
     * 执行 JavaScript 代码
     * 适用于不需要获取执行结果的场景，如触发某个操作、修改样式等。
     *
     * @param jsCode 要执行的 JavaScript 代码字符串
     */
    public void executeJavaScript(String jsCode, @Nullable Consumer<Object> callback) {
        if (browser.getCefBrowser() == null) {
            LOG.warn("Browser is not ready, cannot execute JavaScript.");
            return;
        }
        // 注意：此版本的 executeJavaScript 方法只有三个参数
        browser.getCefBrowser().executeJavaScript(jsCode, browser.getCefBrowser().getURL(), 0);
    }

    /**
     * 执行 JavaScript 代码并返回 Future
     */
    public CompletableFuture<Object> executeJavaScriptAsync(String jsCode) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        executeJavaScript(jsCode, result -> {
            if (result instanceof Map && ((Map<?, ?>) result).containsKey("error")) {
                future.completeExceptionally(new RuntimeException((String) ((Map<?, ?>) result).get("error")));
            } else {
                future.complete(result);
            }
        });

        return future;
    }

    /**
     * 调用 JavaScript 函数
     */
    public void callJSFunction(String functionName, Object[] args, @Nullable Consumer<Object> callback) {
        StringBuilder jsCode = new StringBuilder(functionName + "(");

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof String) {
                    jsCode.append("'").append(args[i].toString().replace("'", "\\'")).append("'");
                } else {
                    jsCode.append(args[i]);
                }

                if (i < args.length - 1) {
                    jsCode.append(",");
                }
            }
        }

        jsCode.append(")");

        executeJavaScript(jsCode.toString(), callback);
    }

    /**
     * 清理资源
     */
    public void dispose() {
        // 清理所有 JS 查询
        for (JBCefJSQuery query : jsQueries.values()) {
            Disposer.dispose(query);
        }
        jsQueries.clear();
        javaHandlers.clear();

        // 清理浏览器
        Disposer.dispose(browser);
    }

    /**
     * 示例：注册一些常用的 Java 方法
     */
    public void registerDefaultMethods() {
        // 显示消息
        registerJavaMethod("showMessage", params -> {
            if (params instanceof Map<?, ?> paramMap) {
                String message = (String) paramMap.get("message");
                String title = (String) paramMap.get("title");

                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage(project, message, title != null ? title : "信息");
                });
            }
            return null;
        });

        // 获取项目信息
        registerJavaMethod("getProjectInfo", params -> {
            Map<String, Object> info = new HashMap<>();
            info.put("name", project.getName());
            info.put("basePath", project.getBasePath());
            info.put("isOpen", project.isOpen());
            return info;
        });

        // 执行简单的计算
        registerJavaMethod("calculate", params -> {
            if (params instanceof Map<?, ?> paramMap) {
                String operation = (String) paramMap.get("operation");
                double a = ((Number) paramMap.get("a")).doubleValue();
                double b = ((Number) paramMap.get("b")).doubleValue();

                return switch (operation) {
                    case "add" -> a + b;
                    case "subtract" -> a - b;
                    case "multiply" -> a * b;
                    case "divide" -> b != 0 ? a / b : Double.NaN;
                    default -> throw new IllegalArgumentException("Unknown operation: " + operation);
                };
            }
            return null;
        });
    }
}