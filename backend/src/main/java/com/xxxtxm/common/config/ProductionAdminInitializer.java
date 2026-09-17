package com.xxxtxm.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.modules.auth.entity.User;
import com.xxxtxm.modules.auth.mapper.UserMapper;
import com.xxxtxm.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionAdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final AuthService authService;
    private final CurriculumSeeder curriculumSeeder;

    @Value("${ADMIN_USERNAME:}")
    private String username;

    @Value("${ADMIN_PASSWORD:}")
    private String password;

    @Override
    public void run(String... args) {
        curriculumSeeder.seedIfNeeded();
        if (username.isBlank() || password.isBlank()) {
            log.warn("未设置 ADMIN_USERNAME/ADMIN_PASSWORD，跳过生产管理员初始化");
            return;
        }
        boolean exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
        if (!exists) {
            authService.register(username, password, "admin");
            log.info("已创建生产管理员账号: {}", username);
        }
    }
}
