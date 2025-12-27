package com.rubberhuman.client.api;

import com.rubberhuman.client.ClientApp;
import com.rubberhuman.client.interceptor.AuthInterceptor;
import com.rubberhuman.client.util.AlertUtil;
import com.rubberhuman.client.util.SessionManager;
import javafx.application.Platform;
import kong.unirest.HttpRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.IOException;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/api";

    static {
        // 配置 Jackson JSON解析器
        Unirest.config().setObjectMapper(new kong.unirest.jackson.JacksonObjectMapper());

//        // 全局拦截器：处理 Token 过期 (401)
//        Unirest.config().interceptor(new AuthInterceptor());
    }

    // 普通 POST 请求 (发送 JSON 数据)
    public static HttpRequestWithBody post(String endpoint) {
        HttpRequestWithBody req = Unirest.post(BASE_URL + endpoint);
        // 普通接口默认是 JSON
        req.header("Content-Type", "application/json");
        addAuth(req);
        return req;
    }

    // 文件上传 POST 请求 (千万不能设 Content-Type 为 json)
    public static HttpRequestWithBody upload(String endpoint) {
        HttpRequestWithBody req = Unirest.post(BASE_URL + endpoint);
        // 不设置 Content-Type，Unirest 会在放入 .field("file", file) 时自动设置为 multipart/form-data
        addAuth(req);
        return req;
    }

    // GET 请求
    public static HttpRequest<?> get(String endpoint) {
        return addAuth(Unirest.get(BASE_URL + endpoint));
    }

    // 通用鉴权添加
    private static HttpRequest<?> addAuth(HttpRequest<?> request) {
        if (SessionManager.isLoggedIn()) {
            request.header("Authorization", "Bearer " + SessionManager.getToken());
        }
        return request;
    }

    public static <T> boolean checkAuth(HttpResponse<T> response) {
        if (response.getStatus() == 401 || response.getStatus() == 403) {
            // 必须在 JavaFX 线程中更新 UI
            Platform.runLater(() -> {
                SessionManager.clearSession();
                try {
                    // 强制跳转回登录页
                    ClientApp.setRoot("view/login");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return false; // 验证失败
        }
        return true; // 验证通过
    }
}