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
            return;
        }

        // 1. 添加到短期记忆
        shortTermMemory.add(conversationId, messages);

        // 2. 提取并存储实体
        entityMemory.extractAndStore(conversationId, messages);

        // 3. 提取重要信息添加到长期记忆
        extractAndStoreToLongTerm(conversationId, messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        // 获取短期记忆（最近10条）
        return shortTermMemory.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        shortTermMemory.clear(conversationId);
        // TODO: 清空长期记忆和实体记忆
        log.warn("clear 未实现长期记忆和实体记忆的清空");
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

        for (String content : importantContents) {
            longTermMemory.addMemory(userId, content);
        }
    }
}
