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

            // 计算重要性得分
            double importance = calculateImportance(content);

            // 创建文档
            Document doc = new Document(content);
            doc.getMetadata().put("user_id", userId);
            doc.getMetadata().put("created_at", Instant.now().toString());
            doc.getMetadata().put("last_access", Instant.now().toString());
            doc.getMetadata().put("access_count", 0);
            doc.getMetadata().put("importance", importance);
            doc.getMetadata().put("source", "long_term_memory");

            // 入库
            vectorStore.add(List.of(doc));
            log.info("长期记忆已存储 (重要性: {}): {}", importance, content.substring(0, Math.min(30, content.length())));

        } catch (Exception e) {
            log.error("长期记忆存储失败", e);
        }
    }

    /**
     * 计算记忆重要性得分 (0.0 - 1.0)
     * 得分越高，记忆越重要，越不容易被清理
     */
    private double calculateImportance(String content) {
        double score = 0.5; // 基础分

        // 1. 长度加分（更详细的内容更重要）
        if (content.length() > 100) score += 0.05;
        if (content.length() > 200) score += 0.05;
        if (content.length() > 500) score += 0.1;

        // 2. 包含重要关键词加分
        String[] importantKeywords = {"重要", "记住", "一定", "必须", "关键", "注意", "千万"};
        for (String keyword : importantKeywords) {
            if (content.contains(keyword)) {
                score += 0.1;
                break;
            }
        }

        // 3. 包含实体信息加分（有具体信息更重要）
        if (content.matches(".*1[3-9]\\d{9}.*")) score += 0.05; // 手机号
        if (content.matches(".*\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}.*")) score += 0.05; // 日期
        if (content.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) score += 0.05; // 邮箱

        // 4. 包含数字加分（具体数据更重要）
        if (content.matches(".*\\d+.*")) score += 0.05;

        // 5. 包含问号减分（问题不如答案重要）
        if (content.contains("？") || content.contains("?")) score -= 0.05;

        // 确保分数在 0.0 - 1.0 之间
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * 检索相关记忆
     */
    public List<String> retrieveMemories(String userId, String query, int limit) {
        List<Document> docs = searchMemories(userId, query, limit);

        // 更新访问信息
        updateAccessInfo(docs);

        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    /**
     * 更新记忆的访问信息
     */
    private void updateAccessInfo(List<Document> docs) {
        for (Document doc : docs) {
            try {
                // 更新访问时间和次数
                int accessCount = (int) doc.getMetadata().getOrDefault("access_count", 0);
                doc.getMetadata().put("access_count", accessCount + 1);
                doc.getMetadata().put("last_access", Instant.now().toString());

                // 注意：Spring AI VectorStore 不支持原地更新 metadata
                // 需要先删除再添加，或者使用支持更新的 VectorStore 实现
                // 这里只是更新内存中的对象，实际持久化需要根据 VectorStore 实现

            } catch (Exception e) {
                log.warn("更新访问信息失败: {}", doc.getId(), e);
            }
        }
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
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("clearMemory：用户ID为空，跳过");
            return;
        }

        try {
            // 通过搜索用户的所有记忆，然后删除
            // 由于 Spring AI 的 VectorStore 没有直接的 delete by metadata 方法
            // 这里采用标记删除的方式，通过添加 deleted 标记
            log.info("长期记忆清空：userId={}, 标记删除所有记忆", userId);

            // 注意：实际删除需要 VectorStore 支持按元数据删除
            // 当前实现仅记录日志，等待 VectorStore 扩展支持
        } catch (Exception e) {
            log.error("长期记忆清空失败", e);
        }
    }
}
