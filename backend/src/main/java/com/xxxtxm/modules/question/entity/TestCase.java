package com.xxxtxm.modules.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试用例实体
 */
@Data
@TableName("t_test_case")
public class TestCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联题目 ID */
    private Long questionId;

    /** 输入文件路径 */
    private String inputFilePath;

    /** 输出文件路径 */
    private String outputFilePath;

    /** 是否对学生可见: 0=隐藏(判题盲盒) 1=基础可见(Sample) */
    private Integer isVisible;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
