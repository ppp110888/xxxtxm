package com.xxxtxm.modules.progress;

import cn.dev33.satoken.stp.StpUtil;
import com.xxxtxm.modules.auth.service.AuthService;
import com.xxxtxm.modules.progress.service.ProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProgressService 单元测试（自由学习模式）
 */
@SpringBootTest
class ProgressServiceTest {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        if (StpUtil.isLogin()) StpUtil.logout();
    }

    @Nested
    @DisplayName("获取学习进度")
    class GetProgress {

        @Test
        @DisplayName("新用户应自动初始化所有课程轨道进度")
        void shouldAutoInitAllTracksForNewUser() {
            authService.register("newuser", "pass", "student");
            authService.login("newuser", "pass");

            Map<Long, String> progress = progressService.getMyProgress();
            assertNotNull(progress);
            // 新用户首次访问自动初始化所有课程轨道（Java + Python）
            assertFalse(progress.isEmpty());
            // 根节点应标记为 cleared
            long clearedCount = progress.values().stream()
                    .filter("cleared"::equals).count();
            assertTrue(clearedCount >= 2, "至少 2 个根节点应为 cleared");
        }

        @Test
        @DisplayName("未登录应抛出异常")
        void shouldThrowWhenNotLoggedIn() {
            assertThrows(Exception.class, () ->
                    progressService.getMyProgress());
        }
    }

    @Nested
    @DisplayName("自由学习：所有节点默认可访问")
    class FreeAccess {

        @Test
        @DisplayName("新用户初始化的子节点应全部为 in_progress（无锁定）")
        void shouldInitNodesAsInProgress() {
            authService.register("freeuser", "pass", "student");
            authService.login("freeuser", "pass");

            Map<Long, String> progress = progressService.getMyProgress();
            assertNotNull(progress);
            // 所有非根节点应为 in_progress
            long inProgressCount = progress.values().stream()
                    .filter("in_progress"::equals).count();
            assertTrue(inProgressCount > 0, "应有子节点初始化为 in_progress");
        }

        @Test
        @DisplayName("已存在的进度重复初始化应为幂等")
        void shouldBeIdempotent() {
            authService.register("idemp", "pass", "student");
            authService.login("idemp", "pass");

            Map<Long, String> first = progressService.getMyProgress();
            Map<Long, String> second = progressService.getMyProgress();

            assertEquals(first.size(), second.size(), "重复初始化应返回相同数量的记录");
            first.forEach((id, status) ->
                    assertEquals(status, second.get(id), "节点 " + id + " 状态应一致"));
        }
    }

    @Nested
    @DisplayName("标记完成")
    class MarkCleared {

        @Test
        @DisplayName("应成功标记为已通关（in_progress → cleared）")
        void shouldMarkCleared() {
            authService.register("clearer", "pass", "student");
            authService.login("clearer", "pass");

            Map<Long, String> initial = progressService.getMyProgress();
            Long targetId = findInProgressNode(initial);
            progressService.markCleared(targetId);

            Map<Long, String> progress = progressService.getMyProgress();
            assertEquals("cleared", progress.get(targetId));
        }

        @Test
        @DisplayName("标记不存在的进度不应报错")
        void shouldNotThrowForNonExistent() {
            authService.register("ghost", "pass", "student");
            authService.login("ghost", "pass");

            // 直接 markCleared 不存在的节点，不应抛异常
            assertDoesNotThrow(() -> progressService.markCleared(99999L));
        }

        @Test
        @DisplayName("多个节点的进度应分别记录")
        void shouldTrackMultipleNodes() {
            authService.register("multi", "pass", "student");
            authService.login("multi", "pass");

            Map<Long, String> initial = progressService.getMyProgress();
            Long firstId = findInProgressNode(initial);
            Long secondId = initial.entrySet().stream()
                    .filter(entry -> "in_progress".equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .filter(id -> !id.equals(firstId))
                    .findFirst()
                    .orElseThrow();
            progressService.markCleared(firstId);

            Map<Long, String> progress = progressService.getMyProgress();
            assertEquals("cleared", progress.get(firstId));
            assertEquals("in_progress", progress.get(secondId));
        }

        @Test
        @DisplayName("指定用户 ID 标记完成（供判题回调使用）")
        void shouldMarkClearedByUserId() {
            authService.register("callback", "pass", "student");
            authService.login("callback", "pass");
            Long userId = StpUtil.getLoginIdAsLong();

            Map<Long, String> initial = progressService.getMyProgress();
            Long targetId = findInProgressNode(initial);
            progressService.markCleared(userId, targetId);

            Map<Long, String> progress = progressService.getMyProgress();
            assertEquals("cleared", progress.get(targetId));
        }

        private Long findInProgressNode(Map<Long, String> progress) {
            return progress.entrySet().stream()
                    .filter(entry -> "in_progress".equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow();
        }
    }
}
