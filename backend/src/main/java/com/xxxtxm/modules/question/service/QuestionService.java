package com.xxxtxm.modules.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.question.entity.Question;
import com.xxxtxm.modules.question.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 题目服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;

    /** 分页查询 */
    public Page<Question> page(int pageNum, int pageSize, String difficulty, String languageLimit) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(difficulty != null, Question::getDifficulty, difficulty)
                .eq(languageLimit != null, Question::getLanguageLimit, languageLimit)
                .orderByDesc(Question::getCreateTime);
        return questionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /** 查询详情 */
    @Cacheable(value = "questions", key = "#id", unless = "#result == null")
    public Question getById(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(40400, "题目不存在");
        }
        return question;
    }

    /** 创建 */
    @Transactional
    public Question create(Question question) {
        if (question.getTimeLimitMs() == null) {
            question.setTimeLimitMs(1000);
        }
        if (question.getMemoryLimitMb() == null) {
            question.setMemoryLimitMb(256);
        }
        questionMapper.insert(question);
        log.info("题目已创建: {}", question.getTitle());
        return question;
    }

    /** 更新 */
    @Transactional
    @CacheEvict(value = "questions", key = "#id")
    public Question update(Long id, Question question) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(40400, "题目不存在");
        }
        question.setId(id);
        questionMapper.updateById(question);
        return questionMapper.selectById(id);
    }

    /** 删除 */
    @Transactional
    @CacheEvict(value = "questions", key = "#id")
    public void delete(Long id) {
        if (questionMapper.selectById(id) == null) {
            throw new BusinessException(40400, "题目不存在");
        }
        questionMapper.deleteById(id);
        log.info("题目已删除: id={}", id);
    }
}
