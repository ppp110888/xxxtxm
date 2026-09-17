package com.xxxtxm.modules.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.auth.entity.User;
import com.xxxtxm.modules.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService 单元测试
 */
@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // 确保每个测试前退出登录
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Nested
    @DisplayName("注册功能")
    class Register {

        @Test
        @DisplayName("应成功注册新用户")
        void shouldRegisterSuccessfully() {
            User user = authService.register("testuser", "password123", "student");
            assertNotNull(user);
            assertNotNull(user.getId());
            assertEquals("testuser", user.getUsername());
            assertEquals("student", user.getRole());
            // 密码应被 BCrypt 加密
            assertNotEquals("password123", user.getPassword());
        }

        @Test
        @DisplayName("重复用户名应抛出异常")
        void shouldThrowWhenDuplicateUsername() {
            authService.register("duplicate", "pass1", "student");
            assertThrows(BusinessException.class, () ->
                    authService.register("duplicate", "pass2", "student"));
        }

        @Test
        @DisplayName("默认角色应为 student")
        void shouldDefaultToStudent() {
            User user = authService.register("newbie", "pass", null);
            assertEquals("student", user.getRole());
        }
    }

    @Nested
    @DisplayName("登录功能")
    class Login {

        @Test
        @DisplayName("正确凭据应返回 Token")
        void shouldReturnTokenWithCorrectCredentials() {
            authService.register("logintest", "correct", "student");
            String token = authService.login("logintest", "correct");
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(StpUtil.isLogin());
        }

        @Test
        @DisplayName("错误密码应抛出异常")
        void shouldThrowWithWrongPassword() {
            authService.register("test2", "right", "student");
            assertThrows(BusinessException.class, () ->
                    authService.login("test2", "wrong"));
        }

        @Test
        @DisplayName("不存在用户应抛出异常")
        void shouldThrowWithNonExistentUser() {
            assertThrows(BusinessException.class, () ->
                    authService.login("nobody", "whatever"));
        }
    }

    @Nested
    @DisplayName("用户信息获取")
    class GetUser {

        @Test
        @DisplayName("登录后应能获取当前用户信息")
        void shouldReturnLoginUser() {
            authService.register("infotest", "pass", "admin");
            authService.login("infotest", "pass");

            User user = authService.getLoginUser();
            assertNotNull(user);
            assertEquals("infotest", user.getUsername());
            assertEquals("admin", user.getRole());
        }
    }
}
