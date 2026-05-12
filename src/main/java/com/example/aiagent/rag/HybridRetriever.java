package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多路召回器
 */
@Slf4j
@Component
public class HybridRetriever {

    @Resource
    @Qualifier("pgVectorVectorStore")
    private VectorStore vectorStore;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private Reranker reranker;

    @Resource
    private FullTextSearchRetriever fullTextSearchRetriever;

    /**
     * 多路召回入口
     */
    public List<Document> retrieve(String query) {

        List<Document> keywordDocs = fullTextKeywordSearch(query);
        List<Document> vectorDocs = vectorSearch(query);

        List<Document> fused = fusion(keywordDocs, vectorDocs);

        // 重排序
        List<Document> reranked = rerank(query, fused);

        // 取 top 3
        return reranked.stream().limit(3).collect(Collectors.toList());
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
     * ② 全文搜索召回（新版）
     */
    private List<Document> fullTextKeywordSearch(String query) {
        try {
            // 优先使用全文搜索
            List<Document> docs = fullTextSearchRetriever.fullTextSearch(query, 5);
            if (!docs.isEmpty()) {
                log.info("fulltext docs = {}", docs.size());
                return docs;
            }

            // 降级到简单关键词搜索
            docs = fullTextSearchRetriever.simpleKeywordSearch(query, 5);
            log.info("simple keyword docs = {}", docs.size());
            return docs;

        } catch (Exception e) {
            log.warn("全文搜索失败，降级到旧版", e);
            return keywordSearch(query);
        }
    }

    /**
     * ② 关键词召回（旧版，作为降级方案）
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
    private List<Document> fusion(List<Document> keywordDocs,
                                  List<Document> vectorDocs) {

        List<Document> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // keyword 优先
        addDocs(result, seen, keywordDocs);

        // vector 补充
        addDocs(result, seen, vectorDocs);

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

    /**
     * ④ 重排序
     */
    private List<Document> rerank(String query, List<Document> docs) {
        if (docs.size() <= 1) {
            return docs;
        }
        return reranker.rerank(query, docs);
    }
}