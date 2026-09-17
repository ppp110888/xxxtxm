package com.xxxtxm.modules.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.knowledge.entity.KnowledgeNode;
import com.xxxtxm.modules.knowledge.mapper.KnowledgeNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识树服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeNodeMapper knowledgeNodeMapper;

    /**
     * 根据 ID 查询单个节点
     */
    public KnowledgeNode getById(Long id) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new BusinessException(40400, "知识节点不存在");
        }
        return node;
    }

    /**
     * 获取整棵知识树（带 children 嵌套）
     * <p>缓存于 Redis，TTL 2 小时，写操作时主动清除</p>
     */
    @Cacheable(value = "knowledgeTree", key = "'tree'")
    public List<KnowledgeNode> getTree() {
        List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .orderByAsc(KnowledgeNode::getSortOrder));

        // 构建树：parentId → children
        Map<Long, List<KnowledgeNode>> childrenMap = allNodes.stream()
                .collect(Collectors.groupingBy(KnowledgeNode::getParentId));

        // 返回根节点（parentId == 0）
        List<KnowledgeNode> roots = childrenMap.getOrDefault(0L, Collections.emptyList());

        // 不需要递归嵌套，前端 TreeTable 通过 parentId 自己组织
        // 这里直接返回全量平铺列表，前端组件自己处理层级
        return allNodes;
    }

    /**
     * 创建节点
     */
    @Transactional
    @CacheEvict(value = "knowledgeTree", allEntries = true)
    public KnowledgeNode create(KnowledgeNode node) {
        if (node.getSortOrder() == null) {
            node.setSortOrder(0);
        }
        knowledgeNodeMapper.insert(node);
        log.info("知识节点已创建: {} (parent={})", node.getName(), node.getParentId());
        return node;
    }

    /**
     * 更新节点
     */
    @Transactional
    @CacheEvict(value = "knowledgeTree", allEntries = true)
    public KnowledgeNode update(Long id, KnowledgeNode node) {
        KnowledgeNode exist = knowledgeNodeMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(40400, "知识节点不存在");
        }
        node.setId(id);
        knowledgeNodeMapper.updateById(node);
        log.info("知识节点已更新: {}", node.getName());
        return knowledgeNodeMapper.selectById(id);
    }

    /**
     * 删除节点（及其所有子节点）
     */
    @Transactional
    @CacheEvict(value = "knowledgeTree", allEntries = true)
    public void delete(Long id) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new BusinessException(40400, "知识节点不存在");
        }

        // 递归查找所有子节点
        List<Long> toDelete = new ArrayList<>();
        collectChildren(id, toDelete);
        toDelete.add(id);

        knowledgeNodeMapper.deleteBatchIds(toDelete);
        log.info("知识节点已删除: {} (共 {} 个节点)", node.getName(), toDelete.size());
    }

    /** 递归收集子节点 ID */
    private void collectChildren(Long parentId, List<Long> collector) {
        List<KnowledgeNode> children = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getParentId, parentId));
        for (KnowledgeNode child : children) {
            collector.add(child.getId());
            collectChildren(child.getId(), collector);
        }
    }
}
