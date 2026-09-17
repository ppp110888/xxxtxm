package com.xxxtxm.common.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxtxm.modules.knowledge.entity.KnowledgeNode;
import com.xxxtxm.modules.knowledge.mapper.KnowledgeNodeMapper;
import com.xxxtxm.modules.question.entity.Question;
import com.xxxtxm.modules.question.entity.QuestionKnowledge;
import com.xxxtxm.modules.question.entity.TestCase;
import com.xxxtxm.modules.question.mapper.QuestionKnowledgeMapper;
import com.xxxtxm.modules.question.mapper.QuestionMapper;
import com.xxxtxm.modules.question.mapper.TestCaseMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 课程体系种子数据注入器
 *
 * <p>读取 curriculum/ 目录下的章节定义、Markdown 讲义和题目 JSON，
 * 自动创建知识树 + 题库 + 测试用例 + 关联关系。</p>
 *
 * <p>幂等设计：检测到知识节点数 > 10 则跳过。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "default", "prod"})
public class CurriculumSeeder {

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final QuestionMapper questionMapper;
    private final TestCaseMapper testCaseMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final ResourceLoader resourceLoader;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TESTCASE_DIR = "./data/testcases/";
    private static final String CURRICULUM_PATH = "classpath:curriculum/";

    /** logicalId → 实际数据库 ID 的映射 */
    private final Map<String, Long> knowledgeIdMap = new LinkedHashMap<>();

    // ── JSON DTO ──

    @Data
    public static class ChapterDef {
        private String logicalId;
        private String name;
        private String markdownFile;
        private Integer sortOrder;
        private List<SectionDef> sections;
    }

    @Data
    public static class SectionDef {
        private String logicalId;
        private String name;
        private String markdownFile;
        private Integer sortOrder;
    }

    @Data
    public static class QuestionDef {
        private String logicalId;
        private String knowledgeLogicalId;
        private String title;
        private String difficulty;
        private String languageLimit;
        private Integer timeLimitMs;
        private Integer memoryLimitMb;
        private String description;
        private String inputFormat;
        private String outputFormat;
        private String dataRange;
        private String referenceAnswer;
        private String explanation;
        private String type;  // example / practice
        private List<TestCaseDef> testCases;
    }

    @Data
    public static class TestCaseDef {
        private String input;
        private String output;
        private Boolean visible;
    }

    // ── 入口 ──

    /**
     * 检查是否需要初始化，如需则执行完整种子注入。
     *
     * @return true 表示执行了注入，false 表示跳过
     */
    @Transactional
    public boolean seedIfNeeded() {
        Long nodeCount = knowledgeNodeMapper.selectCount(null);
        if (nodeCount > 10) {
            log.info("知识树已有 {} 个节点，跳过课程体系注入", nodeCount);
            return false;
        }

        log.info("===== 课程体系注入开始 =====");

        try {
            seedKnowledgeTree();
            seedQuestions();
            log.info("===== 课程体系注入完成: {} 个知识节点, {} 道题目 =====",
                    knowledgeIdMap.size(), questionMapper.selectCount(null));
        } catch (IOException e) {
            log.error("课程数据读取失败: {}", e.getMessage(), e);
            throw new RuntimeException("课程体系注入失败", e);
        }

        return true;
    }

    // ── 知识树 ──

    private void seedKnowledgeTree() throws IOException {
        // 1. 创建 Java 知识树
        createTrack("chapters.json", "Java 基础教程", "root.md");
        // 2. 创建 Python 知识树
        createTrack("chapters-python.json", "Python 基础教程", "root-python.md");

        log.info("✓ 知识树创建完成: {} 个节点", knowledgeIdMap.size());
    }

    private void createTrack(String chaptersFile, String rootName, String rootMdFile) throws IOException {
        Resource chaptersRes = resourceLoader.getResource(CURRICULUM_PATH + chaptersFile);
        List<ChapterDef> chapters = objectMapper.readValue(
                chaptersRes.getInputStream(),
                new TypeReference<List<ChapterDef>>() {});

        // 创建根节点
        KnowledgeNode rootNode = createNode(0L, rootName, loadMarkdown(rootMdFile), 0);
        // 使用 chaptersFile 前缀区分 Java/Python 根节点
        String rootKey = chaptersFile.replace(".json", "") + "_root";
        knowledgeIdMap.put(rootKey, rootNode.getId());

        // 逐章创建
        for (ChapterDef chapter : chapters) {
            KnowledgeNode chNode = createNode(
                    rootNode.getId(),
                    chapter.getName(),
                    loadMarkdown(chapter.getMarkdownFile()),
                    chapter.getSortOrder());
            knowledgeIdMap.put(chapter.getLogicalId(), chNode.getId());

            // 创建节节点
            if (chapter.getSections() != null) {
                for (SectionDef section : chapter.getSections()) {
                    KnowledgeNode secNode = createNode(
                            chNode.getId(),
                            section.getName(),
                            loadMarkdown(section.getMarkdownFile()),
                            section.getSortOrder());
                    knowledgeIdMap.put(section.getLogicalId(), secNode.getId());
                }
            }
        }

        log.info("✓ {} 知识树: {} 章, {} 节", rootName,
                chapters.size(),
                chapters.stream().mapToInt(c -> c.getSections() != null ? c.getSections().size() : 0).sum());
    }

    private KnowledgeNode createNode(Long parentId, String name, String markdown, int sortOrder) {
        KnowledgeNode node = new KnowledgeNode();
        node.setParentId(parentId);
        node.setName(name);
        node.setMarkdownContent(markdown);
        node.setSortOrder(sortOrder);
        knowledgeNodeMapper.insert(node);
        return node;
    }

    private String loadMarkdown(String fileName) throws IOException {
        Resource res = resourceLoader.getResource(CURRICULUM_PATH + "knowledge-content/" + fileName);
        try (var in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ── 题库 ──

    private void seedQuestions() throws IOException {
        int created = 0;
        // 注入 Java 题库
        created += seedQuestionFile("question-index.json");
        // 注入 Python 题库
        created += seedQuestionFile("question-index-python.json");
        log.info("✓ 题库创建完成: {} 道题目", created);
    }

    private int seedQuestionFile(String fileName) throws IOException {
        Resource indexRes = resourceLoader.getResource(CURRICULUM_PATH + fileName);
        List<QuestionDef> questions = objectMapper.readValue(
                indexRes.getInputStream(),
                new TypeReference<List<QuestionDef>>() {});

        int created = 0;
        for (QuestionDef def : questions) {
            Long knowledgeId = knowledgeIdMap.get(def.getKnowledgeLogicalId());
            if (knowledgeId == null) {
                log.warn("题目 {} 关联的知识节点 {} 不存在，跳过", def.getLogicalId(), def.getKnowledgeLogicalId());
                continue;
            }

            // 创建题目
            Question q = new Question();
            q.setTitle(def.getTitle());
            q.setDifficulty(def.getDifficulty() != null ? def.getDifficulty() : "easy");
            q.setLanguageLimit(def.getLanguageLimit() != null ? def.getLanguageLimit() : "java");
            q.setTimeLimitMs(def.getTimeLimitMs() != null ? def.getTimeLimitMs() : 1000);
            q.setMemoryLimitMb(def.getMemoryLimitMb() != null ? def.getMemoryLimitMb() : 256);
            q.setDescription(def.getDescription());
            q.setInputFormat(def.getInputFormat());
            q.setOutputFormat(def.getOutputFormat());
            q.setDataRange(def.getDataRange());
            q.setReferenceAnswer(def.getReferenceAnswer());
            q.setExplanation(def.getExplanation());
            questionMapper.insert(q);

            // 创建测试用例文件
            if (def.getTestCases() != null) {
                for (int i = 0; i < def.getTestCases().size(); i++) {
                    TestCaseDef tcDef = def.getTestCases().get(i);
                    createTestCaseFile(q.getId(), i + 1, tcDef);
                }
            }

            // 创建题—知识关联
            QuestionKnowledge qk = new QuestionKnowledge();
            qk.setQuestionId(q.getId());
            qk.setKnowledgeId(knowledgeId);
            qk.setType(def.getType() != null ? def.getType() : "practice");
            questionKnowledgeMapper.insert(qk);

            created++;
        }

        return created;
    }

    private void createTestCaseFile(Long questionId, int caseNum, TestCaseDef tcDef) {
        try {
            Path dir = Paths.get(TESTCASE_DIR, questionId.toString());
            Files.createDirectories(dir);

            String prefix = String.valueOf(caseNum);
            Path inputFile = dir.resolve(prefix + ".in");
            Path outputFile = dir.resolve(prefix + ".out");

            Files.writeString(inputFile, tcDef.getInput() != null ? tcDef.getInput() : "",
                    StandardCharsets.UTF_8);
            Files.writeString(outputFile, tcDef.getOutput() != null ? tcDef.getOutput() : "",
                    StandardCharsets.UTF_8);

            TestCase tc = new TestCase();
            tc.setQuestionId(questionId);
            tc.setInputFilePath(inputFile.toString());
            tc.setOutputFilePath(outputFile.toString());
            tc.setIsVisible(tcDef.getVisible() != null && tcDef.getVisible() ? 1 : 0);
            testCaseMapper.insert(tc);
        } catch (IOException e) {
            log.error("创建测试用例文件失败: questionId={}, caseNum={}", questionId, caseNum, e);
            throw new RuntimeException("测试用例文件创建失败", e);
        }
    }
}
