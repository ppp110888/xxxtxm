package com.xxxtxm.modules.submission;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.auth.service.AuthService;
import com.xxxtxm.modules.submission.entity.Submission;
import com.xxxtxm.modules.submission.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SubmissionService 单元测试
 */
@SpringBootTest
class SubmissionServiceTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        if (StpUtil.isLogin()) StpUtil.logout();
    }

    @Nested
    @DisplayName("提交代码")
    class Submit {

        @Test
        @DisplayName("应成功创建 PENDING 状态的提交")
        void shouldCreatePendingSubmission() {
            // 登录
            authService.register("submituser", "pass", "student");
            authService.login("submituser", "pass");

            Submission sub = submissionService.submit(1L, "python", "print(1+2)");

            assertNotNull(sub);
            assertNotNull(sub.getId());
            assertEquals("PENDING", sub.getStatus());
            assertEquals("python", sub.getLanguage());
            assertEquals("print(1+2)", sub.getCode());
        }

        @Test
        @DisplayName("未登录应抛出异常")
        void shouldThrowWhenNotLoggedIn() {
            assertThrows(Exception.class, () ->
                    submissionService.submit(1L, "java", "// empty"));
        }
    }

    @Nested
    @DisplayName("查询提交记录")
    class QuerySubmissions {

        @Test
        @DisplayName("应返回当前用户的提交分页")
        void shouldReturnMySubmissions() {
            authService.register("queryuser", "pass", "student");
            authService.login("queryuser", "pass");

            submissionService.submit(1L, "python", "print(1)");
            submissionService.submit(1L, "java", "// hi");

            Page<Submission> page = submissionService.getMySubmissions(1, 10, null);
            assertNotNull(page);
            assertEquals(2, page.getTotal());
            assertTrue(page.getRecords().stream().allMatch(s -> s.getLanguage() != null));
        }

        @Test
        @DisplayName("应按 questionId 过滤提交记录")
        void shouldFilterByQuestionId() {
            authService.register("filteruser", "pass", "student");
            authService.login("filteruser", "pass");

            submissionService.submit(1L, "python", "a");
            submissionService.submit(2L, "python", "b");

            Page<Submission> page = submissionService.getMySubmissions(1, 10, 1L);
            assertEquals(1, page.getTotal());
        }

        @Test
        @DisplayName("应返回提交详情")
        void shouldReturnSubmissionDetail() {
            authService.register("detailuser", "pass", "student");
            authService.login("detailuser", "pass");

            Submission created = submissionService.submit(1L, "java", "public class Main {}");
            Submission detail = submissionService.getById(created.getId());

            assertEquals(created.getId(), detail.getId());
            assertEquals("java", detail.getLanguage());
        }

        @Test
        @DisplayName("查询不存在的提交应抛出异常")
        void shouldThrowForNonExistent() {
            authService.register("nouser", "pass", "student");
            authService.login("nouser", "pass");

            assertThrows(BusinessException.class, () ->
                    submissionService.getById(99999L));
        }
    }

    @Nested
    @DisplayName("更新判题结果")
    class UpdateResult {

        @Test
        @DisplayName("应正确更新判题状态")
        void shouldUpdateJudgeResult() {
            authService.register("judgeuser", "pass", "student");
            authService.login("judgeuser", "pass");

            Submission sub = submissionService.submit(1L, "python", "print('hi')");

            submissionService.updateResult(sub.getId(), "AC", 120, 45,
                    "{\"passed\": 10, \"total\": 10}", null);

            Submission updated = submissionService.getById(sub.getId());
            assertEquals("AC", updated.getStatus());
            assertEquals(120, updated.getTimeUsed());
            assertEquals(45, updated.getMemoryUsed());
            assertEquals("{\"passed\": 10, \"total\": 10}", updated.getResultDetail());
        }

        @Test
        @DisplayName("应正确记录编译错误信息")
        void shouldRecordErrorMessage() {
            authService.register("ceuser", "pass", "student");
            authService.login("ceuser", "pass");

            Submission sub = submissionService.submit(1L, "java", "broken code");

            submissionService.updateResult(sub.getId(), "CE", null, null,
                    null, "SyntaxError: unexpected token");

            Submission updated = submissionService.getById(sub.getId());
            assertEquals("CE", updated.getStatus());
            assertEquals("SyntaxError: unexpected token", updated.getErrorMessage());
        }
    }
}
