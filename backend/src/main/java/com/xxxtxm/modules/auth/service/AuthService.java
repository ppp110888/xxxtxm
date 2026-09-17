package com.xxxtxm.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.auth.entity.User;
import com.xxxtxm.modules.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;

    /**
     * 用户注册
     */
    public User register(String username, String password, String role) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setRole(role != null ? role : "student");
        user.setNickname(username);
        userMapper.insert(user);

        log.info("新用户注册: {} (角色: {})", username, user.getRole());
        return user;
    }

    /**
     * 用户登录
     * @return Sa-Token token
     */
    public String login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 登录
        StpUtil.login(user.getId());
        // 将角色信息存入 Session
        StpUtil.getSession().set("role", user.getRole());
        StpUtil.getSession().set("username", user.getUsername());

        log.info("用户登录: {} (角色: {})", username, user.getRole());

        return StpUtil.getTokenValue();
    }

    /**
     * 获取当前登录用户信息
     */
    public User getLoginUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userMapper.selectById(userId);
    }
}
