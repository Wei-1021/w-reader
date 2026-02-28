package com.wei.wreader.utils.comm.script;

import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.utils.comm.UrlUtil;
import org.jsoup.nodes.Element;
import org.mozilla.javascript.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 功能完善的 Rhino JavaScript 执行引擎
 * <p>
 * 特性：
 * - 线程安全（每个执行使用独立 Context）
 * - 支持注入全局变量（Java 对象 → JS）
 * - 支持调用 JS 函数并传参
 * - 支持执行超时控制（防止死循环）
 * - 支持沙箱模式（禁用危险操作）
 * - 自动类型转换（JS → Java 原生类型）
 * - 详细的错误信息（包含行号、堆栈）
 *
 * @author weizhanjie
 */
public class RhinoJsEngine {

    // 默认超时：5 秒
    private static final long DEFAULT_TIMEOUT_MS = 5000;

    // 是否启用沙箱（禁用 eval, load, quit 等）
    private final boolean sandboxMode;
    private final long defaultTimeoutMs;

    public RhinoJsEngine() {
        this(false, DEFAULT_TIMEOUT_MS);
    }

    public RhinoJsEngine(boolean sandboxMode) {
        this(sandboxMode, DEFAULT_TIMEOUT_MS);
    }

    public RhinoJsEngine(boolean sandboxMode, long defaultTimeoutMs) {
        this.sandboxMode = sandboxMode;
        this.defaultTimeoutMs = Math.max(100, defaultTimeoutMs); // 最小 100ms
    }

    // ------------------ 执行脚本 ------------------

    /**
     * 执行 JavaScript 脚本
     */
    public JsResult execute(String jsCode) {
        return execute(jsCode, null, defaultTimeoutMs);
    }

    /**
     * 执行 JavaScript 脚本
     *
     * @param jsCode   js代码
     * @param bindings 绑定变量（通过 bindings 注入全局变量）
     * @return
     */
    public JsResult execute(String jsCode, Map<String, Object> bindings) {
        return execute(jsCode, bindings, defaultTimeoutMs);
    }

    /**
     * 执行 JavaScript 脚本
     *
     * @param jsCode    js代码
     * @param timeoutMs 超时时间
     * @return
     */
    public JsResult execute(String jsCode, long timeoutMs) {
        return execute(jsCode, null, timeoutMs);
    }

    /**
     * 执行 JavaScript 脚本（带绑定和超时）
     *
     * @param jsCode    js代码
     * @param bindings  绑定变量（通过 bindings 注入全局变量）
     * @param timeoutMs 超时时间
     * @return
     */
    public JsResult execute(String jsCode, Map<String, Object> bindings, long timeoutMs) {
        return executeInternal(() -> doExecute(jsCode, bindings), timeoutMs);
    }

    // ------------------ 调用函数 ------------------

    /**
     * 调用 JavaScript 函数
     */
    public JsResult callFunction(String jsCode, String funcName, Object... args) {
        return callFunction(jsCode, funcName, null, args);
    }

    /**
     * 调用 JavaScript 函数
     *
     * @param jsCode   js代码
     * @param funcName 函数名
     * @param bindings 绑定变量（通过 bindings 注入全局变量）
     * @param args     传给函数的实际参数
     * @return
     */
    public JsResult callFunction(String jsCode, String funcName, Map<String, Object> bindings, Object... args) {
        return callFunctionWithTimeout(jsCode, funcName, bindings, defaultTimeoutMs, args);
    }

    /**
     * 调用 JavaScript 函数（带超时）
     *
     * @param jsCode    js代码
     * @param funcName  函数名
     * @param timeoutMs 超时时间
     * @param args      传给函数的实际参数
     * @return
     */
    public JsResult callFunctionWithTimeout(String jsCode, String funcName, long timeoutMs, Object... args) {
        return callFunctionWithTimeout(jsCode, funcName, null, timeoutMs, args);
    }

    /**
     * 调用 JavaScript 函数（带绑定和超时时间）
     *
     * @param jsCode    js代码
     * @param funcName  函数名
     * @param bindings  绑定变量（通过 bindings 注入全局变量）
     * @param timeoutMs 超时时间
     * @param args      传给函数的实际参数
     * @return
     */
    public JsResult callFunctionWithTimeout(String jsCode, String funcName, Map<String, Object> bindings,
                                            long timeoutMs, Object... args) {
        return executeInternal(() -> doCallFunction(jsCode, funcName, bindings, args), timeoutMs);
    }

    // ------------------ 内部执行逻辑 ------------------

    /**
     * 执行 JavaScript 脚本
     *
     * @param jsCode
     * @param bindings
     * @return
     */
    private JsResult doExecute(String jsCode, Map<String, Object> bindings) {
        Context cx = enterContext();
        try {
            Scriptable scope = createScope(cx);
            setupBindings(cx, scope, bindings);
            Object result = cx.evaluateString(scope, jsCode, "<eval>", 1, null);
            Object converted = convertResult(result);
            return JsResult.success(converted);
        } catch (RhinoException e) {
            return JsResult.error(formatRhinoError(e));
        } catch (Exception e) {
            return JsResult.error("Execution failed: " + e.getMessage());
        } finally {
            Context.exit();
        }
    }

    /**
     * 执行 JavaScript 函数
     *
     * @param jsCode
     * @param funcName
     * @param bindings
     * @param args
     * @return
     */
    private JsResult doCallFunction(String jsCode, String funcName, Map<String, Object> bindings, Object[] args) {
        Context cx = enterContext();
        try {
            Scriptable scope = createScope(cx);
            setupBindings(cx, scope, bindings);

            // 执行函数定义
            cx.evaluateString(scope, jsCode, "<funcDef>", 1, null);

            // 获取函数
            Object funcObj = scope.get(funcName, scope);
            if (!(funcObj instanceof Function)) {
                return JsResult.error("Function '" + funcName + "' not found or is not a function.");
            }

            Function func = (Function) funcObj;
            Object[] jsArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                jsArgs[i] = Context.javaToJS(args[i], scope);
            }

            Object result = func.call(cx, scope, scope, jsArgs);
            Object converted = convertResult(result);
            return JsResult.success(converted);
        } catch (RhinoException e) {
            return JsResult.error(formatRhinoError(e));
        } catch (Exception e) {
            return JsResult.error("Function call failed: " + e.getMessage());
        } finally {
            Context.exit();
        }
    }

    private Context enterContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6); // 启用 ES6 支持
        cx.setOptimizationLevel(9); // 最高优化（-1=解释，0~9=编译）
        if (sandboxMode) {
            cx.setClassShutter(new DenyAllClassShutter()); // 沙箱：禁止访问 Java 类
        }
        return cx;
    }

    private Scriptable createScope(Context cx) {
        ScriptableObject scope = (ScriptableObject) cx.initStandardObjects();
        if (sandboxMode) {
            // 移除危险函数
            scope.delete("eval");
            scope.delete("load");
            scope.delete("loadClass");
            scope.delete("quit");
            scope.delete("print"); // 可选
        }
        return scope;
    }

    private void setupBindings(Context cx, Scriptable scope, Map<String, Object> bindings) {
        if (bindings != null) {
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    Object wrapped = Context.javaToJS(value, scope);
                    scope.put(key, scope, wrapped);
                }
            }
        }
    }

    // ------------------ 类型转换 ------------------

    private Object convertResult(Object obj) {
        if (obj == null || obj instanceof Undefined) return null;
        if (obj instanceof Boolean || obj instanceof Number || obj instanceof String) {
            return obj;
        }
        if (obj instanceof NativeArray) {
            NativeArray arr = (NativeArray) obj;
            int len = (int) arr.getLength();
            Object[] result = new Object[len];
            for (int i = 0; i < len; i++) {
                result[i] = convertResult(arr.get(i, arr));
            }
            return result;
        }
        if (obj instanceof NativeObject) {
            // 转为 Map<String, Object>
            NativeObject no = (NativeObject) obj;
            Map<String, Object> map = new HashMap<>();
            for (Object id : no.getIds()) {
                if (id instanceof String) {
                    map.put((String) id, convertResult(no.get((String) id, no)));
                }
            }
            return map;
        }
        // 处理 NativeJavaObject 包装的 Java 对象
        if (obj instanceof Wrapper) {
            Object wrapped = ((Wrapper) obj).unwrap();
            if (wrapped instanceof String) {
                return wrapped;
            }
            // 其他类型的包装对象也转为字符串
            return wrapped.toString();
        }
        // 其他对象转为字符串（或保留原样）
        return obj.toString();
    }

    // ------------------ 超时控制 ------------------

    private JsResult executeInternal(Supplier<JsResult> task, long timeoutMs) {
        if (timeoutMs <= 0) {
            return task.get();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<JsResult> future = executor.submit(task::get);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return JsResult.error("Execution timed out after " + timeoutMs + " ms");
        } catch (Exception e) {
            return JsResult.error("Execution interrupted: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    // ------------------ 错误格式化 ------------------

    private String formatRhinoError(RhinoException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("JavaScript error:\n");
        sb.append("Message: ").append(e.getMessage()).append("\n");
        if (e instanceof EvaluatorException) {
            EvaluatorException ee = (EvaluatorException) e;
            sb.append("Source: ").append(ee.sourceName()).append("\n");
            sb.append("Line: ").append(ee.lineNumber()).append("\n");
            sb.append("Column: ").append(ee.columnNumber()).append("\n");
        }
        if (e.getScriptStackTrace() != null) {
            sb.append("Stack:\n").append(e.getScriptStackTrace());
        }
        return sb.toString();
    }

    // ------------------ 沙箱：禁止所有 Java 类访问 ------------------
    private static class DenyAllClassShutter implements ClassShutter {
        @Override
        public boolean visibleToScripts(String fullClassName) {
            return false; // 拒绝所有 Java 类
        }
    }

    // ------------------ 结果封装 ------------------

    /**
     * 结果封装
     */
    public static class JsResult {
        private final boolean success;
        private final Object value;
        private final String errorMessage;

        private JsResult(boolean success, Object value, String errorMessage) {
            this.success = success;
            this.value = value;
            this.errorMessage = errorMessage;
        }

        public static JsResult success(Object value) {
            return new JsResult(true, value, null);
        }

        public static JsResult error(String message) {
            return new JsResult(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public Object getValue() {
            return value;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return "JsResult{value=" + value + "}";
            } else {
                return "JsResult{error='" + errorMessage + "'}";
            }
        }
    }

    public static void test() {
        // 创建引擎（开启沙箱 + 3秒超时）
        RhinoJsEngine engine = new RhinoJsEngine();

        // 1. 基本执行
        var r1 = engine.execute("1 + 2 * 3");
        System.out.println(r1.getValue()); // 7

        // 2. 注入变量
        Map<String, Object> bindings = Map.of("name", "Alice", "score", 95);
        var r2 = engine.execute("`${name} got ${score} points!`", bindings);
        System.out.println(r2.getValue()); // Alice got 95 points!

        // 3. 调用函数
        String js = """
                function fibonacci(n) {
                    if (n <= 1) return n;
                    return fibonacci(n - 1) + fibonacci(n - 2);
                }
                """;
        var r3 = engine.callFunction(js, "fibonacci", 10);
        System.out.println(r3.getValue()); // 55

        // 3-1. 调用函数
        String js2 = """
                function fibonacci2(n) {
                    if (n <= 1) return n;
                    return fibonacci2(n - 1) + fibonacci2(n - 2) + (n * score);
                }
                """;
        Map<String, Object> bindings2 = Map.of("score", 95);
        var r31 = engine.callFunction(js2, "fibonacci2", bindings2, 10);
        System.out.println(r31.getValue()); // 55

        // 4. 超时测试（死循环）
//        var r4 = engine.execute("while(true) {}");
//        System.out.println(r4.getErrorMessage()); // Execution timed out...

        // 5. 错误处理
        var r5 = engine.execute("console.log('hello')"); // console 未定义
        System.out.println(r5.getErrorMessage()); // ReferenceError: "console" is not defined

        // 6. 返回对象（自动转 Map）
        var r6 = engine.execute("({x: 10, y: [1,2,3]})");
        if (r6.isSuccess()) {
            Map<String, Object> obj = (Map<String, Object>) r6.getValue();
            System.out.println(obj.get("x")); // 10
            Object[] arr = (Object[]) obj.get("y");
            System.out.println(java.util.Arrays.toString(arr)); // [1, 2, 3]
        }

        // 7.访问 Java 类
        String jsCode = """
            // 1. 调用静态方法：System.currentTimeMillis()
            var now = java.lang.System.currentTimeMillis();
            
            // 2. 创建 Java 对象：ArrayList
            var list = new java.util.ArrayList();
            list.add("Hello");
            list.add("from");
            list.add("Java!");
            var size = list.size();
            
            // 3. 调用 Java 字符串方法
            var text = "  Trim me  ";
            var trimmed = text.trim(); // 注意：这里 text 是 JS 字符串，自动转为 Java String
            
            // 4. 使用 Java 数学库
            var pi = java.lang.Math.PI;
            var random = java.lang.Math.random();
            
            // 返回结果对象
            ({
                currentTimeMillis: now,
                listSize: size,
                trimmedText: trimmed,
                pi: pi,
                random: random
            })
            """;

        var result = engine.execute(jsCode);
        if (result.isSuccess()) {
            System.out.println("✅ JS 调用 Java 成功:");
            System.out.println(result.getValue());
        } else {
            System.err.println("❌ 错误: " + result.getErrorMessage());
        }

        // 8 test
        BookInfo bookInfo = new BookInfo();
        bookInfo.setBookId("111");
        bookInfo.setBookName("bookname001");
        bookInfo.setBookUrl("https://www.8xsk.info/bookinfo/bookname001/111");
        bookInfo.setBookAuthor("");
        bookInfo.setBookDesc("");
        bookInfo.setBookImgUrl("");

        Element element = new Element("div");
        element.append("""
                <div id="pagination">
                    <ul class="pagination pagination-sm pagination-chap">
                        <li><a href="/novel2541/">First</a></li>
                        <li><a href="/novel2541/?p=26">Prev</a></li>
                        &nbsp;
                        <li><a href="/novel2541/?p=25">25</a></li>
                        &nbsp;
                        <li><a href="/novel2541/?p=26">26</a></li>
                        &nbsp;
                        <li class="active"><a>27</a></li>
                        <li class="active"><a href="/novel2541/?p=29">Next</a></li>
                        <li class="active"><a>Last</a></li>
                    </ul>
                </div>
                """);
        Element nextLiHtmlElement = element.selectFirst(".pagination li:nth-last-child(2)");

        String jsCode8 = """   
                let tt = new Date().getTime();
                let key = bookInfo.getBookName();
                let bookId = bookInfo.getBookId();
                let baseUrl = "https://www.8xsk.info";
                let url = `/e/search/index.php?keyboard=${key}&show=&searchget=1&tt=${tt}&bookId=${bookId}`;
                
                let newUrl = urlUtil.getFullURL(baseUrl, url);
                
                let nextLiHtmlElement = element.selectFirst(".pagination li:nth-last-of-type(2)");
                let nextLiHtml = nextLiHtmlElement != null ? nextLiHtmlElement.html() : "";
                let href = nextLiHtmlElement.selectFirst("a").attr("href");
                ({
                    url1: url,
                    newUrl1: newUrl.toString(),
                    nextLiHtml1: nextLiHtml,
                    href1: href,
                })
                """;

        var result8 = engine.execute(jsCode8, new HashMap<>() {{
            put("bookInfo", bookInfo);
            put("urlUtil", new UrlUtil());
            put("element", element);
        }});
        System.out.println(result8.getValue() + ", " + result8.getErrorMessage());
    }
}