package com.wei.wreader.utils.comm.script;

import org.mozilla.javascript.*;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JavaScript执行工具类（基于Rhino引擎）
 *
 * @author weizhanjie
 */
public class JsEngineExecutor {

    /**
     * 配置类
     */
    public static class JsConfig {
        /**
         * 优化级别：-1解释执行，0-9编译优化
         */
        private int optimizationLevel = -1;
        /**
         * 执行超时时间(毫秒)
         */
        private long timeoutMs = 5000;
        /**
         * 严格模式
         */
        private boolean strictMode = false;
        /**
         * 锁定作用域
         */
        private boolean sealedScope = false;
        /**
         * 最大栈深度
         */
        private int maxStackDepth = 1000;
        /**
         * 启用Java访问
         */
        private boolean enableJavaAccess = true;
        /**
         * 指令观察阈值
         */
        private int instructionObserverThreshold = 10000;

        public JsConfig optimizationLevel(int level) {
            this.optimizationLevel = Math.max(-1, Math.min(9, level));
            return this;
        }

        public JsConfig timeout(long timeout, TimeUnit unit) {
            this.timeoutMs = unit.toMillis(timeout);
            return this;
        }

        public JsConfig strictMode(boolean strict) {
            this.strictMode = strict;
            return this;
        }

        public JsConfig sealedScope(boolean sealed) {
            this.sealedScope = sealed;
            return this;
        }

        public JsConfig maxStackDepth(int depth) {
            this.maxStackDepth = depth;
            return this;
        }

        public JsConfig enableJavaAccess(boolean enable) {
            this.enableJavaAccess = enable;
            return this;
        }

        public JsConfig instructionObserverThreshold(int threshold) {
            this.instructionObserverThreshold = threshold;
            return this;
        }
    }

    /**
     * 超时异常类
     */
    public static class JsTimeoutException extends RuntimeException {
        public JsTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * 自定义异常类
     */
    public static class JsExecutionException extends RuntimeException {
        private final String jsStackTrace;

        public JsExecutionException(String message) {
            super(message);
            this.jsStackTrace = null;
        }

        public JsExecutionException(String message, Throwable cause) {
            super(message, cause);
            this.jsStackTrace = extractJsStackTrace(cause);
        }

        private String extractJsStackTrace(Throwable cause) {
            if (cause instanceof JavaScriptException) {
                JavaScriptException jsEx = (JavaScriptException) cause;
                return jsEx.getScriptStackTrace();
            }
            if (cause instanceof WrappedException) {
                return cause.getMessage();
            }
            return null;
        }

        public String getJsStackTrace() {
            return jsStackTrace;
        }
    }

    /**
     * 执行上下文包装器
     */
    private static class ExecutionContext {
        final long startTime = System.currentTimeMillis();
        final Thread executingThread = Thread.currentThread();
        volatile boolean cancelled = false;

        boolean isTimeout(long timeoutMs) {
            return System.currentTimeMillis() - startTime > timeoutMs;
        }

        void cancel() {
            this.cancelled = true;
            executingThread.interrupt();
        }
    }

    /**
     * 支持超时的ContextFactory
     */
    private class TimeoutContextFactory extends ContextFactory {
        private final ThreadLocal<ExecutionContext> executionContext = new ThreadLocal<>();

        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setOptimizationLevel(config.optimizationLevel);
            cx.setLanguageVersion(Context.VERSION_ES6);
            if (config.optimizationLevel == -1) {
                cx.setMaximumInterpreterStackDepth(config.maxStackDepth);
            }

            // 设置WrapFactory
            cx.setWrapFactory(new WrapFactory() {
                @Override
                public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
                    if (obj instanceof Map) {
                        ScriptableObject jsObj = (ScriptableObject) cx.newObject(scope);
                        Map<?, ?> map = (Map<?, ?>) obj;
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            Object key = entry.getKey();
                            Object value = entry.getValue();
                            if (key != null) {
                                Object jsValue = Context.javaToJS(value, scope);
                                jsObj.put(key.toString(), jsObj, jsValue);
                            }
                        }
                        return jsObj;
                    }
                    if (obj instanceof Collection) {
                        Collection<?> collection = (Collection<?>) obj;
                        Object[] array = collection.toArray();
                        return cx.newArray(scope, array);
                    }
                    return super.wrap(cx, scope, obj, staticType);
                }

                @Override
                public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
                    if (config.enableJavaAccess && obj != null) {
                        return super.wrapNewObject(cx, scope, obj);
                    }
                    return super.wrapNewObject(cx, scope, obj);
                }
            });

            return cx;
        }

        @Override
        protected void observeInstructionCount(Context cx, int instructionCount) {
            ExecutionContext context = executionContext.get();
            if (context != null && context.isTimeout(config.timeoutMs)) {
                context.cancel();
                throw new JsTimeoutException("JavaScript execution timeout after " + config.timeoutMs + "ms");
            }
            super.observeInstructionCount(cx, instructionCount);
        }

        ExecutionContext startExecution() {
            ExecutionContext context = new ExecutionContext();
            executionContext.set(context);
            return context;
        }

        void endExecution() {
            executionContext.remove();
        }

        void cancelCurrentExecution() {
            ExecutionContext context = executionContext.get();
            if (context != null) {
                context.cancel();
            }
        }
    }

    private final TimeoutContextFactory contextFactory;
    private final JsConfig config;
    private static final AtomicInteger ENGINE_COUNTER = new AtomicInteger(0);
    private final int engineId;
    private final ExecutorService timeoutExecutor;

    // 缓存
    private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();
    private final Map<String, CachedFunction> functionCache = new ConcurrentHashMap<>();
    private final ReentrantLock executionLock = new ReentrantLock();

    public JsEngineExecutor() {
        this(new JsConfig());
    }

    public JsEngineExecutor(JsConfig config) {
        this.config = config;
        this.engineId = ENGINE_COUNTER.incrementAndGet();
        this.contextFactory = new TimeoutContextFactory();

        this.timeoutExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "js-executor-" + engineId);
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 执行JavaScript代码
     */
    public Object execute(String script) {
        return execute(script, null, null);
    }

    /**
     * 执行JavaScript代码
     * @param script js代码
     * @param variables 全局变量
     * @return
     */
    public Object execute(String script, Map<String, Object> variables) {
        return execute(script, variables, null);
    }

    /**
     * 执行JavaScript代码
     * @param script js代码
     * @param variables 全局变量
     * @param sourceName 源文件名
     * @return
     */
    public Object execute(String script, Map<String, Object> variables, String sourceName) {
        if (sourceName == null) {
            sourceName = "script_" + engineId + "_" + System.currentTimeMillis();
        }

        executionLock.lock();
        try {
            return doExecute(script, variables, sourceName);
        } finally {
            executionLock.unlock();
        }
    }

    /**
     * 执行JavaScript代码
     * @param script js代码
     * @param variables 全局变量
     * @param sourceName 源文件名
     * @return
     */
    private Object doExecute(String script, Map<String, Object> variables, String sourceName) {
        Context cx = null;
        ExecutionContext execContext = null;

        try {
            execContext = contextFactory.startExecution();
            cx = contextFactory.enterContext();

            Scriptable scope = cx.initStandardObjects();

            // 设置严格模式
            if (config.strictMode) {
                script = "'use strict';\n" + script;
            }

            // 绑定变量
            if (variables != null) {
                bindVariables(cx, scope, variables);
            }

            // 密封作用域
            if (config.sealedScope) {
                try {
                    cx.seal(scope);
                } catch (Exception e) {
                    // 某些版本的Rhino可能不支持sealObject
                    System.err.println("Warning: sealObject not supported: " + e.getMessage());
                }
            }

            // 设置指令观察阈值
            cx.setInstructionObserverThreshold(config.instructionObserverThreshold);

            // 执行脚本
            Object result = cx.evaluateString(scope, script, sourceName, 1, null);

            // 转换为Java对象
            return Context.jsToJava(result, Object.class);

        } catch (RhinoException e) {
            throw new JsExecutionException("JavaScript error: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof JsTimeoutException) {
                throw new JsTimeoutException(e.getMessage());
            }
            throw new JsExecutionException("Execution failed: " + e.getMessage(), e);
        } finally {
            if (cx != null) {
                Context.exit();
            }
            if (execContext != null) {
                contextFactory.endExecution();
            }
        }
    }

    /**
     * 执行JavaScript文件
     * @param filePath 文件路径
     * @param variables 全局变量
     * @return 执行结果
     * @throws Exception 读取文件异常
     */
    public Object executeFile(Path filePath, Map<String, Object> variables) throws Exception {
        String script = Files.readString(filePath, StandardCharsets.UTF_8);
        return execute(script, variables, filePath.getFileName().toString());
    }

    /**
     * 调用JavaScript函数
     * @param script js代码
     * @param functionName 函数名
     * @param args 函数参数
     * @return 函数返回值
     */
    public Object callFunction(String script, String functionName, Object... args) {
        return callFunction(script, null, functionName, args);
    }

    /**
     * 调用JavaScript函数
     * @param script js代码
     * @param variables 全局变量
     * @param functionName 函数名
     * @param args 函数参数
      * @return 函数返回值
     */
    public Object callFunction(String script, Map<String, Object> variables,
                               String functionName, Object... args) {
        executionLock.lock();
        try {
            Context cx = null;
            ExecutionContext execContext = null;

            try {
                execContext = contextFactory.startExecution();
                cx = contextFactory.enterContext();

                Scriptable scope = cx.initStandardObjects();

                // 绑定变量
                if (variables != null) {
                    bindVariables(cx, scope, variables);
                }

                // 执行脚本（定义函数）
                cx.evaluateString(scope, script, "function_def", 1, null);

                // 获取函数
                Object funcObj = scope.get(functionName, scope);
                if (!(funcObj instanceof Function)) {
                    throw new JsExecutionException("Function '" + functionName + "' not found");
                }

                return callFunctionInternal(cx, scope, (Function) funcObj, args);

            } finally {
                if (cx != null) {
                    Context.exit();
                }
                if (execContext != null) {
                    contextFactory.endExecution();
                }
            }
        } finally {
            executionLock.unlock();
        }
    }

    /**
     * 内部调用函数，不进行参数绑定和指令观察阈值设置
     * @param cx
     * @param scope
     * @param function
     * @param args
     * @return
     */
    private Object callFunctionInternal(Context cx, Scriptable scope,
                                        Function function, Object... args) {
        // 准备参数
        Object[] jsArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            jsArgs[i] = Context.javaToJS(args[i], scope);
        }

        // 设置指令观察阈值
        cx.setInstructionObserverThreshold(config.instructionObserverThreshold);

        // 调用函数
        Object result = function.call(cx, scope, scope, jsArgs);

        // 转换为Java对象
        return Context.jsToJava(result, Object.class);
    }

    /**
     * 编译并缓存脚本
     * @param script 要编译的脚本
     * @param sourceName 源文件名
     */
    public String compileScript(String script, String sourceName) {
        if (sourceName == null) {
            sourceName = "compiled_" + engineId + "_" + System.currentTimeMillis();
        }

        String cacheKey = sourceName + "_" + script.hashCode();

        String finalSourceName = sourceName;
        return scriptCache.computeIfAbsent(cacheKey, key -> {
            Context cx = null;
            try {
                cx = contextFactory.enterContext();
                return cx.compileString(script, finalSourceName, 1, null);
            } finally {
                if (cx != null) {
                    Context.exit();
                }
            }
        }).toString();
    }

    /**
     * 执行已编译的脚本
     * @param scriptId 编译后的脚本ID
     * @param variables 全局变量
     */
    public Object executeCompiled(String scriptId, Map<String, Object> variables) {
        Script compiledScript = scriptCache.get(scriptId);
        if (compiledScript == null) {
            throw new IllegalArgumentException("Script not found: " + scriptId);
        }

        executionLock.lock();
        try {
            Context cx = null;
            ExecutionContext execContext = null;

            try {
                execContext = contextFactory.startExecution();
                cx = contextFactory.enterContext();

                Scriptable scope = cx.initStandardObjects();

                // 绑定变量
                if (variables != null) {
                    bindVariables(cx, scope, variables);
                }

                // 设置指令观察阈值
                cx.setInstructionObserverThreshold(config.instructionObserverThreshold);

                // 执行编译后的脚本
                Object result = compiledScript.exec(cx, scope);

                // 转换为Java对象
                return Context.jsToJava(result, Object.class);

            } finally {
                if (cx != null) {
                    Context.exit();
                }
                if (execContext != null) {
                    contextFactory.endExecution();
                }
            }
        } finally {
            executionLock.unlock();
        }
    }

    /**
     * 编译并缓存函数
     * @param script js代码
     * @param functionName 函数名
     * @return 返回函数ID
     */
    public String compileFunction(String script, String functionName) {
        String functionId = "func_" + functionName + "_" + script.hashCode();

        if (!functionCache.containsKey(functionId)) {
            executionLock.lock();
            try {
                if (!functionCache.containsKey(functionId)) {
                    Context cx = null;
                    try {
                        cx = contextFactory.enterContext();
                        Scriptable scope = cx.initStandardObjects();

                        // 编译脚本
                        cx.evaluateString(scope, script, "func_compile", 1, null);

                        // 获取函数
                        Object funcObj = scope.get(functionName, scope);
                        if (!(funcObj instanceof Function)) {
                            throw new JsExecutionException("Function '" + functionName + "' not found");
                        }

                        // 缓存函数
                        functionCache.put(functionId, new CachedFunction(scope, (Function) funcObj));

                    } finally {
                        if (cx != null) {
                            Context.exit();
                        }
                    }
                }
            } finally {
                executionLock.unlock();
            }
        }

        return functionId;
    }

    /**
     * 调用缓存的函数
     */
    public Object callCachedFunction(String functionId, Object... args) {
        CachedFunction cached = functionCache.get(functionId);
        if (cached == null) {
            throw new JsExecutionException("Function not found in cache: " + functionId);
        }

        executionLock.lock();
        try {
            Context cx = null;
            ExecutionContext execContext = null;

            try {
                execContext = contextFactory.startExecution();
                cx = contextFactory.enterContext();

                return callFunctionInternal(cx, cached.scope, cached.function, args);

            } finally {
                if (cx != null) {
                    Context.exit();
                }
                if (execContext != null) {
                    contextFactory.endExecution();
                }
            }
        } finally {
            executionLock.unlock();
        }
    }

    /**
     * 绑定变量
     */
    private void bindVariables(Context cx, Scriptable scope, Map<String, Object> variables) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Object jsValue = Context.javaToJS(entry.getValue(), scope);
            ScriptableObject.putProperty(scope, entry.getKey(), jsValue);
        }
    }

    /**
     * 安全执行
     */
    public Object executeSafely(String script, Object defaultValue) {
        try {
            return execute(script);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 异步执行
     */
    public CompletableFuture<Object> executeAsync(String script, Map<String, Object> variables) {
        return CompletableFuture.supplyAsync(() -> execute(script, variables), timeoutExecutor);
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        scriptCache.clear();
        functionCache.clear();
    }

    /**
     * 获取缓存状态
     */
    public Map<String, Integer> getCacheStats() {
        return Map.of(
                "scripts", scriptCache.size(),
                "functions", functionCache.size()
        );
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        clearCache();
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // 内部类
    private static class CachedFunction {
        final Scriptable scope;
        final Function function;

        CachedFunction(Scriptable scope, Function function) {
            this.scope = scope;
            this.function = function;
        }
    }
}