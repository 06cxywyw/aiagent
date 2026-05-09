package com.example.aiagent.memory;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 长期记忆：基于向量检索的语义记忆
 * 存储重要的对话内容，支持语义检索
 */
@Slf4j
@Component
public class LongTermMemory {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    /**
     * 记忆最小长度阈值（太短的不存储）
     */
    private static final int MIN_CONTENT_LENGTH = 50;

    /**
     * 相似度阈值
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * 最大返回记忆数量
     */
    private static final int MAX_MEMORY_COUNT = 5;

    public LongTermMemory(@Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel,
                          @Qualifier("hybridVectorStoreAdapter") VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    /**
     * 添加记忆
     */
    public void addMemory(String userId, String content) {
        if (StrUtil.isBlank(content) || content.length() < MIN_CONTENT_LENGTH) {
            return;
        }

        try {
            // 检查是否已有相似记忆
            List<Document> similarDocs = searchMemories(userId, content, 1);
            if (!similarDocs.isEmpty()) {
                double maxScore = similarDocs.stream()
                        .mapToDouble(d -> (Double) d.getMetadata().getOrDefault("score", 0.0))
                        .max()
                        .orElse(0.0);

                // 相似度太高则不重复存储
                if (maxScore > SIMILARITY_THRESHOLD) {
                    log.debug("记忆已存在，跳过存储: {}", content.substring(0, Math.min(30, content.length())));
                    return;
                }
            }

            // 创建文档
            Document doc = new Document(content);
            doc.getMetadata().put("user_id", userId);
            doc.getMetadata().put("created_at", Instant.now().toString());
            doc.getMetadata().put("source", "long_term_memory");

            // 入库
            vectorStore.add(List.of(doc));
            log.info("长期记忆已存储: {}", content.substring(0, Math.min(30, content.length())));

        } catch (Exception e) {
            log.error("长期记忆存储失败", e);
        }
    }

    /**
     * 检索相关记忆
     */
    public List<String> retrieveMemories(String userId, String query, int limit) {
        List<Document> docs = searchMemories(userId, query, limit);

        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    /**
     * 搜索记忆
     */
    private List<Document> searchMemories(String userId, String query, int limit) {
        try {
            // 使用 VectorStore 搜索
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            // 过滤用户相关的记忆
            List<Document> userDocs = results.stream()
                    .filter(doc -> {
                        Object uid = doc.getMetadata().get("user_id");
                        return uid == null || uid.toString().equals(userId);
                    })
                    .collect(Collectors.toList());

            // 添加相似度得分到 metadata
            for (int i = 0; i < userDocs.size(); i++) {
                userDocs.get(i).getMetadata().put("score", 1.0 - (double) i / userDocs.size());
            }

            return userDocs;

        } catch (Exception e) {
            log.error("记忆检索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 从对话中提取重要信息并存储
     */
    public void extractAndStore(String userId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (Message message : messages) {
            String content = message.getText();

            // 跳过系统消息
            if (message instanceof UserMessage || message instanceof AssistantMessage) {
                addMemory(userId, content);
            }
        }
    }

    /**
     * 清空用户记忆
     */
    public void clearMemory(String userId) {
        // TODO: 实现删除指定用户的所有记忆
        log.warn("clearMemory 未实现");
    }
}
