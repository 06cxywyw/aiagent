package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强的关键词召回器
 * 支持中文分词、同义词扩展、多策略融合
 */
@Slf4j
@Component
public class EnhancedKeywordRetriever {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ChineseTokenizer tokenizer;

    /**
     * 增强的关键词召回
     * 策略：分词 + 同义词 + 多关键词 OR
     */
    public List<Document> enhancedSearch(String query, int limit) {
        try {
            // 1. 分词
            List<String> keywords = tokenizer.extractKeywords(query, 5);
            log.info("分词结果: {}", keywords);

            // 2. 同义词扩展
            Set<String> expandedKeywords = new HashSet<>();
            for (String keyword : keywords) {
                expandedKeywords.addAll(tokenizer.expandSynonyms(keyword));
            }
            log.info("同义词扩展: {}", expandedKeywords);

            // 3. 构建 SQL（OR 逻辑）
            if (expandedKeywords.isEmpty()) {
                return Collections.emptyList();
            }

            StringBuilder sql = new StringBuilder("""
                SELECT content, metadata,
                       (
            """);

            List<Object> params = new ArrayList<>();
            int scoreIndex = 0;

            for (String keyword : expandedKeywords) {
                if (scoreIndex > 0) sql.append(" + ");
                sql.append("CASE WHEN content ILIKE ? THEN 1 ELSE 0 END");
                params.add("%" + keyword + "%");
                scoreIndex++;
            }

            sql.append("""
                       ) as match_score
                FROM vector_store
                WHERE (
            """);

            for (int i = 0; i < expandedKeywords.size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("content ILIKE ?");
            }

            sql.append("""
                )
                ORDER BY match_score DESC
                LIMIT ?
            """);

            // 添加 WHERE 参数
            params.addAll(expandedKeywords.stream()
                    .map(k -> "%" + k + "%")
                    .collect(Collectors.toList()));
            params.add(limit);

            return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "enhanced_keyword");
                    doc.getMetadata().put("match_score", rs.getInt("match_score"));
                    return doc;
                }
            );

        } catch (Exception e) {
            log.error("增强关键词搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 精确短语匹配
     */
    public List<Document> phraseSearch(String phrase, int limit) {
        try {
            String sql = """
                SELECT content, metadata
                FROM vector_store
                WHERE content LIKE ?
                LIMIT ?
                """;

            return jdbcTemplate.query(
                sql,
                new Object[]{"%" + phrase + "%", limit},
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "phrase");
                    return doc;
                }
            );

        } catch (Exception e) {
            log.error("短语搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 前缀搜索（适合自动补全）
     */
    public List<Document> prefixSearch(String prefix, int limit) {
        try {
            String sql = """
                SELECT content, metadata
                FROM vector_store
                WHERE content LIKE ?
                LIMIT ?
                """;

            return jdbcTemplate.query(
                sql,
                new Object[]{prefix + "%", limit},
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "prefix");
                    return doc;
                }
            );

        } catch (Exception e) {
            log.error("前缀搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 模糊搜索（容错）
     */
    public List<Document> fuzzySearch(String query, int limit) {
        try {
            // 使用 PostgreSQL 的相似度函数
            String sql = """
                SELECT content, metadata,
                       similarity(content, ?) as sim
                FROM vector_store
                WHERE similarity(content, ?) > 0.3
                ORDER BY sim DESC
                LIMIT ?
                """;

            return jdbcTemplate.query(
                sql,
                new Object[]{query, query, limit},
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "fuzzy");
                    doc.getMetadata().put("similarity", rs.getDouble("sim"));
                    return doc;
                }
            );

        } catch (Exception e) {
            log.warn("模糊搜索失败（需要 pg_trgm 扩展）", e);
            return Collections.emptyList();
        }
    }
}
