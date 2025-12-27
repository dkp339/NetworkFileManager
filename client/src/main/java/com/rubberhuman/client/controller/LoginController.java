package com.rubberhuman.client.controller;

import com.rubberhuman.client.ClientApp;
import com.rubberhuman.client.api.AuthService;
import com.rubberhuman.client.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    protected void onLoginButtonClick() {
        String u = usernameField.getText();
        String p = passwordField.getText();

        if (u.isEmpty() || p.isEmpty()) {
            AlertUtil.showError("错误", "用户名和密码不能为空");
            return;
        }

        if (authService.login(u, p)) {
            try {
                // 登录成功，跳转主界面
                ClientApp.setRoot("view/main");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            AlertUtil.showError("登录失败", "用户名或密码错误");
        }
    }

    @FXML
    protected void onRegisterLinkClick() throws IOException {
        ClientApp.setRoot("view/register");
    }
}