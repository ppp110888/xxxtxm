package com.xxxtxm.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识节点实体 — 树形结构
 */
@Data
@TableName("t_knowledge_node")
public class KnowledgeNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父节点 ID，0 表示根节点 */
    private Long parentId;

    /** 节点名称 */
    private String name;

    /** Markdown 讲义内容 */
    private String markdownContent;

    /** 排序字段 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
