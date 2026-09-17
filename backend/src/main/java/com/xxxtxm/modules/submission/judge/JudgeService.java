package com.xxxtxm.modules.submission.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.modules.question.entity.QuestionKnowledge;
import com.xxxtxm.modules.question.entity.TestCase;
import com.xxxtxm.modules.question.mapper.QuestionKnowledgeMapper;
import com.xxxtxm.modules.question.service.TestCaseService;
import com.xxxtxm.modules.submission.entity.Submission;
import com.xxxtxm.modules.submission.mapper.SubmissionMapper;
import com.xxxtxm.modules.submission.service.SubmissionService;
import com.xxxtxm.modules.submission.ws.JudgeWebSocketHandler;
import com.xxxtxm.modules.progress.service.ProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 判题服务 — 协调编译→运行→比对全流程
 *
 * <p>dev 模式使用本地 ProcessBuilder 执行代码</p>
 * <p>prod 模式使用 Go-Judge 沙箱执行代码</p>
 */
@Slf4j
@Service
public class JudgeService {

    private final SubmissionService submissionService;
    private final TestCaseService testCaseService;

    // dev 环境注入本地执行器，prod 环境注入 GoJudgeClient
    @Autowired(required = false)
    private DevJudgeService devJudgeService;

    @Autowired(required = false)
    private GoJudgeClient goJudgeClient;

    @Autowired(required = false)
    private JudgeWebSocketHandler wsHandler;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private QuestionKnowledgeMapper questionKnowledgeMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    public JudgeService(SubmissionService submissionService, TestCaseService testCaseService) {
        this.submissionService = submissionService;
        this.testCaseService = testCaseService;
    }

    /**
     * 自测运行（不比对，直接返回输出）
     */
    public JudgeResult run(String language, String code, String input,
                            int timeLimitMs, int memoryLimitMb) {
        return executeCode(language, code, input, timeLimitMs, memoryLimitMb);
    }

    /**
     * 正式判题 — 对全部测试用例执行并逐项比对
     */
    @Transactional
    public void judge(Long submissionId) {
        Submission submission = submissionService.getById(submissionId);
        if (submission == null) {
            log.error("Judge failed: submission {} not found", submissionId);
            return;
        }

        // 更新状态为判题中
        submissionService.updateResult(submissionId, "JUDGING", null, null, null, null);

        Long questionId = submission.getQuestionId();
        List<TestCase> testCases = testCaseService.listByQuestionId(questionId);

        if (testCases.isEmpty()) {
            // 无测试用例 → 模拟 AC（开发阶段）
            log.warn("No test cases for question {}, simulating AC", questionId);
            submissionService.updateResult(submissionId, "AC", 0, 0,
                    "{\"passed\": 1, \"total\": 1, \"note\": \"No test cases configured\"}", null);
            return;
        }

        int passed = 0;
        int total = testCases.size();
        String finalStatus = "AC";
        Integer maxTime = 0;
        Integer maxMemory = 0;
        StringBuilder resultLog = new StringBuilder();
        String compileErrorMessage = null;

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            JudgeResult result = judgeSingle(submission, tc, i + 1, total);

            // 编译错误 → 立即终止，无需继续测其他用例
            if (result.getExitCode() != 0 && !result.isTimeout() && !result.isMemoryOverflow()
                    && result.getStderr() != null && result.getStderr().contains("Compilation error")) {
                finalStatus = "CE";
                compileErrorMessage = result.getStderr();
                break;
            }

            // 追踪最大耗时/内存
            if (result.getTimeUsed() != null && result.getTimeUsed() > maxTime) {
                maxTime = result.getTimeUsed();
            }
            if (result.getMemoryUsed() != null && result.getMemoryUsed() > maxMemory) {
                maxMemory = result.getMemoryUsed();
            }

            if (result.getExitCode() == 0 && !result.isTimeout() && !result.isMemoryOverflow()) {
                passed++;
                resultLog.append(String.format("[✓] Case %d: passed (%dms)\n",
                        i + 1, result.getTimeUsed()));
            } else {
                if (result.isTimeout()) {
                    finalStatus = "TLE";
                    resultLog.append(String.format("[✗] Case %d: Time Limit Exceeded\n", i + 1));
                } else if (result.isMemoryOverflow()) {
                    finalStatus = "MLE";
                    resultLog.append(String.format("[✗] Case %d: Memory Limit Exceeded\n", i + 1));
                } else {
                    // 比对输出（或运行时错误）
                    String expected = readFileContent(tc.getOutputFilePath());
                    if (compareOutput(result.getStdout(), expected)) {
                        passed++;
                        resultLog.append(String.format("[✓] Case %d: passed (%dms)\n",
                                i + 1, result.getTimeUsed()));
                    } else {
                        // 非零退出码且非超时/超内存 → 可能是运行时错误
                        if (result.getExitCode() != 0) {
                            finalStatus = "RE";
                            resultLog.append(String.format(
                                    "[✗] Case %d: Runtime Error (exit=%d)\n",
                                    i + 1, result.getExitCode()));
                        } else {
                            finalStatus = "WA";
                            resultLog.append(String.format(
                                    "[✗] Case %d: Wrong Answer\n  expected: %s\n  actual:   %s\n",
                                    i + 1, truncate(expected), truncate(result.getStdout())));
                        }
                    }
                    // 附带 stderr（Python/Java 运行时错误详情）
                    if (result.getStderr() != null && !result.getStderr().isBlank()
                            && !result.getStderr().contains("Compilation error")) {
                        resultLog.append("  stderr: ").append(truncate(result.getStderr())).append("\n");
                    }
                }
            }
        }

        // 更新最终结果
        String resultDetail;
        String errorMessage = null;

        if ("CE".equals(finalStatus)) {
            // 编译错误：详情放入 errorMessage，方便前端展示
            resultDetail = String.format(
                    "{\"passed\": 0, \"total\": %d, \"status\": \"CE\"}", total);
            errorMessage = compileErrorMessage;
        } else {
            resultDetail = String.format(
                    "{\"passed\": %d, \"total\": %d, \"status\": \"%s\", \"details\": \"%s\"}",
                    passed, total, finalStatus, escapeJson(resultLog.toString()));
        }

        submissionService.updateResult(submissionId, finalStatus, maxTime, maxMemory,
                resultDetail, errorMessage);

        // WebSocket 实时推送判题结果
        pushResult(submissionId, finalStatus, maxTime, maxMemory, resultDetail);

        // 判题通过 → 检查并更新对应知识节点的学习进度
        if ("AC".equals(finalStatus)) {
            tryMarkKnowledgeCleared(submission);
        }

        log.info("Judge complete: submission={}, status={}, {}/{} passed, {}ms/{}KB",
                submissionId, finalStatus, passed, total, maxTime, maxMemory);
    }

    /**
     * 对单个测试用例执行判题
     */
    private JudgeResult judgeSingle(Submission submission, TestCase testCase,
                                     int caseNum, int total) {
        String input = readFileContent(testCase.getInputFilePath());

        return executeCode(
                submission.getLanguage(), submission.getCode(), input,
                10000, 256);
    }

    // ── 进度联动 ──

    /**
     * 判题通过后检查该题关联的知识节点下的所有练习题是否全部 AC，
     * 若全部 AC 则自动标记该知识节点为"已通关"。
     */
    private void tryMarkKnowledgeCleared(Submission submission) {
        List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(
                new LambdaQueryWrapper<QuestionKnowledge>()
                        .eq(QuestionKnowledge::getQuestionId, submission.getQuestionId()));

        for (QuestionKnowledge link : links) {
            Long knowledgeId = link.getKnowledgeId();

            // 查询该知识节点下所有 practice 类型的题目
            List<QuestionKnowledge> allPractices = questionKnowledgeMapper.selectList(
                    new LambdaQueryWrapper<QuestionKnowledge>()
                            .eq(QuestionKnowledge::getKnowledgeId, knowledgeId)
                            .eq(QuestionKnowledge::getType, "practice"));

            if (allPractices.isEmpty()) continue;

            // 检查该用户是否已 AC 所有 practice 题目（包含本次提交）
            boolean allAc = true;
            for (QuestionKnowledge pk : allPractices) {
                Long acCount = submissionMapper.selectCount(
                        new LambdaQueryWrapper<Submission>()
                                .eq(Submission::getUserId, submission.getUserId())
                                .eq(Submission::getQuestionId, pk.getQuestionId())
                                .eq(Submission::getStatus, "AC"));
                if (acCount == 0) {
                    allAc = false;
                    break;
                }
            }

            if (allAc) {
                progressService.markCleared(submission.getUserId(), knowledgeId);
                log.info("用户 {} 完成知识节点 {} 下全部 {} 道练习题，自动标记为已通关",
                        submission.getUserId(), knowledgeId, allPractices.size());
            }
        }
    }

    // ── 代码执行 ──

    private JudgeResult executeCode(String language, String source, String input,
                                     int timeLimitMs, int memoryLimitMb) {
        // dev 环境优先用本地执行器
        if (devJudgeService != null) {
            try {
                return devJudgeService.execute(language, source, input, timeLimitMs, memoryLimitMb);
            } catch (Exception e) {
                log.warn("Dev judge failed: {}", e.getMessage());
            }
        }

        // prod 环境或 dev 回退使用 Go-Judge
        if (goJudgeClient != null) {
            return goJudgeClient.execute(language, source, input, timeLimitMs, memoryLimitMb);
        }

        return JudgeResult.runtimeError("No judge executor available", 0, 0);
    }

    // ── WebSocket 推送 ──

    private void pushResult(Long submissionId, String status, Integer time, Integer memory,
                             String detail) {
        if (wsHandler == null) return;
        try {
            Submission sub = submissionService.getById(submissionId);
            if (sub != null) {
                wsHandler.pushJudgeResult(sub.getUserId(), submissionId, status, time, memory, detail);
            }
        } catch (Exception ignored) {}
    }

    // ── 输出比对 ──

    /**
     * 比对 stdout 与预期输出（容忍末尾换行差异、行内空格）
     */
    public static boolean compareOutput(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        return normalize(actual).equals(normalize(expected));
    }

    private static String normalize(String s) {
        return s.trim()
                .replaceAll("[ \t]+", " ")       // 合并连续空格
                .replaceAll("\\r\\n|\\r", "\n")  // 统一换行符
                .replaceAll("\\n+$", "");         // 去除末尾空行
    }

    // ── 文件读取 ──

    private String readFileContent(String filePath) {
        if (filePath == null) return "";
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (IOException e) {
            log.debug("Cannot read file {}: {}", filePath, e.getMessage());
        }
        return "";
    }

    private String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
