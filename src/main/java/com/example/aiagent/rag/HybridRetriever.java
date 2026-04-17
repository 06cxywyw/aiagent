package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 多路召回器
 */
@Slf4j
@Component
public class HybridRetriever {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 多路召回入口
     */
    public List<Document> retrieve(String query) {

        List<Document> vectorDocs = vectorSearch(query);
        List<Document> keywordDocs = keywordSearch(query);

        return fusion(vectorDocs, keywordDocs);
    }

    /**
     * ① 向量召回
     */
    private List<Document> vectorSearch(String query) {

        List<Document> docs = vectorStore.similaritySearch(query);

        for (Document doc : docs) {
            doc.getMetadata().put("source", "vector");
        }

        log.info("vector docs = {}", docs.size());
        return docs;
    }

    /**
     * ② 关键词召回（SQL + metadata）
     */
    private List<Document> keywordSearch(String query) {

        try {
            return jdbcTemplate.query(
                    """
                    SELECT content, metadata
                    FROM vector_store
                    WHERE metadata->>'keywords' ILIKE ?
                    LIMIT 5
                    """,
                    new Object[]{"%" + query + "%"},
                    (rs, i) -> {
                        Document doc = new Document(rs.getString("content"));
                        doc.getMetadata().put("source", "keyword");
                        return doc;
                    }
            );
        } catch (Exception e) {
            log.warn("keyword search failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * ③ 融合 + 去重 + 简单排序
     */
    private List<Document> fusion(List<Document> vectorDocs,
                                  List<Document> keywordDocs) {

        List<Document> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // vector 优先
        addDocs(result, seen, vectorDocs);

        // keyword 补充
        addDocs(result, seen, keywordDocs);

        return result;
    }

    private void addDocs(List<Document> result,
                         Set<String> seen,
                         List<Document> docs) {

        for (Document doc : docs) {

            String id = resolveId(doc);

            if (seen.add(id)) {
                result.add(doc);
            }
        }
    }

    /**
     * 文档唯一标识（关键）
     */
    private String resolveId(Document doc) {

        Object id = doc.getMetadata().get("id");

        if (id != null) {
            return id.toString();
        }

        return String.valueOf(doc.getText().hashCode());
    }
}