package com.yamada;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class Demo {

    @GetMapping("/")
    public Object init() {
        GDUTLoginUtil loginUtil = GDUTLoginUtil.newInstance(false);
        return loginUtil.getCookie();
    }

    @GetMapping("/getImageCode")
    public void getImageCode(HttpServletResponse response) {
        GDUTLoginUtil.getInstance().getImageCode(response);
    }

    @PostMapping("/login")
    public Object login(@RequestParam("account") String account, @RequestParam("password") String password,
                        @RequestParam("verifycode") String verifycode) {
        GDUTLoginUtil loginUtil = GDUTLoginUtil.getInstance();
        String response = loginUtil.login(account, password, verifycode);
        if (loginUtil.isLoginSuccess(response)) {
            return "登录成功，cookie为：" + loginUtil.getCookie();
        }
        return "登录失败：" + loginUtil.getLoginMessage(response);
    }
}
