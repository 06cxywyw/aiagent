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
 */
@Slf4j
@Component
public class HybridVectorStoreAdapter implements VectorStore {

    @Resource
    private HybridRetriever hybridRetriever;

    // ⭐ 新版 Spring AI 用这个
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String query = request.getQuery();
        log.debug("向量搜索: {}", query);
        return hybridRetriever.retrieve(query);
    }

    // 兼容旧版接口
    @Override
    public List<Document> similaritySearch(String query) {
        log.debug("向量搜索(旧版): {}", query);
        return hybridRetriever.retrieve(query);
    }

    @Override
    public void add(List<Document> documents) {
        throw new UnsupportedOperationException("read-only vector store");
    }

    @Override
    public void delete(List<String> idList) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void delete(org.springframework.ai.vectorstore.filter.Filter.Expression expression) {
        throw new UnsupportedOperationException("not supported");
    }
}
