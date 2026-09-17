package com.xxxtxm.modules.progress.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户学习进度实体
 */
@Data
@TableName("t_user_progress")
public class UserProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 知识节点 ID */
    private Long knowledgeId;

    /** 状态: locked / in_progress / cleared */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
