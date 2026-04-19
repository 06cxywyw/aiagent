package com.example.aiagent.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.document.Document;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

/**
 * 自动化评估HybridRetriever召回率的测试类
 */

@ExtendWith(MockitoExtension.class)
public class HybridRetrieverRecallTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private HybridRetriever hybridRetriever;

    // 查询与标准答案（文档id集合）映射
    private final Map<String, Set<String>> groundTruth = new HashMap<>();


    @BeforeEach
    public void setup() {
        // 示例：可根据实际业务维护标准答案
        groundTruth.put("恋爱技巧", Set.of("doc1", "doc2"));
        groundTruth.put("分手挽回", Set.of("doc3"));
        groundTruth.put("约会建议", Set.of("doc4", "doc5"));

        // mock vectorStore.similaritySearch 返回
        org.mockito.Mockito.when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            List<Document> docs = new ArrayList<>();
            if (query.equals("恋爱技巧")) {
                docs.add(mockDoc("doc1"));
            } else if (query.equals("分手挽回")) {
                docs.add(mockDoc("doc3"));
            } else if (query.equals("约会建议")) {
                docs.add(mockDoc("doc4"));
                docs.add(mockDoc("doc5"));
            }
            return docs;
        });

        // mock jdbcTemplate.query 返回，指定RowMapper类型避免重载歧义
        org.mockito.Mockito.when(jdbcTemplate.query(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(Object[].class),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Document>>any()
        )).thenReturn(Collections.emptyList());
    }

    private Document mockDoc(String id) {
        Document doc = new Document("mock content");
        doc.getMetadata().put("id", id);
        return doc;
    }

    @Test
    public void testRecall() {
        int total = groundTruth.size();
        int hit = 0;
        for (Map.Entry<String, Set<String>> entry : groundTruth.entrySet()) {
            String query = entry.getKey();
            Set<String> expectedIds = entry.getValue();
            List<Document> results = hybridRetriever.retrieve(query);
            Set<String> retrievedIds = new HashSet<>();
            for (Document doc : results) {
                Object id = doc.getMetadata().get("id");
                if (id != null) {
                    retrievedIds.add(id.toString());
                }
            }
            // 判断是否召回了标准答案中的任意一个
            boolean recall = !Collections.disjoint(expectedIds, retrievedIds);
            if (recall) hit++;
            System.out.printf("Query: %s, Recall: %s, Expected: %s, Retrieved: %s\n", query, recall, expectedIds, retrievedIds);
        }
        double recallRate = (double) hit / total;
        System.out.printf("总召回率: %.2f\n", recallRate);
        // 你可以根据业务需求设定阈值
        Assertions.assertTrue(recallRate >= 0.7, "召回率低于预期");
    }
}
