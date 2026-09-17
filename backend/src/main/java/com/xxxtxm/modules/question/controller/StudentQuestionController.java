package com.xxxtxm.modules.question.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.question.entity.Question;
import com.xxxtxm.modules.question.entity.QuestionKnowledge;
import com.xxxtxm.modules.question.entity.TestCase;
import com.xxxtxm.modules.question.mapper.QuestionKnowledgeMapper;
import com.xxxtxm.modules.question.service.QuestionService;
import com.xxxtxm.modules.question.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库控制器（学生端 — 只读）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/student/questions")
@SaCheckRole("student")
@RequiredArgsConstructor
public class StudentQuestionController {

    private final QuestionService questionService;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final TestCaseService testCaseService;

    /** 获取题目详情 */
    @GetMapping("/{id}")
    public R<Question> getById(@PathVariable Long id) {
        return R.ok(questionService.getById(id));
    }

    /** 获取题目可见测试用例（含输入/输出内容） */
    @GetMapping("/{id}/cases")
    public R<List<Map<String, Object>>> getVisibleCases(@PathVariable Long id) {
        List<TestCase> cases = testCaseService.listVisibleByQuestionId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TestCase tc : cases) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tc.getId());
            item.put("questionId", tc.getQuestionId());
            item.put("isVisible", tc.getIsVisible());
            item.put("inputPreview", readPreview(tc.getInputFilePath()));
            item.put("outputPreview", readPreview(tc.getOutputFilePath()));
            result.add(item);
        }
        return R.ok(result);
    }

    private String readPreview(String filePath) {
        if (filePath == null) return "";
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.readString(path).trim();
            }
        } catch (IOException e) {
            log.debug("读取用例文件失败: {}", filePath);
        }
        return "(文件用例)";
    }

    /** 按知识节点查询关联题目 */
    @GetMapping
    public R<List<Question>> listByKnowledge(
            @RequestParam(required = false) Long knowledgeId,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<QuestionKnowledge> wrapper = new LambdaQueryWrapper<QuestionKnowledge>()
                .eq(knowledgeId != null, QuestionKnowledge::getKnowledgeId, knowledgeId)
                .eq(type != null, QuestionKnowledge::getType, type);

        List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(wrapper);
        if (links.isEmpty()) {
            return R.ok(List.of());
        }

        // 批量查询关联的题目
        List<Long> questionIds = links.stream()
                .map(QuestionKnowledge::getQuestionId)
                .distinct()
                .collect(Collectors.toList());

        List<Question> questions = questionIds.stream()
                .map(questionService::getById)
                .collect(Collectors.toList());

        return R.ok(questions);
    }
}
