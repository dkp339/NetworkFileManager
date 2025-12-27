package com.rubberhuman.client.api;

import com.rubberhuman.client.model.LoginRequest;
import com.rubberhuman.client.model.LoginResponse;
import com.rubberhuman.client.model.RegisterRequest;
import com.rubberhuman.client.util.SessionManager;
import kong.unirest.HttpResponse;

public class AuthService {

    public boolean login(String username, String password) {
        LoginRequest req = new LoginRequest(username, password);
        HttpResponse<LoginResponse> res = ApiClient.post("/auth/login")
                .body(req)
                .asObject(LoginResponse.class);

        if (res.isSuccess()) {
            LoginResponse body = res.getBody();
            SessionManager.saveSession(body.getToken(), body.getUsername(), body.getRole());
            return true;
        }
        return false;
    }

    public boolean register(String username, String password, String email) {
        RegisterRequest req = new RegisterRequest(username, password, email);
        HttpResponse<String> res = ApiClient.post("/auth/register")
                .body(req)
                .asString();
        return res.isSuccess();
    }
}