package com.xxxtxm.modules.progress.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.progress.entity.UserProgress;
import com.xxxtxm.modules.progress.mapper.UserProgressMapper;
import com.xxxtxm.modules.auth.entity.User;
import com.xxxtxm.modules.auth.mapper.UserMapper;
import com.xxxtxm.modules.question.entity.Question;
import com.xxxtxm.modules.question.mapper.QuestionMapper;
import com.xxxtxm.modules.submission.entity.Submission;
import com.xxxtxm.modules.submission.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个人成长中心数据控制器（学生端）
 */
@RestController
@RequestMapping("/api/v1/student/growth")
@SaCheckRole("student")
@RequiredArgsConstructor
public class GrowthController {

    private final SubmissionMapper submissionMapper;
    private final UserProgressMapper progressMapper;
    private final QuestionMapper questionMapper;
    private final UserMapper userMapper;

    /** 获取个人成长数据 */
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        Long userId = StpUtil.getLoginIdAsLong();

        // 提交统计
        List<Submission> allSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getIsDeleted, 0));

        long totalSubmissions = allSubs.size();
        long totalAc = allSubs.stream().filter(s -> "AC".equals(s.getStatus())).count();
        int acRate = totalSubmissions > 0
                ? (int) Math.round((double) totalAc / totalSubmissions * 100) : 0;

        // 最常用语言
        String preferredLang = allSubs.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getLanguage() != null ? s.getLanguage() : "unknown",
                        Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toUpperCase())
                .orElse("--");

        // 进度统计
        List<UserProgress> progressList = progressMapper.selectList(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId));
        long clearedNodes = progressList.stream()
                .filter(p -> "cleared".equals(p.getStatus())).count();
        long inProgressNodes = progressList.stream()
                .filter(p -> "in_progress".equals(p.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSubmissions", totalSubmissions);
        stats.put("totalAc", totalAc);
        stats.put("acRate", acRate);
        stats.put("preferredLanguage", preferredLang);
        stats.put("clearedNodes", clearedNodes);
        stats.put("inProgressNodes", inProgressNodes);
        stats.put("totalNodes", clearedNodes + inProgressNodes);

        return R.ok(stats);
    }

    /** 获取错题列表（非 AC 提交） */
    @GetMapping("/wrong-answers")
    public R<List<Map<String, Object>>> getWrongAnswers() {
        Long userId = StpUtil.getLoginIdAsLong();

        List<Submission> wrongSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getIsDeleted, 0)
                        .ne(Submission::getStatus, "AC")
                        .ne(Submission::getStatus, "PENDING")
                        .ne(Submission::getStatus, "JUDGING")
                        .orderByDesc(Submission::getCreateTime));

        List<Map<String, Object>> result = wrongSubs.stream()
                .limit(50)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("questionId", s.getQuestionId());
                    m.put("status", s.getStatus());
                    m.put("language", s.getLanguage());
                    m.put("timeUsed", s.getTimeUsed());
                    m.put("memoryUsed", s.getMemoryUsed());
                    m.put("createTime", s.getCreateTime());
                    return m;
                })
                .collect(Collectors.toList());

        return R.ok(result);
    }

    /** 获取提交热力图数据（每日提交计数） */
    @GetMapping("/heatmap")
    public R<List<Map<String, Object>>> getHeatmap(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int weeks) {
        Long userId = StpUtil.getLoginIdAsLong();

        LocalDate today = LocalDate.now();
        LocalDate since = today.minusWeeks(weeks);

        // 查询该用户指定时间范围内的所有提交
        List<Submission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getIsDeleted, 0)
                        .ge(Submission::getCreateTime, since.atStartOfDay())
                        .le(Submission::getCreateTime, today.plusDays(1).atStartOfDay()));

        // 按日期分组计数
        Map<LocalDate, Long> countByDate = subs.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCreateTime().toLocalDate(),
                        Collectors.counting()));

        // 生成日期范围内的每一天数据（保证前端渲染网格完整）
        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = since; !d.isAfter(today); d = d.plusDays(1)) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", d.toString());
            m.put("count", countByDate.getOrDefault(d, 0L).intValue());
            result.add(m);
        }

        return R.ok(result);
    }

    /** 错题排行榜 — 统计每道题有多少不同用户答错（所有用户可见） */
    @GetMapping("/leaderboard/wrong-ranking")
    public R<List<Map<String, Object>>> getWrongRanking() {
        // 查询所有非 AC/PENDING/JUDGING 的提交
        List<Submission> wrongSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getIsDeleted, 0)
                        .notIn(Submission::getStatus, "AC", "PENDING", "JUDGING"));

        // 按 questionId 分组，统计不同用户数
        Map<Long, Set<Long>> wrongUsersByQuestion = new HashMap<>();
        Map<Long, Long> totalAttemptsByQuestion = new HashMap<>();
        for (Submission s : wrongSubs) {
            wrongUsersByQuestion
                    .computeIfAbsent(s.getQuestionId(), k -> new HashSet<>())
                    .add(s.getUserId());
            totalAttemptsByQuestion.merge(s.getQuestionId(), 1L, Long::sum);
        }

        // 构建排行榜
        List<Map<String, Object>> ranking = wrongUsersByQuestion.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(50)
                .map(entry -> {
                    Long qid = entry.getKey();
                    int wrongUsers = entry.getValue().size();
                    long attempts = totalAttemptsByQuestion.getOrDefault(qid, 0L);

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("questionId", qid);
                    item.put("wrongUserCount", wrongUsers);
                    item.put("totalAttempts", attempts);

                    // 获取题目名称和难度
                    Question q = questionMapper.selectById(qid);
                    if (q != null) {
                        item.put("questionTitle", q.getTitle());
                        item.put("difficulty", q.getDifficulty());
                    } else {
                        item.put("questionTitle", "题目 #" + qid);
                        item.put("difficulty", "unknown");
                    }
                    return item;
                })
                .collect(Collectors.toList());

        return R.ok(ranking);
    }

    /** 通过率排行榜 — 统计每道题的 AC 通过率（所有用户可见） */
    @GetMapping("/leaderboard/pass-rate-ranking")
    public R<List<Map<String, Object>>> getPassRateRanking() {
        List<Submission> allSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getIsDeleted, 0));

        // 按 questionId 分组：统计 AC 用户集合 和 所有尝试用户集合
        Map<Long, Set<Long>> acUsersByQuestion = new HashMap<>();
        Map<Long, Set<Long>> allUsersByQuestion = new HashMap<>();

        for (Submission s : allSubs) {
            allUsersByQuestion
                    .computeIfAbsent(s.getQuestionId(), k -> new HashSet<>())
                    .add(s.getUserId());
            if ("AC".equals(s.getStatus())) {
                acUsersByQuestion
                        .computeIfAbsent(s.getQuestionId(), k -> new HashSet<>())
                        .add(s.getUserId());
            }
        }

        List<Map<String, Object>> ranking = allUsersByQuestion.entrySet().stream()
                .filter(e -> e.getValue().size() > 0)
                .sorted((a, b) -> {
                    double rateA = (double) acUsersByQuestion.getOrDefault(a.getKey(), Collections.emptySet()).size()
                            / a.getValue().size();
                    double rateB = (double) acUsersByQuestion.getOrDefault(b.getKey(), Collections.emptySet()).size()
                            / b.getValue().size();
                    return Double.compare(rateA, rateB); // 从低到高（最难的排前面）
                })
                .limit(50)
                .map(entry -> {
                    Long qid = entry.getKey();
                    int totalUsers = entry.getValue().size();
                    int acUsers = acUsersByQuestion.getOrDefault(qid, Collections.emptySet()).size();
                    double passRate = totalUsers > 0
                            ? Math.round((double) acUsers / totalUsers * 1000.0) / 10.0
                            : 0.0;

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("questionId", qid);
                    item.put("passRate", passRate);
                    item.put("acUsers", acUsers);
                    item.put("totalUsers", totalUsers);

                    Question q = questionMapper.selectById(qid);
                    if (q != null) {
                        item.put("questionTitle", q.getTitle());
                        item.put("difficulty", q.getDifficulty());
                    } else {
                        item.put("questionTitle", "题目 #" + qid);
                        item.put("difficulty", "unknown");
                    }
                    return item;
                })
                .collect(Collectors.toList());

        return R.ok(ranking);
    }

    /** 通过题目数量排行榜 — 统计每个用户 AC 的不同题目数（所有用户可见） */
    @GetMapping("/leaderboard/passed-count-ranking")
    public R<List<Map<String, Object>>> getPassedCountRanking() {
        // 查询所有 AC 提交
        List<Submission> acSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStatus, "AC"));

        // 按 userId 分组，统计每个用户通过的 different questionId 数量
        Map<Long, Set<Long>> passedQuestionsByUser = new LinkedHashMap<>();
        for (Submission s : acSubs) {
            passedQuestionsByUser
                    .computeIfAbsent(s.getUserId(), k -> new HashSet<>())
                    .add(s.getQuestionId());
        }

        // 排序取前 50
        List<Map.Entry<Long, Set<Long>>> sorted = new ArrayList<>(passedQuestionsByUser.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        if (sorted.size() > 50) {
            sorted = sorted.subList(0, 50);
        }

        // 构建排行榜（传统 for 循环，避免 lambda 内访问 field 的潜在问题）
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : sorted) {
            Long uid = entry.getKey();
            int passedCount = entry.getValue().size();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", uid);
            item.put("passedCount", passedCount);

            // 获取用户信息
            try {
                User user = userMapper.selectById(uid);
                if (user != null) {
                    item.put("nickname",
                            user.getNickname() != null && !user.getNickname().isBlank()
                                    ? user.getNickname() : user.getUsername());
                } else {
                    item.put("nickname", "用户 #" + uid);
                }
            } catch (Exception e) {
                item.put("nickname", "用户 #" + uid);
            }
            ranking.add(item);
        }

        return R.ok(ranking);
    }

    /** 我的通关列表 — 当前用户已 AC 的题目，可点击查看详情 */
    @GetMapping("/cleared-questions")
    public R<List<Map<String, Object>>> getClearedQuestions() {
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询当前用户所有 AC 提交，按题目去重取最早 AC 时间
        List<Submission> acSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getStatus, "AC")
                        .eq(Submission::getIsDeleted, 0)
                        .orderByAsc(Submission::getCreateTime));

        // 按 questionId 去重，保留最早的 AC 时间
        Map<Long, Submission> firstAcByQuestion = new LinkedHashMap<>();
        for (Submission s : acSubs) {
            firstAcByQuestion.putIfAbsent(s.getQuestionId(), s);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Submission> entry : firstAcByQuestion.entrySet()) {
            Long qid = entry.getKey();
            Submission s = entry.getValue();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", qid);
            item.put("acTime", s.getCreateTime());

            Question q = questionMapper.selectById(qid);
            if (q != null) {
                item.put("title", q.getTitle());
                item.put("difficulty", q.getDifficulty());
                item.put("languageLimit", q.getLanguageLimit());
            } else {
                item.put("title", "题目 #" + qid);
                item.put("difficulty", "unknown");
                item.put("languageLimit", "all");
            }
            result.add(item);
        }

        return R.ok(result);
    }
}
