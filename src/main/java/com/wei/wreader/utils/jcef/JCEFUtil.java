package com.wei.wreader.utils.jcef;

import com.intellij.ui.jcef.*;
import com.intellij.util.ui.UIUtil;
import com.wei.wreader.utils.comm.ThemeUtils;
import org.cef.browser.CefBrowser;

import java.util.function.Function;

/**
 * JCEF工具类
 * @author weizhanjie
 */
public class JCEFUtil {
    /**
     * 统一外观模式--IDE 主题/滚动条/字体注入
     */
    public static void injectTheme(JBCefBrowser jbCefBrowser) {
        CefBrowser browser = jbCefBrowser.getCefBrowser();
        // 把 IDE 主题变量注入页面
        int labelFontSize = UIUtil.getLabelFont().getSize();
        String script = "window.IDE_THEME='" + (ThemeUtils.isDarkTheme() ? "dark" : "light") + "';" +
                "document.documentElement.style.setProperty('--jb-font-size','" + labelFontSize + "px');";
        browser.executeJavaScript(script, browser.getURL(), 0);

        // 滚动条样式一键同步
        browser.executeJavaScript(
                "const s=document.createElement('style');" +
                        "s.innerHTML='* {color: #FFF;} ';" +
                        "document.head.appendChild(s);",
                browser.getURL(), 0);

        String scrollbarsStyle = JBCefScrollbarsHelper.buildScrollbarsStyle();
        browser.executeJavaScript(
                "const s=document.createElement('style');" +
                        "s.innerHTML='" + scrollbarsStyle + "';" +
                        "document.head.appendChild(s);",
                browser.getURL(), 0);
    }

    /**
     * JavaScript调用Java
     * @param jbCefBrowser
     * @param function
     */
    public static void addJSQuery(JBCefBrowser jbCefBrowser,
                                  Function<? super String, ? extends JBCefJSQuery.Response> function,
                                  String callback) {
//        JBCefJSQuery jsQuery = JBCefJSQuery.create((JBCefBrowserBase) jbCefBrowser);
//        jsQuery.addHandler(function);
//
//        jbCefBrowser.getCefBrowser().executeJavaScript(
//                "window.jsCallJavaFunction = function(res) {" +
//                    jsQuery.inject("res") +
//                    "};",
//                jbCefBrowser.getCefBrowser().getURL(), 0
//        );
//
//        jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
//            @Override
//            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
//                browser.executeJavaScript(
//                    callback, jbCefBrowser.getCefBrowser().getURL(), 0
//                );
//            }
//        }, jbCefBrowser.getCefBrowser());


        JBCefJSQuery openLinkQuery = JBCefJSQuery.create((JBCefBrowserBase) jbCefBrowser); // 1
        openLinkQuery.addHandler((link) -> { // 2
            System.out.println("123321");
            return null; // 3
        });

        jbCefBrowser.getCefBrowser().executeJavaScript( // 4
                "window.openLink = function(link) {" +
                        openLinkQuery.inject("link") + // 5
                        "};",
                jbCefBrowser.getCefBrowser().getURL(), 0
        );

        jbCefBrowser.getCefBrowser().executeJavaScript( // 6
                """
                document.addEventListener('click', function (e) {
                  const link = e.target.closest('a').href;
                  if (link) {
                    window.openLink(link);
                  }
                });""",
                jbCefBrowser.getCefBrowser().getURL(), 0
        );
    }
}
