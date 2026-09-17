package com.xxxtxm.modules.submission.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxxtxm.common.config.RabbitMQConfig;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.submission.entity.Submission;
import com.xxxtxm.modules.submission.judge.JudgeResult;
import com.xxxtxm.modules.submission.judge.JudgeService;
import com.xxxtxm.modules.submission.service.SubmissionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 提交控制器（学生端）
 *
 * <p>判题模式自动切换:
 *   - 当 RabbitMQ 可用时 → 异步投递到 MQ，消费者执行判题
 *   - 当 RabbitMQ 不可用时 → 同步调用 JudgeService（dev 模式回退）
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/student")
@SaCheckRole("student")
@Validated
public class SubmissionController {

    private final SubmissionService submissionService;
    private final JudgeService judgeService;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    public SubmissionController(SubmissionService submissionService, JudgeService judgeService) {
        this.submissionService = submissionService;
        this.judgeService = judgeService;
    }

    /** 提交代码（正式判题） */
    @PostMapping("/submit")
    public R<Submission> submit(
            @RequestParam @NotNull Long questionId,
            @RequestParam @NotBlank String language,
            @RequestParam @NotBlank String code) {
        Submission submission = submissionService.submit(questionId, language, code);

        if (rabbitTemplate != null) {
            // ── 异步模式：投递到 RabbitMQ ──
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.JUDGE_EXCHANGE,
                    RabbitMQConfig.JUDGE_ROUTING_KEY,
                    submission.getId());
            log.info("Judge task sent to MQ: submissionId={}", submission.getId());
        } else {
            // ── 同步模式：直接调用判题（dev 回退）──
            try {
                judgeService.judge(submission.getId());
            } catch (Exception e) {
                log.error("Judge failed for submission {}: {}", submission.getId(), e.getMessage());
                submissionService.updateResult(submission.getId(), "SE", null, null,
                        null, "Judge system error: " + e.getMessage());
            }
        }

        // 返回最新状态
        Submission updated = submissionService.getById(submission.getId());
        return R.ok("提交成功", updated);
    }

    /** 自测运行（不记录提交，不比对答案） */
    @PostMapping("/run")
    public R<Map<String, Object>> run(
            @RequestParam @NotNull Long questionId,
            @RequestParam @NotBlank String language,
            @RequestParam @NotBlank String code,
            @RequestParam(defaultValue = "") String input) {
        // 使用默认时限
        JudgeResult result = judgeService.run(language, code, input, 10000, 256);

        Map<String, Object> data = Map.of(
                "stdout", result.getStdout(),
                "stderr", result.getStderr(),
                "exitCode", result.getExitCode(),
                "timeUsed", result.getTimeUsed(),
                "memoryUsed", result.getMemoryUsed(),
                "timeout", result.isTimeout(),
                "memoryOverflow", result.isMemoryOverflow()
        );

        return R.ok(data);
    }

    /** 我的提交列表 */
    @GetMapping("/submissions")
    public R<Page<Submission>> mySubmissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long questionId) {
        return R.ok(submissionService.getMySubmissions(page, pageSize, questionId));
    }

    /** 提交详情 */
    @GetMapping("/submissions/{id}")
    public R<Submission> getSubmission(@PathVariable Long id) {
        return R.ok(submissionService.getById(id));
    }

    /** 批量查询最佳提交状态 */
    @GetMapping("/submissions/best-status")
    public R<Map<Long, String>> getBestStatus(@RequestParam String questionIds) {
        List<Long> ids = new java.util.ArrayList<>();
        for (String s : questionIds.split(",")) {
            ids.add(Long.parseLong(s.trim()));
        }
        return R.ok(submissionService.getBestStatus(ids));
    }
}
