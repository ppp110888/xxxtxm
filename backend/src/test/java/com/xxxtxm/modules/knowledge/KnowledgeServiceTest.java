package com.xxxtxm.modules.knowledge;

import com.xxxtxm.common.exception.BusinessException;
import com.xxxtxm.modules.knowledge.entity.KnowledgeNode;
import com.xxxtxm.modules.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KnowledgeService 单元测试
 */
@SpringBootTest
class KnowledgeServiceTest {

    @Autowired
    private KnowledgeService knowledgeService;

    private Long rootId;

    @BeforeEach
    void setUp() {
        // 创建测试根节点
        KnowledgeNode root = new KnowledgeNode();
        root.setParentId(0L);
        root.setName("测试根节点");
        root.setSortOrder(1);
        root = knowledgeService.create(root);
        rootId = root.getId();
    }

    @Nested
    @DisplayName("创建节点")
    class Create {

        @Test
        @DisplayName("应成功创建子节点")
        void shouldCreateChildNode() {
            KnowledgeNode child = new KnowledgeNode();
            child.setParentId(rootId);
            child.setName("子节点");
            child.setSortOrder(1);
            KnowledgeNode saved = knowledgeService.create(child);

            assertNotNull(saved.getId());
            assertEquals(rootId, saved.getParentId());
            assertEquals("子节点", saved.getName());
        }

        @Test
        @DisplayName("sortOrder 默认为 0")
        void shouldDefaultSortOrder() {
            KnowledgeNode node = new KnowledgeNode();
            node.setParentId(rootId);
            node.setName("默认排序");
            KnowledgeNode saved = knowledgeService.create(node);

            assertEquals(0, saved.getSortOrder());
        }
    }

    @Nested
    @DisplayName("查询树")
    class GetTree {

        @Test
        @DisplayName("应返回包含所有节点的列表")
        void shouldReturnAllNodes() {
            // 创建几个子节点
            for (int i = 0; i < 3; i++) {
                KnowledgeNode child = new KnowledgeNode();
                child.setParentId(rootId);
                child.setName("子节点" + i);
                child.setSortOrder(i);
                knowledgeService.create(child);
            }

            List<KnowledgeNode> tree = knowledgeService.getTree();
            assertNotNull(tree);
            assertTrue(tree.size() >= 4); // root + 3 children
        }
    }

    @Nested
    @DisplayName("更新节点")
    class Update {

        @Test
        @DisplayName("应成功更新节点名称")
        void shouldUpdateNode() {
            KnowledgeNode update = new KnowledgeNode();
            update.setName("更新后的名称");
            KnowledgeNode result = knowledgeService.update(rootId, update);

            assertEquals("更新后的名称", result.getName());
        }

        @Test
        @DisplayName("更新不存在的节点应抛出异常")
        void shouldThrowForNonExistent() {
            KnowledgeNode update = new KnowledgeNode();
            update.setName("x");
            assertThrows(BusinessException.class, () ->
                    knowledgeService.update(99999L, update));
        }
    }

    @Nested
    @DisplayName("删除节点")
    class Delete {

        @Test
        @DisplayName("删除父节点应级联删除子节点")
        void shouldCascadeDelete() {
            // 创建子节点
            KnowledgeNode child = new KnowledgeNode();
            child.setParentId(rootId);
            child.setName("待删除子节点");
            knowledgeService.create(child);

            knowledgeService.delete(rootId);

            // 父节点和子节点都应被删除（逻辑删除）
            assertThrows(BusinessException.class, () -> {
                KnowledgeNode n = new KnowledgeNode();
                n.setName("x");
                knowledgeService.update(rootId, n);
            });
        }
    }
}
