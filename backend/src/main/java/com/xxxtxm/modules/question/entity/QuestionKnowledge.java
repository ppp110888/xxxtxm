package com.xxxtxm.modules.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题—知识节点关联实体
 */
@Data
@TableName("t_question_knowledge")
public class QuestionKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目 ID */
    private Long questionId;

    /** 知识节点 ID */
    private Long knowledgeId;

    /** 类型: example(经典例题) / practice(实战练习) */
    private String type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
