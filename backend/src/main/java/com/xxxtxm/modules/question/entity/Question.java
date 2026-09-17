package com.xxxtxm.modules.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体
 */
@Data
@TableName("t_question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目标题 */
    private String title;

    /** 难度: easy / medium / hard */
    private String difficulty;

    /** 语言限制: python / java / all */
    private String languageLimit;

    /** 运行时间限制 (ms) */
    private Integer timeLimitMs;

    /** 内存限制 (MB) */
    private Integer memoryLimitMb;

    /** 题目描述 (Markdown) */
    private String description;

    /** 输入格式说明 */
    private String inputFormat;

    /** 输出格式说明 */
    private String outputFormat;

    /** 数据范围提示 */
    private String dataRange;

    /** 参考代码（例题专用） */
    private String referenceAnswer;

    /** 题目详解 Markdown（例题专用） */
    private String explanation;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
