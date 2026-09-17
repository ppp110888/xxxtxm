package com.xxxtxm.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.auth.entity.User;
import com.xxxtxm.modules.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端用户服务
 *
 * <p>提供用户列表查询、信息修改、禁用/启用功能。
 * 不提供创建（已有注册接口），不暴露密码字段。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;

    /**
     * 分页查询用户列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param username 可选的用户名模糊搜索
     * @param role     可选的角色筛选
     * @return 分页结果（password 字段已清除）
     */
    public Page<Map<String, Object>> page(int pageNum, int pageSize, String username, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(username != null && !username.isBlank(), User::getUsername, username)
                .eq(role != null && !role.isBlank(), User::getRole, role)
                .orderByDesc(User::getCreateTime);

        Page<User> raw = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 转换为 Map 并清除密码字段
        Page<Map<String, Object>> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("role", u.getRole());
            m.put("createTime", u.getCreateTime());
            m.put("updateTime", u.getUpdateTime());
            return m;
        }).toList());
        return result;
    }

    /**
     * 获取用户详情（不返回密码）
     */
    public Map<String, Object> getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(40400, "用户不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("nickname", user.getNickname());
        m.put("role", user.getRole());
        m.put("createTime", user.getCreateTime());
        m.put("updateTime", user.getUpdateTime());
        return m;
    }

    /**
     * 更新用户基本信息（昵称、角色）
     * <p>不更新密码；密码修改走专门的密码接口。</p>
     */
    @Transactional
    public void update(Long id, String nickname, String role) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(40400, "用户不存在");
        }

        if (role != null && !role.isBlank()) {
            if (!"admin".equals(role) && !"student".equals(role)) {
                throw new BusinessException(40000, "角色只能为 admin 或 student");
            }
            user.setRole(role);
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }

        userMapper.updateById(user);
        log.info("管理员更新用户信息: id={}, role={}, nickname={}", id, role, nickname);
    }

    /**
     * 软删除用户
     * <p>管理员不能删除自己。</p>
     */
    @Transactional
    public void delete(Long id) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId.equals(id)) {
            throw new BusinessException(40000, "不能删除自己的账号");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(40400, "用户不存在");
        }

        userMapper.deleteById(id);
        log.info("管理员删除用户: id={}, username={}", id, user.getUsername());
    }
}
