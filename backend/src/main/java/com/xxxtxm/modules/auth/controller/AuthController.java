package com.xxxtxm.modules.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.auth.entity.User;
import com.xxxtxm.modules.auth.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /** 注册 */
    @PostMapping("/register")
    public R<User> register(
            @RequestParam @NotBlank String username,
            @RequestParam @NotBlank String password) {
        User user = authService.register(username, password, "student");
        // 屏蔽密码字段
        user.setPassword(null);
        return R.ok("注册成功", user);
    }

    /** 登录 */
    @PostMapping("/login")
    public R<String> login(
            @RequestParam @NotBlank String username,
            @RequestParam @NotBlank String password) {
        String token = authService.login(username, password);
        return R.ok("登录成功", token);
    }

    /** 获取当前用户信息 */
    @GetMapping("/me")
    public R<User> me() {
        User user = authService.getLoginUser();
        user.setPassword(null);
        return R.ok(user);
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.okMsg("已退出登录");
    }
}
