package com.xxxtxm.modules.question.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.question.entity.Question;
import com.xxxtxm.modules.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 题库控制器（管理端）
 */
@RestController
@RequestMapping("/api/v1/admin/questions")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /** 分页列表 */
    @GetMapping
    public R<Page<Question>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String languageLimit) {
        return R.ok(questionService.page(page, pageSize, difficulty, languageLimit));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public R<Question> getById(@PathVariable Long id) {
        return R.ok(questionService.getById(id));
    }

    /** 创建 */
    @PostMapping
    public R<Question> create(@Valid @RequestBody Question question) {
        return R.ok(questionService.create(question));
    }

    /** 更新 */
    @PutMapping("/{id}")
    public R<Question> update(@PathVariable Long id, @Valid @RequestBody Question question) {
        return R.ok(questionService.update(id, question));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return R.okMsg("删除成功");
    }
}
