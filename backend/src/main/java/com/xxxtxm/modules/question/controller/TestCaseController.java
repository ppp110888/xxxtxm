package com.xxxtxm.modules.question.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.question.entity.TestCase;
import com.xxxtxm.modules.question.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 测试用例控制器（管理端）
 */
@RestController
@RequestMapping("/api/v1/admin/cases")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService testCaseService;

    /** 查询某题的测试用例列表 */
    @GetMapping
    public R<List<TestCase>> list(@RequestParam Long questionId) {
        return R.ok(testCaseService.listByQuestionId(questionId));
    }

    /** 上传测试用例 */
    @PostMapping("/upload")
    public R<TestCase> upload(
            @RequestParam Long questionId,
            @RequestParam("inputFile") MultipartFile inputFile,
            @RequestParam("outputFile") MultipartFile outputFile,
            @RequestParam(defaultValue = "0") Integer isVisible) {
        TestCase testCase = testCaseService.upload(questionId, inputFile, outputFile, isVisible);
        return R.ok("上传成功", testCase);
    }

    /** 删除测试用例 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        testCaseService.delete(id);
        return R.okMsg("删除成功");
    }
}
