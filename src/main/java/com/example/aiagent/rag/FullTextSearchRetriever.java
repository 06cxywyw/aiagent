package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 全文搜索召回器（优化版）
 */
@Slf4j
@Component
public class FullTextSearchRetriever {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 全文搜索召回
     * 使用 PostgreSQL 的 tsvector 和 tsquery
     */
    public List<Document> fullTextSearch(String query, int limit) {
        try {
            // 方案1：使用 to_tsvector 和 to_tsquery（中文需要分词插件）
            String sql = """
                SELECT content, metadata,
                       ts_rank(to_tsvector('simple', content), to_tsquery('simple', ?)) as rank
                FROM vector_store
                WHERE to_tsvector('simple', content) @@ to_tsquery('simple', ?)
                ORDER BY rank DESC
                LIMIT ?
                """;

            return jdbcTemplate.query(
                sql,
                new Object[]{preprocessQuery(query), preprocessQuery(query), limit},
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "fulltext");
                    doc.getMetadata().put("rank", rs.getDouble("rank"));
                    return doc;
                }
            );

        } catch (Exception e) {
            log.error("全文搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 预处理查询（转为 tsquery 格式）
     */
    private String preprocessQuery(String query) {
        // 将空格替换为 & (AND 操作)
        return query.trim().replaceAll("\\s+", " & ");
    }

    /**
     * BM25 搜索（更高级的全文搜索算法）
     */
    public List<Document> bm25Search(String query, int limit) {
        try {
            // 使用 PostgreSQL 的 BM25 扩展（需要安装 pg_bm25）
            String sql = """
                SELECT content, metadata,
                       bm25_score(content, ?) as score
                FROM vector_store
                WHERE content LIKE ?
                ORDER BY score DESC
                LIMIT ?
                """;

            return jdbcTemplate.query(
                sql,
                new Object[]{query, "%" + query + "%", limit},
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "bm25");
                    doc.getMetadata().put("score", rs.getDouble("score"));
                    return doc;
                }
            );

        } catch (Exception e) {
            log.warn("BM25 搜索失败，降级到简单搜索", e);
            return simpleKeywordSearch(query, limit);
        }
    }

    /**
     * 简单关键词搜索（降级方案）
     */
    public List<Document> simpleKeywordSearch(String query, int limit) {
        try {
            // 搜索正文内容，不依赖 metadata
            String sql = """
                SELECT content, metadata
                FROM vector_store
                WHERE content ILIKE ?
                LIMIT ?
                """;

            return jdbcTemplate.query(
                sql,
                new Object[]{"%" + query + "%", limit},
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "keyword");
                    return doc;
                }
            );

        } catch (Exception e) {
            log.error("关键词搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 多关键词搜索（OR 逻辑）
     */
    public List<Document> multiKeywordSearch(String query, int limit) {
        try {
            // 分词
            String[] keywords = query.split("\\s+");

            // 构建 SQL
            StringBuilder sql = new StringBuilder("SELECT content, metadata FROM vector_store WHERE ");
            for (int i = 0; i < keywords.length; i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("content ILIKE ?");
            }
            sql.append(" LIMIT ?");

            // 构建参数
            Object[] params = new Object[keywords.length + 1];
            for (int i = 0; i < keywords.length; i++) {
                params[i] = "%" + keywords[i] + "%";
            }
            params[keywords.length] = limit;

            return jdbcTemplate.query(
                sql.toString(),
                params,
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "multi_keyword");
                    return doc;
                }
            );

        } catch (Exception e) {
            log.error("多关键词搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 正则表达式搜索
     */
    public List<Document> regexSearch(String pattern, int limit) {
        try {
            String sql = """
                SELECT content, metadata
                FROM vector_store
                WHERE content ~* ?
                LIMIT ?
                """;

            return jdbcTemplate.query(
                sql,
                new Object[]{pattern, limit},
                (rs, i) -> {
                    Document doc = new Document(rs.getString("content"));
                    doc.getMetadata().put("source", "regex");
                    return doc;
                }
            );

        } catch (Exception e) {
            log.error("正则搜索失败", e);
            return Collections.emptyList();
        }
    }
}
