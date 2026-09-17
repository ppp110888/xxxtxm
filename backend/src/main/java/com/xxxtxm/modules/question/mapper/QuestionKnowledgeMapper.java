package com.xxxtxm.modules.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxxtxm.modules.question.entity.QuestionKnowledge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题—知识节点关联 Mapper
 */
@Mapper
public interface QuestionKnowledgeMapper extends BaseMapper<QuestionKnowledge> {
}
