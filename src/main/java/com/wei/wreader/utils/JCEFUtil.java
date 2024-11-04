package com.wei.wreader.utils;

import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class JCEFUtil {

    public static JBCefBrowser createBrowser(Project project) {
        JBCefClient client = JBCefApp.getInstance().createClient();
        // CefMessageRouter 用于处理来自 Chromium 浏览器的消息和事件，
        // 前端代码可以通过innerCefQuery和innerCefQueryCancel发起消息给插件进行处理
        CefMessageRouter.CefMessageRouterConfig routerConfig =
                new CefMessageRouter.CefMessageRouterConfig("innerCefQuery", "innerCefQueryCancel");
        CefMessageRouter messageRouter = CefMessageRouter.create(routerConfig, new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                return super.onQuery(browser, frame, queryId, request, persistent, callback);
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
                super.onQueryCanceled(browser, frame, queryId);
            }
        });
        client.getCefClient().addMessageRouter(messageRouter);
        // 用于处理以http://inner/开头的请求。 用于拦截特定请求，转发请求到本地以获取本地资源
        CefApp.getInstance()
                .registerSchemeHandlerFactory("http", "inner", new CefSchemeHandlerFactory() {
                    @Override
                    public CefResourceHandler create(CefBrowser cefBrowser, CefFrame cefFrame, String s, CefRequest cefRequest) {
                        return null;
                    }
                });
        return new JBCefBrowser(client, "");
    }

}
