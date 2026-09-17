package com.xxxtxm.modules.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xxxtxm.common.result.R;
import com.xxxtxm.modules.knowledge.entity.KnowledgeNode;
import com.xxxtxm.modules.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识树控制器（学生端 — 只读）
 */
@RestController
@RequestMapping("/api/v1/student/knowledge")
@SaCheckRole("student")
@RequiredArgsConstructor
public class StudentKnowledgeController {

    private final KnowledgeService knowledgeService;

    /** 获取知识树（含全量节点） */
    @GetMapping("/tree")
    public R<List<KnowledgeNode>> getTree() {
        return R.ok(knowledgeService.getTree());
    }

    /** 获取单个知识节点详情 */
    @GetMapping("/{id}")
    public R<KnowledgeNode> getById(@PathVariable Long id) {
        return R.ok(knowledgeService.getById(id));
    }
}
