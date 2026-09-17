package com.xxxtxm.common.config;

import cn.hutool.crypto.digest.BCrypt;
import com.xxxtxm.modules.auth.entity.User;
import com.xxxtxm.modules.auth.mapper.UserMapper;
import com.xxxtxm.modules.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 种子数据初始化
 * <p>在 dev 和 default 模式下运行，创建默认账号和学习进度。</p>
 * <p>知识树和题库由 {@link CurriculumSeeder} 负责注入。</p>
 */
@Slf4j
@Component
@Profile({"dev", "default"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final CurriculumSeeder curriculumSeeder;
    private final ProgressService progressService;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. 先注入课程体系（知识树 + 题库）
        curriculumSeeder.seedIfNeeded();

        // 2. 初始化默认用户
        Long userCount = userMapper.selectCount(null);
        if (userCount > 0) {
            log.info("数据库已有 {} 条用户，跳过用户初始化", userCount);
            return;
        }

        log.info("===== 默认用户初始化 =====");
        seedUsers();
        log.info("===== 用户初始化完成 =====");
    }

    // ── 用户 ────────────────────────────────────
    private void seedUsers() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123"));
        admin.setRole("admin");
        admin.setNickname("系统管理员");
        userMapper.insert(admin);
        log.info("✓ 管理员: admin / admin123");

        User student = new User();
        student.setUsername("student");
        student.setPassword(BCrypt.hashpw("student123"));
        student.setRole("student");
        student.setNickname("小明");
        userMapper.insert(student);
        log.info("✓ 学生: student / student123");

        // 为学生初始化学习进度（解锁所有章节入口）
        progressService.initializeProgress(student.getId());
    }
}
