package com.xxxtxm.modules.progress.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.modules.knowledge.entity.KnowledgeNode;
import com.xxxtxm.modules.knowledge.mapper.KnowledgeNodeMapper;
import com.xxxtxm.modules.progress.entity.UserProgress;
import com.xxxtxm.modules.progress.mapper.UserProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户学习进度服务
 * <p>自由学习模式：所有内容均可自由访问，进度仅追踪完成状态，不设关卡限制。
 * 根节点默认已通关，其余节点初始为"学习中"，完成练习后自动标记为"已通关"。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserProgressMapper userProgressMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    /**
     * 获取当前用户的所有知识节点进度。
     * <p>首次访问时自动初始化：根节点标记为 cleared，其直接子节点（章节）标记为 in_progress。</p>
     *
     * @return Map<knowledgeId, status>
     */
    @Cacheable(value = "userProgress", key = "T(cn.dev33.satoken.stp.StpUtil).getLoginIdAsLong()")
    public Map<Long, String> getMyProgress() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserProgress> list = userProgressMapper.selectList(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId));

        // 首次访问：完整初始化
        if (list.isEmpty()) {
            list = initializeProgress(userId);
        } else {
            // 已存在用户：检查是否有新增的课程轨道（如 Python）未初始化
            List<KnowledgeNode> roots = knowledgeNodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getParentId, 0L));
            for (KnowledgeNode root : roots) {
                boolean hasRootProgress = list.stream()
                        .anyMatch(p -> p.getKnowledgeId().equals(root.getId()));
                if (!hasRootProgress) {
                    log.info("为用户 {} 补初始化课程轨道: {}", userId, root.getName());
                    List<UserProgress> newTrack = initializeTrack(userId, root);
                    list.addAll(newTrack);
                }
            }
        }

        return list.stream()
                .collect(Collectors.toMap(UserProgress::getKnowledgeId, UserProgress::getStatus));
    }

    /**
     * 初始化新用户的学习进度 — 级联解锁三层：根 → 章节 → 小节。
     * <p>支持多课程轨道（Java + Python），遍历所有根节点进行初始化。</p>
     * <p>不依赖登录态，供 DataInitializer 和首次访问的 HTTP 请求共用。</p>
     *
     * @return 新创建的进度记录列表
     */
    @Transactional
    public List<UserProgress> initializeProgress(Long userId) {
        log.info("为用户 {} 初始化学习进度", userId);

        List<KnowledgeNode> roots = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getParentId, 0L));

        if (roots.isEmpty()) {
            log.warn("未找到根节点，跳过进度初始化");
            return List.of();
        }

        List<UserProgress> all = new java.util.ArrayList<>();
        for (KnowledgeNode root : roots) {
            all.addAll(initializeTrack(userId, root));
        }

        int totalChapters = (int) all.stream()
                .filter(p -> "in_progress".equals(p.getStatus()))
                .count();
        log.info("初始化完成 — {} 个根节点已通关，{} 个节点已解锁",
                roots.size(), totalChapters);
        return all;
    }

    /**
     * 为单个课程轨道初始化进度 — 根节点 cleared，章节和小节 in_progress。
     * <p>已存在的进度记录会被跳过，避免重复插入（幂等）。</p>
     */
    private List<UserProgress> initializeTrack(Long userId, KnowledgeNode root) {
        List<UserProgress> all = new java.util.ArrayList<>();

        // 根节点 → cleared（如不存在）
        if (!exists(userId, root.getId())) {
            UserProgress rootProgress = new UserProgress();
            rootProgress.setUserId(userId);
            rootProgress.setKnowledgeId(root.getId());
            rootProgress.setStatus("cleared");
            userProgressMapper.insert(rootProgress);
            all.add(rootProgress);
        }

        // 章节 + 小节 → in_progress（跳过已存在）
        List<KnowledgeNode> chapters = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getParentId, root.getId()));
        for (KnowledgeNode chapter : chapters) {
            if (!exists(userId, chapter.getId())) {
                UserProgress cp = new UserProgress();
                cp.setUserId(userId);
                cp.setKnowledgeId(chapter.getId());
                cp.setStatus("in_progress");
                userProgressMapper.insert(cp);
                all.add(cp);
            }

            List<KnowledgeNode> sections = knowledgeNodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getParentId, chapter.getId()));
            for (KnowledgeNode section : sections) {
                if (!exists(userId, section.getId())) {
                    UserProgress sp = new UserProgress();
                    sp.setUserId(userId);
                    sp.setKnowledgeId(section.getId());
                    sp.setStatus("in_progress");
                    userProgressMapper.insert(sp);
                    all.add(sp);
                }
            }
        }
        return all;
    }

    private boolean exists(Long userId, Long knowledgeId) {
        return userProgressMapper.selectCount(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getKnowledgeId, knowledgeId)) > 0;
    }

    /**
     * 标记知识节点为已完成（in_progress → cleared）。
     * 当该节点下的所有练习题全部 AC 时由判题回调调用。
     * <p>使用 StpUtil 获取当前登录用户，适用于 HTTP 请求上下文。</p>
     */
    @Transactional
    @CacheEvict(value = "userProgress", key = "T(cn.dev33.satoken.stp.StpUtil).getLoginIdAsLong()")
    public void markCleared(Long knowledgeId) {
        markCleared(StpUtil.getLoginIdAsLong(), knowledgeId);
    }

    /**
     * 标记知识节点为已完成（in_progress → cleared）— 指定用户版本。
     * <p>供判题回调（MQ 消费者线程）等无登录上下文的场景使用。</p>
     *
     * @param userId       用户 ID
     * @param knowledgeId  知识节点 ID
     */
    @Transactional
    @CacheEvict(value = "userProgress", key = "#userId")
    public void markCleared(Long userId, Long knowledgeId) {
        UserProgress progress = userProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getKnowledgeId, knowledgeId));

        if (progress != null && !"cleared".equals(progress.getStatus())) {
            progress.setStatus("cleared");
            userProgressMapper.updateById(progress);
            log.info("用户 {} 完成知识节点 {}", userId, knowledgeId);
        }
    }
}
