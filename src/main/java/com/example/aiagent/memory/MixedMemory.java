package com.example.aiagent.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 混合记忆管理器
 * 整合短期记忆、长期记忆和实体记忆
 *
 * 架构改进：
 * 1. 实现完整的 ChatMemory 接口
 * 2. 统一的 add/get/clear 操作
 * 3. 长期记忆和实体记忆随短期记忆同步更新
 */
@Slf4j
public class MixedMemory implements ChatMemory {

    private final RedisChatMemory shortTermMemory;
    private final LongTermMemory longTermMemory;
    private final EntityMemory entityMemory;

    public MixedMemory(RedisChatMemory shortTermMemory, LongTermMemory longTermMemory, EntityMemory entityMemory) {
        this.shortTermMemory = shortTermMemory;
        this.longTermMemory = longTermMemory;
        this.entityMemory = entityMemory;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            log.debug("混合记忆添加：消息列表为空，跳过");
            return;
        }

        log.debug("混合记忆添加：conversationId={}, 消息数={}", conversationId, messages.size());

        // 1. 添加到短期记忆
        shortTermMemory.add(conversationId, messages);

        // 2. 提取并存储实体（从所有消息中提取）
        entityMemory.extractAndStore(conversationId, messages);

        // 3. 提取重要信息添加到长期记忆（仅用户消息）
        extractAndStoreToLongTerm(conversationId, messages);

        log.debug("混合记忆添加完成：conversationId={}", conversationId);
    }

    @Override
    public List<Message> get(String conversationId) {
        // 获取短期记忆（最近10条）
        return shortTermMemory.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        log.info("混合记忆清空：conversationId={}", conversationId);

        // 清空短期记忆
        shortTermMemory.clear(conversationId);

        // 清空长期记忆
        longTermMemory.clearMemory(conversationId);

        // 清空实体记忆
        entityMemory.clearEntities(conversationId);

        log.info("混合记忆清空完成：conversationId={}", conversationId);
    }

    /**
     * 检索相关记忆
     */
    public List<String> retrieveRelevantMemories(String userId, String query, int limit) {
        // 优先检索长期记忆
        return longTermMemory.retrieveMemories(userId, query, limit);
    }

    /**
     * 获取实体信息
     */
    public java.util.Map<String, String> getEntityInfo(String userId) {
        return entityMemory.getEntities(userId);
    }

    /**
     * 提取重要信息并存储到长期记忆
     */
    private void extractAndStoreToLongTerm(String userId, List<Message> messages) {
        // 只存储用户消息中的重要信息
        List<String> importantContents = messages.stream()
                .filter(msg -> msg instanceof org.springframework.ai.chat.messages.UserMessage)
                .map(Message::getText)
                .filter(content -> content != null && content.length() >= 50)
                .collect(Collectors.toList());

        if (importantContents.isEmpty()) {
            log.debug("长期记忆提取：无满足长度要求的内容");
            return;
        }

        for (String content : importantContents) {
            longTermMemory.addMemory(userId, content);
        }

        log.debug("长期记忆提取：存储了 {} 条重要信息", importantContents.size());
    }
}
