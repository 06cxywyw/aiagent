package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
@Slf4j
public class PgVectorVectorStoreConfig {

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyHashUtil myHashUtil;

    @Resource
    private SemanticSplitter semanticSplitter;

    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate,
                                           @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel,
                                           LoveAppDocumentLoader loader) {

        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .build();

        try {
            // ✅ 1. 幂等判断（更稳）
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store",
                    Integer.class
            );

            if (count != null && count > 10) {
                log.info("【RAG】向量库已有数据（{}条），跳过初始化", count);
                return vectorStore;
            }

            log.info("【RAG】开始初始化向量库...");

            // ✅ 2. 加载文档
            List<Document> documents = loader.loadMarkdowns();

            if (documents == null || documents.isEmpty()) {
                log.warn("【RAG】没有加载到文档");
                return vectorStore;
            }

            log.info("【RAG】原始文档数: {}", documents.size());

            // ✅ 3. 语义切片
            documents = semanticSplitter.splitByMarkdown(documents);
            log.info("【RAG】语义切片后: {}", documents.size());

            // ✅ 4. Token切片
            documents = myTokenTextSplitter.splitCustomized(documents);
            log.info("【RAG】Token切片后: {}", documents.size());

            // ✅ 5. 去重（提前，降低成本）
            int before = documents.size();
            documents = myHashUtil.deduplicate(documents);
            log.info("【RAG】去重: {} → {}", before, documents.size());

            // ✅ 6. 元数据增强（正确写法：分批 + 全量 + 校验）
            log.info("【RAG】开始元数据增强...");

            int enrichBatchSize = 10; // 控制QPS
            List<Document> enrichedDocs = new java.util.ArrayList<>();

            for (int i = 0; i < documents.size(); i += enrichBatchSize) {
                int end = Math.min(i + enrichBatchSize, documents.size());

                List<Document> batch = documents.subList(i, end);

                try {
                    // 👉 调用你写好的 Enricher
                    List<Document> enriched = myKeywordEnricher.enrichDocuments(batch);

                    for (Document doc : enriched) {

                        // ✅ 兜底校验（关键！！）
                        if (!doc.getMetadata().containsKey("keywords")) {

                            String text = doc.getText();
                            if (text != null) {
                                String keyword = text.length() > 10
                                        ? text.substring(0, 10)
                                        : text;

                                doc.getMetadata().put("keywords", keyword);
                            }
                        }

                        // 👉 可选：标记来源（加分点）
                        doc.getMetadata().putIfAbsent("keyword_source", "llm_or_fallback");
                    }

                    enrichedDocs.addAll(enriched);

                    log.info("【RAG】关键词增强 batch {}-{} 完成", i, end);

                } catch (Exception e) {

                    log.warn("❌【RAG】LLM增强失败，降级 batch {}-{}", i, end);

                    // 👉 整批降级
                    for (Document doc : batch) {
                        String text = doc.getText();
                        if (text != null) {
                            String keyword = text.length() > 10
                                    ? text.substring(0, 10)
                                    : text;

                            doc.getMetadata().put("keywords", keyword);
                            doc.getMetadata().put("keyword_source", "fallback");
                        }
                    }

                    enrichedDocs.addAll(batch);
                }
            }

            documents = enrichedDocs;

            log.info("【RAG】metadata示例: {}", documents.get(0).getMetadata());
            // ✅ 7. 批量入库（避免 embedding 限制）
            int batchSize = 20;
            int total = documents.size();
            int batchCount = (total + batchSize - 1) / batchSize;

            for (int i = 0; i < batchCount; i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, total);

                List<Document> batch = documents.subList(start, end);

                vectorStore.add(batch);

                log.info("【RAG】批次 {}/{} 入库完成，数量: {}", i + 1, batchCount, batch.size());
            }

            log.info("【RAG】初始化完成，总入库: {}", total);

        } catch (Exception e) {
            log.error("【RAG】初始化失败", e);
        }

        return vectorStore;
    }
}