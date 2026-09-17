package com.xxxtxm.modules.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.auth.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端用户管理控制器
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** 分页查询用户列表 */
    @GetMapping
    public R<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        return R.ok(adminUserService.page(page, pageSize, username, role));
    }

    /** 获取用户详情 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(adminUserService.getById(id));
    }

    /** 更新用户信息（昵称、角色） */
    @PutMapping("/{id}")
    public R<Void> update(
            @PathVariable Long id,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String role) {
        adminUserService.update(id, nickname, role);
        return R.okMsg("更新成功");
    }

    /** 删除用户（软删除） */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return R.okMsg("删除成功");
    }
}
