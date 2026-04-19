package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * rag中间层
 */
@Component
public class HybridVectorStoreAdapter implements VectorStore {

    @Resource
    private HybridRetriever hybridRetriever;

    // ⭐ 新版 Spring AI 用这个
    @Override
    public List<Document> similaritySearch(SearchRequest request) {

        String query = request.getQuery();

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