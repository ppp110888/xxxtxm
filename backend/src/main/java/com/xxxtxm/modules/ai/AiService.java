package com.xxxtxm.modules.ai;

import com.xxxtxm.modules.knowledge.mapper.KnowledgeNodeMapper;
import com.xxxtxm.modules.question.service.QuestionService;
import com.xxxtxm.modules.submission.entity.Submission;
import com.xxxtxm.modules.submission.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 伴学服务 — 知识点讲解 + 学情报告
 *
 * <p>核心场景：
 *   <li>知识点讲解 — AI 用通俗语言讲解知识点，降低学习门槛</li>
 *   <li>学情报告 — 聚合错题数据，生成个性化学习建议</li>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiModelClient aiClient;
    private final SubmissionMapper submissionMapper;
    private final QuestionService questionService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    // ═══════════════════════════════════════════════════════════
    //  Prompt 模板
    // ═══════════════════════════════════════════════════════════


    private static final String SYSTEM_DIAGNOSE_CODE = """
            你是一位经验丰富的编程导师，专门帮助初学者分析代码错误并提供修复指导。

            你的诊断应该：
            1. **错误定位** — 指出代码中错误的行或逻辑，用通俗语言解释为什么错
            2. **根因分析** — 分析错误产生的根本原因（语法、逻辑、边界条件等）
            3. **修复方案** — 给出具体的修改建议和正确的代码示例
            4. **知识点回顾** — 关联到相关的编程知识点，帮助学生巩固基础
            5. **避免再犯** — 给出1-2条实用的检查清单，帮助学生在未来避免类似错误

            回复格式：使用 Markdown，代码用 ``` 包裹并标注语言。
            语气：耐心、鼓励性，不要责备学生，强调"犯错是学习的一部分"。
            长度：200-400 字，聚焦于具体错误，不要泛泛而谈。
            注意：如果你的诊断涉及完整代码，只展示需要修改的关键部分，避免给出完整答案。
            """;

    private static final String SYSTEM_REPORT = """
            你是一位经验丰富的编程教育分析师。请根据学生的错题数据，生成一份学情诊断报告。

            报告结构：
            1. **总体评价** — 2-3句话概括学生的整体学习状态
            2. **薄弱知识点** — 列出最容易出错的知识点（TOP 2-3），每个简要说明
            3. **错误类型分析** — 按 WA/TLE/RE/CE 分类，指出最突出的问题类型
            4. **学习建议** — 给出 3 条具体可行的改进建议
            5. **鼓励与展望** — 正向激励，设定下一个学习目标

            使用 Markdown 格式。语气温暖专业，有数据支撑。
            长度：300-500 字。
            """;

    private static final String SYSTEM_EXPLAIN_KNOWLEDGE = """
            你是一位耐心的编程启蒙老师，正在给零基础学员讲解一个编程知识点。

            你的讲解应该：
            1. **生活化类比** — 用一个生活中的例子来类比这个知识点，让学员容易理解
            2. **核心概念** — 用通俗的语言解释这个知识点的核心内容
            3. **代码示例** — 给一个最简单的代码示例，并逐行解释
            4. **常见误区** — 提醒1-2个初学者最容易犯的错误
            5. **小练习** — 结束时给一个思考题，引导学员动手尝试

            回复格式：使用 Markdown，代码用 ``` 包裹并标注语言。
            语气：亲切、生动、像朋友在讲解一样。
            长度：300-500 字。
            难度定位：面向零基础初学者，避免使用专业术语，必须解释每个新概念。
            """;





    // ═══════════════════════════════════════════════════════════
    //  学情报告
    // ═══════════════════════════════════════════════════════════

    /**
     * 生成学情报告（SSE 流式）
     */
    public SseEmitter generateReport(Long userId) {
        SseEmitter emitter = new SseEmitter(120_000L);

        // 收集用户的错题数据
        List<Submission> recentSubs = submissionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .eq(Submission::getIsDeleted, 0)
                        .orderByDesc(Submission::getCreateTime)
                        .last("LIMIT 200"));

        if (recentSubs.isEmpty()) {
            sendChunk(emitter, "还没有提交记录，先去 IDE 写几道题再来生成报告吧！ 🚀");
            emitter.complete();
            return emitter;
        }

        long total = recentSubs.size();
        long acCount = recentSubs.stream().filter(s -> "AC".equals(s.getStatus())).count();
        Map<String, Long> statusCounts = recentSubs.stream()
                .filter(s -> !"AC".equals(s.getStatus()) && !"PENDING".equals(s.getStatus()) && !"JUDGING".equals(s.getStatus()))
                .collect(Collectors.groupingBy(Submission::getStatus, Collectors.counting()));

        // 收集题目名称
        Set<Long> qIds = recentSubs.stream().map(Submission::getQuestionId).collect(Collectors.toSet());
        List<String> questionNames = new ArrayList<>();
        for (Long qid : qIds) {
            try { questionNames.add(questionService.getById(qid).getTitle()); } catch (Exception ignored) {}
        }

        String userPrompt = buildReportPrompt(total, acCount, statusCounts, questionNames,
                recentSubs.get(0).getLanguage());
        List<AiModelClient.Message> messages = List.of(
                new AiModelClient.Message("system", SYSTEM_REPORT),
                new AiModelClient.Message("user", userPrompt)
        );

        new Thread(() -> {
            try {
                aiClient.chatStream(messages,
                        chunk -> sendChunk(emitter, chunk),
                        () -> {
                            sendChunk(emitter, "\n\n---\n📊 *报告由 AI 自动生成*");
                            emitter.complete();
                        });
            } catch (Exception e) {
                log.error("AI 报告生成异常 userId={}", userId, e);
                sendChunk(emitter, "\n\n[AI 服务暂时不可用]");
                emitter.complete();
            }
        }, "ai-report-" + userId).start();

        return emitter;
    }

    // ═══════════════════════════════════════════════════════════
    //  代码纠错诊断（SSE 流式）
    // ═══════════════════════════════════════════════════════════

    /**
     * AI 诊断错误代码 — 分析错误原因并给出修复建议
     *
     * @param submissionId 提交记录 ID
     * @return SseEmitter 流式推送诊断内容
     */
    public SseEmitter diagnoseSubmission(Long submissionId) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            sendChunk(emitter, "[错误] 提交记录不存在");
            emitter.complete();
            return emitter;
        }

        // 只对非 AC 提交进行诊断
        if ("AC".equals(submission.getStatus()) || "PENDING".equals(submission.getStatus())
                || "JUDGING".equals(submission.getStatus())) {
            sendChunk(emitter, "当前提交状态为 " + submission.getStatus() + "，无需诊断。请提交代码获取判题结果后再试。");
            emitter.complete();
            return emitter;
        }

        // 加载题目信息
        String questionTitle = "未知题目";
        String questionDesc = "";
        try {
            var q = questionService.getById(submission.getQuestionId());
            if (q != null) {
                questionTitle = q.getTitle();
                questionDesc = q.getDescription() != null ? q.getDescription() : "";
            }
        } catch (Exception ignored) {}

        String userPrompt = buildDiagnosePrompt(submission, questionTitle, questionDesc);
        List<AiModelClient.Message> messages = List.of(
                new AiModelClient.Message("system", SYSTEM_DIAGNOSE_CODE),
                new AiModelClient.Message("user", userPrompt)
        );

        new Thread(() -> {
            try {
                aiClient.chatStream(messages,
                        chunk -> sendChunk(emitter, chunk),
                        () -> {
                            sendChunk(emitter, "\n\n---\n💡 *AI 诊断仅供参考，请理解后自行修改代码*");
                            emitter.complete();
                        });
            } catch (Exception e) {
                log.error("AI 代码诊断异常 submissionId={}", submissionId, e);
                sendChunk(emitter, "\n\n[AI 服务暂时不可用]");
                emitter.complete();
            }
        }, "ai-diagnose-" + submissionId).start();

        return emitter;
    }

    // ═══════════════════════════════════════════════════════════
    //  自由诊断（无需提交记录，SSE 流式）
    // ═══════════════════════════════════════════════════════════

    /**
     * AI 诊断当前代码 — 无需提交记录，随时可用
     *
     * @param questionId 题目 ID
     * @param language   编程语言
     * @param code       用户代码
     * @return SseEmitter 流式推送诊断内容
     */
    public SseEmitter diagnoseCode(Long questionId, String language, String code) {
        SseEmitter emitter = new SseEmitter(120_000L);

        if (code == null || code.isBlank()) {
            sendChunk(emitter, "请先编写代码，再使用 AI 诊断功能。");
            emitter.complete();
            return emitter;
        }

        // 加载题目信息
        String questionTitle = "未知题目";
        String questionDesc = "";
        try {
            var q = questionService.getById(questionId);
            if (q != null) {
                questionTitle = q.getTitle();
                questionDesc = q.getDescription() != null ? q.getDescription() : "";
            }
        } catch (Exception ignored) {}

        String userPrompt = buildDiagnoseCodePrompt(code, language, questionTitle, questionDesc);
        List<AiModelClient.Message> messages = List.of(
                new AiModelClient.Message("system", SYSTEM_DIAGNOSE_CODE),
                new AiModelClient.Message("user", userPrompt)
        );

        new Thread(() -> {
            try {
                aiClient.chatStream(messages,
                        chunk -> sendChunk(emitter, chunk),
                        () -> {
                            sendChunk(emitter, "\n\n---\n💡 *AI 诊断仅供参考，请理解后自行修改代码*");
                            emitter.complete();
                        });
            } catch (Exception e) {
                log.error("AI 代码诊断异常 questionId={}", questionId, e);
                sendChunk(emitter, "\n\n[AI 服务暂时不可用]");
                emitter.complete();
            }
        }, "ai-diagnose-code-" + questionId).start();

        return emitter;
    }

    // ═══════════════════════════════════════════════════════════
    //  知识点讲解（SSE 流式）
    // ═══════════════════════════════════════════════════════════

    /**
     * AI 讲解知识点 — 用通俗语言+生活化类比解释编程概念
     *
     * @param knowledgeId 知识节点 ID
     * @return SseEmitter 流式推送讲解内容
     */
    public SseEmitter explainKnowledge(Long knowledgeId) {
        SseEmitter emitter = new SseEmitter(120_000L);

        com.xxxtxm.modules.knowledge.entity.KnowledgeNode node =
                knowledgeNodeMapper.selectById(knowledgeId);
        if (node == null) {
            sendChunk(emitter, "[错误] 知识节点不存在");
            emitter.complete();
            return emitter;
        }

        // 截取讲义内容前 2000 字作为 AI 上下文
        String markdown = node.getMarkdownContent();
        String excerpt = (markdown != null && !markdown.isBlank())
                ? (markdown.length() > 2000 ? markdown.substring(0, 2000) + "..." : markdown)
                : "（暂无讲义内容）";

        String userPrompt = String.format("""
                请帮我讲解以下编程知识点：

                **知识点名称：** %s
                **讲义内容摘要：**
                %s

                请用通俗易懂的方式（最好有生活化类比）讲解这个知识点。
                """, node.getName(), excerpt);

        List<AiModelClient.Message> messages = List.of(
                new AiModelClient.Message("system", SYSTEM_EXPLAIN_KNOWLEDGE),
                new AiModelClient.Message("user", userPrompt)
        );

        new Thread(() -> {
            try {
                aiClient.chatStream(messages,
                        chunk -> sendChunk(emitter, chunk),
                        () -> emitter.complete());
            } catch (Exception e) {
                log.error("AI 知识点讲解异常 knowledgeId={}", knowledgeId, e);
                sendChunk(emitter, "\n\n[AI 服务暂时不可用]");
                emitter.complete();
            }
        }, "ai-explain-" + knowledgeId).start();

        return emitter;
    }



    // ═══════════════════════════════════════════════════════════
    //  Prompt 构建
    // ═══════════════════════════════════════════════════════════

    private String buildDiagnoseCodePrompt(String code, String language, String questionTitle, String questionDesc) {
        StringBuilder sb = new StringBuilder();
        sb.append("请帮我审查这段代码：\n\n");

        sb.append("**题目：** ").append(questionTitle).append("\n");
        if (questionDesc != null && !questionDesc.isBlank()) {
            String desc = questionDesc.length() > 500 ? questionDesc.substring(0, 500) + "..." : questionDesc;
            sb.append("**题目描述：** ").append(desc).append("\n");
        }

        sb.append("**编程语言：** ").append(language).append("\n");
        sb.append("\n**代码：**\n```").append(language).append("\n");
        String truncated = code;
        if (truncated.length() > 3000) truncated = truncated.substring(0, 3000) + "\n// ... (代码过长已截断)";
        sb.append(truncated).append("\n```\n");

        sb.append("\n请审查这段代码：");
        sb.append("1) 分析代码逻辑是否正确、是否满足题目要求；");
        sb.append("2) 指出潜在问题（语法错误、逻辑缺陷、边界条件遗漏、性能问题等）；");
        sb.append("3) 给出优化建议和修复方案。");
        sb.append("\n如果代码看起来基本正确，告知用户并给出进一步提升的建议。");
        return sb.toString();
    }

    private String buildDiagnosePrompt(Submission submission, String questionTitle, String questionDesc) {
        StringBuilder sb = new StringBuilder();
        sb.append("请帮我分析这段代码的错误：\n\n");

        sb.append("**题目：** ").append(questionTitle).append("\n");
        if (questionDesc != null && !questionDesc.isBlank()) {
            String desc = questionDesc.length() > 500 ? questionDesc.substring(0, 500) + "..." : questionDesc;
            sb.append("**题目描述：** ").append(desc).append("\n");
        }

        sb.append("**编程语言：** ").append(submission.getLanguage()).append("\n");
        sb.append("**判题结果：** ").append(statusLabel(submission.getStatus())).append("\n");

        if (submission.getErrorMessage() != null && !submission.getErrorMessage().isBlank()) {
            String err = submission.getErrorMessage();
            if (err.length() > 800) err = err.substring(0, 800) + "...";
            sb.append("**错误信息：**\n```\n").append(err).append("\n```\n");
        }

        sb.append("**提交的代码：**\n```").append(submission.getLanguage()).append("\n");
        String code = submission.getCode();
        if (code != null && code.length() > 2000) code = code.substring(0, 2000) + "\n// ... (代码过长已截断)";
        sb.append(code != null ? code : "").append("\n```\n");

        sb.append("\n请诊断这段代码的问题，并给出修复建议。");
        return sb.toString();
    }

    private String buildReportPrompt(long total, long acCount,
                                      Map<String, Long> statusCounts,
                                      List<String> questionNames,
                                      String preferredLang) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是一位编程学习者的近期数据，请生成学情诊断报告：\n\n");

        long acRate = total > 0 ? acCount * 100 / total : 0;
        sb.append("- **总提交次数：** ").append(total).append("\n");
        sb.append("- **通过率：** ").append(acRate).append("%（").append(acCount).append("/").append(total).append("）\n");
        sb.append("- **使用语言：** ").append(preferredLang != null ? preferredLang : "未知").append("\n");

        if (!statusCounts.isEmpty()) {
            sb.append("- **错误分布：** ");
            statusCounts.forEach((k, v) -> sb.append(statusLabel(k)).append(" ").append(v).append("次, "));
            sb.setLength(sb.length() - 2); // 去掉末尾逗号
            sb.append("\n");
        }

        if (!questionNames.isEmpty()) {
            sb.append("- **做过的题目：** ");
            questionNames.stream().limit(10).forEach(n -> sb.append(n).append("、"));
            sb.setLength(sb.length() - 1);
            sb.append("\n");
        }

        sb.append("\n请分析学习者的薄弱环节，并给出改进建议。");
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════════════════

    private String statusLabel(String s) {
        return switch (s) {
            case "AC" -> "✅ 通过 (AC)";
            case "WA" -> "❌ 答案错误 (WA)";
            case "TLE" -> "⏰ 运行超时 (TLE)";
            case "MLE" -> "💾 内存超限 (MLE)";
            case "RE" -> "💥 运行错误 (RE)";
            case "CE" -> "📝 编译错误 (CE)";
            case "SE" -> "⚠️ 系统错误 (SE)";
            default -> s;
        };
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException e) {
            log.debug("SSE 发送失败，客户端可能已断开");
            emitter.completeWithError(e);
        }
    }

    private SseEmitter error(SseEmitter emitter, String msg) {
        sendChunk(emitter, "[错误] " + msg);
        emitter.complete();
        return emitter;
    }
}
