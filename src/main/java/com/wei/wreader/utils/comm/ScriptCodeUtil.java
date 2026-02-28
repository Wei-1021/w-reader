package com.wei.wreader.utils.comm;

import com.intellij.openapi.ui.Messages;
import com.wei.wreader.utils.comm.script.RhinoJsEngine;
import com.wei.wreader.utils.data.ConstUtil;
import com.wei.wreader.utils.ui.MessageDialogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 脚本代码工具类
 *
 * @author weizhanjie
 */
public class ScriptCodeUtil {

    /**
     * 判断是否为代码配置类型
     *
     * @param codeStr 代码字符串
     * @return
     */
    public static boolean isJavaCodeConfig(String codeStr) {
        // 是否Java代码配置
        boolean isJavaCodeConfig = codeStr.startsWith(ConstUtil.JAVA_CODE_CONFIG_START_LABEL) &&
                codeStr.endsWith(ConstUtil.JAVA_CODE_CONFIG_END_LABEL);
        // 是否js代码配置: <js></js>
        boolean isJsCodeTagConfig = codeStr.startsWith(ConstUtil.JS_CODE_CONFIG_START_LABEL) &&
                codeStr.endsWith(ConstUtil.JS_CODE_CONFIG_END_LABEL);
        // 是否js代码配置: @js:
        boolean isJsCodeMarkConfig = codeStr.startsWith(ConstUtil.JS_CODE_CONFIG_LABEL);
        return isJavaCodeConfig || isJsCodeTagConfig || isJsCodeMarkConfig;
    }

    /**
     * 判断是否为旧版代码配置类型，即{@code <java></java>}中没有{@code <code></code>}标签的配置，
     *
     * @param codeStr 代码字符串
     * @return
     */
    public static boolean isOldJavaCodeConfig(String codeStr) {
        // 是否Java代码配置
        boolean isJavaCodeConfig = codeStr.startsWith(ConstUtil.JAVA_CODE_CONFIG_START_LABEL) &&
                codeStr.endsWith(ConstUtil.JAVA_CODE_CONFIG_END_LABEL);
        // 是否代码配置: <code></code>
        boolean isCodeTagConfig = codeStr.contains(ConstUtil.CODE_CONFIG_CODE_START) &&
                codeStr.contains(ConstUtil.CODE_CONFIG_CODE_END);
        // 当有{@code <java></java>}中却没有{@code <code></code>}标签时，代表旧版配置
        return isJavaCodeConfig && !isCodeTagConfig;
    }

    /**
     * 获取js脚本/java代码的执行结果
     *
     * @param codeStr            代码字符串
     * @param javaParameterTypes java代码参数类型
     * @param javaParams         java代码参数
     * @param jsParams           js代码需要注入全局的参数
     * @return 执行结果
     * @throws Exception
     */
    public static List<String> getScriptCodeExeListResult(String codeStr,
                                                         Class<?>[] javaParameterTypes,
                                                         Object[] javaParams,
                                                         Map<String, Object> jsParams) {
       return getScriptCodeExeListResult(codeStr, javaParameterTypes, javaParams, jsParams, String.class);
    }

    /**
     * 获取js脚本/java代码的执行结果
     *
     * @param codeStr            代码字符串
     * @param javaParameterTypes java代码参数类型
     * @param javaParams         java代码参数
     * @param jsParams           js代码需要注入全局的参数
     * @param clazz              返回结果的List中每个元素的类型
     * @return 执行结果
     * @throws Exception
     */
    public static <T> List<T> getScriptCodeExeListResult(String codeStr,
                                                         Class<?>[] javaParameterTypes,
                                                         Object[] javaParams,
                                                         Map<String, Object> jsParams,
                                                         Class<T> clazz) {
        Object result = getScriptCodeExeResult(codeStr, "execute", javaParameterTypes,
                javaParams, jsParams, null, null);
        List<T> list = new ArrayList<>();
        if (result instanceof List<?> listResult) {
            for (Object obj : listResult) {
                list.add(clazz.cast(obj));
            }

            return list;
        }
        return null;
    }

    /**
     * 获取js脚本/java代码的执行结果
     *
     * @param codeStr            代码字符串
     * @param javaParameterTypes java代码参数类型
     * @param javaParams         java代码参数
     * @param jsParams           js代码需要注入全局的参数
     * @return 执行结果
     * @throws Exception
     */
    public static Object getScriptCodeExeResult(String codeStr,
                                                Class<?>[] javaParameterTypes,
                                                Object[] javaParams,
                                                Map<String, Object> jsParams) {
        return getScriptCodeExeResult(codeStr, "execute", javaParameterTypes,
                javaParams, jsParams, null, null);
    }


    /**
     * 获取js脚本/java代码的执行结果
     *
     * @param codeStr            代码字符串
     * @param javaParameterTypes java代码参数类型
     * @param javaParams         java代码参数
     * @param jsParams           js代码需要注入全局的参数
     * @param errorCall          执行失败回调
     * @return 执行结果
     * @throws Exception
     */
    public static Object getScriptCodeExeResult(String codeStr,
                                                Class<?>[] javaParameterTypes,
                                                Object[] javaParams,
                                                Map<String, Object> jsParams,
                                                Consumer<RhinoJsEngine.JsResult> errorCall) {
        return getScriptCodeExeResult(codeStr, "execute", javaParameterTypes,
                javaParams, jsParams, null, errorCall);
    }

    /**
     * 获取js脚本/java代码的执行结果
     *
     * @param codeStr            代码字符串
     * @param javaParameterTypes java代码参数类型
     * @param javaParams         java代码参数
     * @param jsParams           js代码需要注入全局的参数
     * @param successCall        执行成功回调
     * @param errorCall          执行失败回调
     * @return 执行结果
     * @throws Exception
     */
    public static Object getScriptCodeExeResult(String codeStr,
                                                Class<?>[] javaParameterTypes,
                                                Object[] javaParams,
                                                Map<String, Object> jsParams,
                                                Consumer<RhinoJsEngine.JsResult> successCall,
                                                Consumer<RhinoJsEngine.JsResult> errorCall) {
        return getScriptCodeExeResult(codeStr, "execute", javaParameterTypes,
                javaParams, jsParams, successCall, errorCall);
    }

    /**
     * 获取js脚本/java代码的执行结果
     *
     * @param codeStr            代码字符串
     * @param methodName         java代码执行入口的方法名
     * @param javaParameterTypes java代码参数类型
     * @param javaParams         java代码参数
     * @param jsParams           js代码需要注入全局的参数
     * @param successCall        执行成功回调
     * @param errorCall          执行失败回调
     * @return 执行结果
     * @throws Exception
     */
    public static Object getScriptCodeExeResult(String codeStr,
                                                String methodName,
                                                Class<?>[] javaParameterTypes,
                                                Object[] javaParams,
                                                Map<String, Object> jsParams,
                                                Consumer<RhinoJsEngine.JsResult> successCall,
                                                Consumer<RhinoJsEngine.JsResult> errorCall) {
        Object result = null;
        // **** 判断代码配置类型，并获取请求结果 ****
        // 是否Java代码配置
        boolean isJavaCodeConfig = codeStr.startsWith(ConstUtil.JAVA_CODE_CONFIG_START_LABEL) &&
                codeStr.endsWith(ConstUtil.JAVA_CODE_CONFIG_END_LABEL);
        // 是否js代码配置: <js></js>
        boolean isJsCodeTagConfig = codeStr.startsWith(ConstUtil.JS_CODE_CONFIG_START_LABEL) &&
                codeStr.endsWith(ConstUtil.JS_CODE_CONFIG_END_LABEL);
        // 是否js代码配置: @js:
        boolean isJsCodeMarkConfig = codeStr.startsWith(ConstUtil.JS_CODE_CONFIG_LABEL);
        if (isJavaCodeConfig) {
            try {
                result = DynamicCodeExecutor.executeMethod(codeStr, methodName,
                        javaParameterTypes, javaParams);
            } catch (Exception e) {
                if (errorCall != null) {
                    errorCall.accept(RhinoJsEngine.JsResult.error(e.getMessage()));
                }
            }
        } else if (isJsCodeTagConfig || isJsCodeMarkConfig) {
            String jsCode;
            // 使用<js></js>格式的配置
            if (isJsCodeTagConfig) {
                // 提取被<js></js>包住的js代码
                jsCode = codeStr.substring(
                        codeStr.indexOf(ConstUtil.JS_CODE_CONFIG_START_LABEL) +
                                ConstUtil.JS_CODE_CONFIG_START_LABEL.length(),
                        codeStr.indexOf(ConstUtil.JS_CODE_CONFIG_END_LABEL)
                );
            }
            // 使用@js:格式的配置
            else {
                // 提取@js:后面的js代码
                jsCode = codeStr.substring(ConstUtil.JS_CODE_CONFIG_LABEL.length());
            }

            try {
                // 因Map.of()创建的Map集合无法修改，所以这里需要复制jsParams
                Map<String, Object> jsParamsCopy = new HashMap<>(jsParams);
                // 注入全局参数：UrlUtil对象
                jsParamsCopy.put("urlUtil", new UrlUtil());

                RhinoJsEngine rhinoJsEngine = new RhinoJsEngine();
                RhinoJsEngine.JsResult executeResult = rhinoJsEngine.execute(jsCode, jsParamsCopy);
                if (executeResult.isSuccess()) {
                    result = executeResult.getValue();
                    if (successCall != null) {
                        successCall.accept(executeResult);
                    }
                } else {
                    if (errorCall != null) {
                        errorCall.accept(executeResult);
                    } else {
                        Messages.showErrorDialog(
                                ConstUtil.WREADER_JSCODE_EXECUTE_ERROR_MSG + executeResult.getErrorMessage(),
                                MessageDialogUtil.TITLE_ERROR);
                    }
                }
            } catch (Exception e) {
                if (errorCall != null) {
                    errorCall.accept(RhinoJsEngine.JsResult.error(e.getMessage()));
                } else {
                    Messages.showErrorDialog(
                            ConstUtil.WREADER_JSCODE_EXECUTE_ERROR_MSG + e.getMessage(),
                            MessageDialogUtil.TITLE_ERROR);
                }
            }
        }

        return result;
    }
}
