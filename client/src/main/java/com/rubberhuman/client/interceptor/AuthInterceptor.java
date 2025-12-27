package com.rubberhuman.client.interceptor;

import com.rubberhuman.client.ClientApp;
import com.rubberhuman.client.util.AlertUtil;
import com.rubberhuman.client.util.SessionManager;
import javafx.application.Platform;
import kong.unirest.*;

import java.io.IOException;

public class AuthInterceptor implements Interceptor {

    @Override
    public void onRequest(HttpRequest<?> request, Config config) {
        // 这里一般不处理 401
    }

    @Override
    public void onResponse(HttpResponse<?> response,
                           HttpRequestSummary request,
                           Config config) {

        if (response.getStatus() == 401) {
            System.out.println("检测到 401 未授权，准备跳转登录页...");

            SessionManager.clearSession();

            Platform.runLater(() -> {
                try {
                    AlertUtil.showError("认证过期", "您的登录状态已失效，请重新登录。");
                    ClientApp.setRoot("view/login");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
