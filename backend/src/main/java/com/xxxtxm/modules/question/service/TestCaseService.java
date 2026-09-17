package com.xxxtxm.modules.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.question.entity.TestCase;
import com.xxxtxm.modules.question.mapper.TestCaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 测试用例服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseMapper testCaseMapper;

    private static final String UPLOAD_DIR = "./data/testcases/";

    /** 查询某道题的所有测试用例 */
    public List<TestCase> listByQuestionId(Long questionId) {
        return testCaseMapper.selectList(
                new LambdaQueryWrapper<TestCase>()
                        .eq(TestCase::getQuestionId, questionId)
                        .orderByAsc(TestCase::getCreateTime));
    }

    /** 查询某道题的学生可见用例 */
    public List<TestCase> listVisibleByQuestionId(Long questionId) {
        return testCaseMapper.selectList(
                new LambdaQueryWrapper<TestCase>()
                        .eq(TestCase::getQuestionId, questionId)
                        .eq(TestCase::getIsVisible, 1)
                        .orderByAsc(TestCase::getCreateTime));
    }

    /** 上传单个测试用例 */
    @Transactional
    public TestCase upload(Long questionId, MultipartFile inputFile,
                           MultipartFile outputFile, Integer isVisible) {
        try {
            // 确保目录存在
            Path dir = Paths.get(UPLOAD_DIR, questionId.toString());
            Files.createDirectories(dir);

            // 生成唯一文件名
            String uuid = UUID.randomUUID().toString().substring(0, 8);

            // 保存输入文件
            String inputPath = dir.resolve(uuid + ".in").toString();
            inputFile.transferTo(Paths.get(inputPath));

            // 保存输出文件
            String outputPath = dir.resolve(uuid + ".out").toString();
            outputFile.transferTo(Paths.get(outputPath));

            TestCase testCase = new TestCase();
            testCase.setQuestionId(questionId);
            testCase.setInputFilePath(inputPath);
            testCase.setOutputFilePath(outputPath);
            testCase.setIsVisible(isVisible != null ? isVisible : 0);

            testCaseMapper.insert(testCase);
            log.info("测试用例已上传: questionId={}, isVisible={}", questionId, isVisible);

            return testCase;
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /** 删除测试用例 */
    @Transactional
    public void delete(Long id) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null) {
            throw new BusinessException(40400, "测试用例不存在");
        }

        // 删除物理文件
        try {
            Files.deleteIfExists(Paths.get(testCase.getInputFilePath()));
            Files.deleteIfExists(Paths.get(testCase.getOutputFilePath()));
        } catch (IOException e) {
            log.warn("删除用例文件失败: {}", e.getMessage());
        }

        testCaseMapper.deleteById(id);
        log.info("测试用例已删除: id={}", id);
    }
}
