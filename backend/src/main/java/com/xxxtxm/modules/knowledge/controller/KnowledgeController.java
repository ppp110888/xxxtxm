package com.xxxtxm.modules.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.knowledge.entity.KnowledgeNode;
import com.xxxtxm.modules.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识树控制器（管理端）
 */
@RestController
@RequestMapping("/api/v1/admin/knowledge")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /** 获取知识树 */
    @GetMapping("/tree")
    public R<List<KnowledgeNode>> getTree() {
        return R.ok(knowledgeService.getTree());
    }

    /** 创建节点 */
    @PostMapping
    public R<KnowledgeNode> create(@Valid @RequestBody KnowledgeNode node) {
        return R.ok(knowledgeService.create(node));
    }

    /** 更新节点 */
    @PutMapping("/{id}")
    public R<KnowledgeNode> update(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeNode node) {
        return R.ok(knowledgeService.update(id, node));
    }

    /** 删除节点 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return R.okMsg("删除成功");
    }
}
