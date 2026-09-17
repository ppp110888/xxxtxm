package com.xxxtxm.modules.progress.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学习进度控制器（学生端）
 */
@RestController
@RequestMapping("/api/v1/student/progress")
@SaCheckRole("student")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /** 获取我的学习进度 */
    @GetMapping
    public R<Map<Long, String>> getMyProgress() {
        return R.ok(progressService.getMyProgress());
    }
}
