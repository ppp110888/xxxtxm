package com.xxxtxm.modules.submission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交记录实体
 */
@Data
@TableName("t_submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 题目 ID */
    private Long questionId;

    /** 语言: python / java */
    private String language;

    /** 源代码 */
    private String code;

    /** 判题状态: PENDING/COMPILING/RUNNING/JUDGING/AC/WA/TLE/MLE/RE/CE/SE */
    private String status;

    /** 运行耗时 (ms) */
    private Integer timeUsed;

    /** 内存使用 (MB) */
    private Integer memoryUsed;

    /** 判题详情 (JSON) */
    private String resultDetail;

    /** stderr / 错误信息 */
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
