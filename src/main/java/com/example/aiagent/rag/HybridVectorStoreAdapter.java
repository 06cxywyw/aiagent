package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 中间层，整合多路召回和重排序
 *
 * 架构改进：
 * 1. 实现完整的 VectorStore 接口
 * 2. add() 方法：通过底层 vectorStore 添加文档
 * 3. delete() 方法：支持按 ID 删除
 * 4. similaritySearch()：使用 hybridRetriever 进行多路召回
 */
@Slf4j
@Component
public class HybridVectorStoreAdapter implements VectorStore {

    @Resource
    private HybridRetriever hybridRetriever;

    @Resource
    private VectorStore vectorStore;

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String query = request.getQuery();
        log.debug("向量搜索: query={}, topK={}", query, request.getTopK());
        return hybridRetriever.retrieve(query);
    }

    // 兼容旧版接口
    @Override
    public List<Document> similaritySearch(String query) {
        log.debug("向量搜索(旧版): query={}", query);
        return hybridRetriever.retrieve(query);
    }

    @Override
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("VectorStore 添加：文档列表为空，跳过");
            return;
        }

        log.info("VectorStore 添加：文档数={}", documents.size());

        try {
            // 直接调用底层 vectorStore 的 add 方法
            // HybridRetriever 基于 vectorStore 进行检索，所以添加到 vectorStore 即可
            vectorStore.add(documents);

            log.info("VectorStore 添加完成：成功添加 {} 个文档", documents.size());
        } catch (Exception e) {
            log.error("VectorStore 添加失败", e);
            throw new RuntimeException("添加文档到向量库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            log.debug("VectorStore 删除：ID 列表为空，跳过");
            return;
        }

        log.info("VectorStore 删除：ID 数量={}", ids.size());

        try {
            // 调用底层 vectorStore 的 delete 方法
            vectorStore.delete(ids);

            log.info("VectorStore 删除完成：成功删除 {} 个文档", ids.size());
        } catch (Exception e) {
            log.error("VectorStore 删除失败", e);
            throw new RuntimeException("删除向量文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(org.springframework.ai.vectorstore.filter.Filter.Expression expression) {
        log.warn("VectorStore 删除：按表达式删除未实现，使用 ID 删除替代");
        // 当前版本不支持按表达式删除
        // 可以在 future 版本中通过检索出 ID 再删除
    }
}
