package com.xxxtxm.modules.submission.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.submission.entity.Submission;
import com.xxxtxm.modules.submission.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 提交记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionMapper submissionMapper;

    /**
     * 学生提交代码
     */
    @Transactional
    public Submission submit(Long questionId, String language, String code) {
        Long userId = StpUtil.getLoginIdAsLong();

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setQuestionId(questionId);
        submission.setLanguage(language);
        submission.setCode(code);
        submission.setStatus("PENDING");

        submissionMapper.insert(submission);
        log.info("提交记录已创建: submissionId={}, userId={}, questionId={}, language={}",
                submission.getId(), userId, questionId, language);
        // 判题任务由 SubmissionController 投递至 RabbitMQ（或同步回退），此处不重复处理
        return submission;
    }

    /**
     * 分页查询当前用户的提交记录
     */
    public Page<Submission> getMySubmissions(int pageNum, int pageSize, Long questionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(questionId != null, Submission::getQuestionId, questionId)
                .orderByDesc(Submission::getCreateTime);
        return submissionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 查询单条提交详情
     */
    public Submission getById(Long id) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new BusinessException(40400, "提交记录不存在");
        }
        return submission;
    }

    /**
     * 获取当前用户在指定题目上的最佳提交状态
     * @return Map<questionId, bestStatus> — 只返回有提交记录的题目
     */
    public Map<Long, String> getBestStatus(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        Long userId = StpUtil.getLoginIdAsLong();
        // 查询用户在指定题目上的所有 AC 提交
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .in(Submission::getQuestionId, questionIds)
                .eq(Submission::getStatus, "AC");
        List<Submission> acList = submissionMapper.selectList(wrapper);
        Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (Submission s : acList) {
            result.put(s.getQuestionId(), "AC");
        }
        // 对于没有 AC 的题目，检查是否有其他提交（WA/TLE等）
        for (Long qid : questionIds) {
            if (!result.containsKey(qid)) {
                Long count = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getQuestionId, qid));
                if (count > 0) {
                    result.put(qid, "WA");  // 有提交但未 AC
                }
            }
        }
        return result;
    }

    /**
     * 更新判题结果（由判题消费者调用）
     */
    @Transactional
    public void updateResult(Long submissionId, String status, Integer timeUsed,
                             Integer memoryUsed, String resultDetail, String errorMessage) {
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setStatus(status);
        submission.setTimeUsed(timeUsed);
        submission.setMemoryUsed(memoryUsed);
        submission.setResultDetail(resultDetail);
        submission.setErrorMessage(errorMessage);
        submissionMapper.updateById(submission);
    }
}
