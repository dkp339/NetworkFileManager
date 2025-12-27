package com.rubberhuman.client.controller;

import com.rubberhuman.client.ClientApp;
import com.rubberhuman.client.api.AuthService;
import com.rubberhuman.client.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;

    private final AuthService authService = new AuthService();

    @FXML
    protected void onRegisterButtonClick() {
        String u = usernameField.getText();
        String p = passwordField.getText();
        String cp = confirmPasswordField.getText();
        String e = emailField.getText();

        if (u.isEmpty() || p.isEmpty()) {
            AlertUtil.showError("错误", "必填项不能为空");
            return;
        }
        if (!p.equals(cp)) {
            AlertUtil.showError("错误", "两次密码输入不一致");
            return;
        }

        if (authService.register(u, p, e)) {
            AlertUtil.showInfo("成功", "注册成功，请登录");
            try {
                ClientApp.setRoot("view/login");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            AlertUtil.showError("失败", "注册失败，用户名可能已存在");
        }
    }

    @FXML
    protected void onBackToLoginClick() throws IOException {
        ClientApp.setRoot("view/login");
    }
}