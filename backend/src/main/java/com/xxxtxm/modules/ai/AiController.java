package com.xxxtxm.modules.ai;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * AI 伴学 Controller — 知识点讲解 + 学情报告
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/student/ai")
@SaCheckRole("student")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * AI 讲解知识点（SSE 流式）
     * <p>用通俗语言 + 生活化类比解释编程概念，降低学习门槛。</p>
     */
    @GetMapping(value = "/explain/knowledge/{knowledgeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter explainKnowledge(@PathVariable Long knowledgeId) {
        log.info("AI 知识点讲解: userId={}, knowledgeId={}", StpUtil.getLoginId(), knowledgeId);
        return aiService.explainKnowledge(knowledgeId);
    }

    /**
     * 生成学情报告（SSE 流式）
     */
    @GetMapping(value = "/report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter report() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("AI 学情报告生成: userId={}", userId);
        return aiService.generateReport(userId);
    }

    /**
     * AI 诊断错误代码（SSE 流式）
     * <p>根据提交记录分析代码错误原因，给出修复建议。</p>
     */
    @GetMapping(value = "/diagnose/{submissionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diagnose(@PathVariable Long submissionId) {
        log.info("AI 代码诊断: userId={}, submissionId={}", StpUtil.getLoginId(), submissionId);
        return aiService.diagnoseSubmission(submissionId);
    }

    /**
     * AI 自由诊断代码（SSE 流式）
     * <p>无需提交记录，传入代码即可获得 AI 分析建议。随时可用。</p>
     */
    @PostMapping(value = "/diagnose-code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diagnoseCode(@RequestBody Map<String, Object> body) {
        Long questionId = body.get("questionId") instanceof Number n ? n.longValue() : null;
        String language = body.get("language") instanceof String s ? s : "python";
        String code = body.get("code") instanceof String s ? s : "";

        if (questionId == null) {
            // 返回一个已完成的 emitter 并发送错误消息
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data("[错误] 缺少 questionId 参数"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        log.info("AI 自由诊断: userId={}, questionId={}, language={}", StpUtil.getLoginId(), questionId, language);
        return aiService.diagnoseCode(questionId, language, code);
    }
}
