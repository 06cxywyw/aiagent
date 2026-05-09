package com.example.aiagent.memory;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 短期记忆：最近 10 条对话
 * 基于 Redis List 实现
 */
@Component
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate redisTemplate;

    /**
     * key 前缀
     */
    private static final String KEY_PREFIX = "chat:memory:short:";

    /**
     * 最大上下文条数（短期记忆：10条）
     */
    private static final int MAX_CONTEXT_SIZE = 10;

    /**
     * 过期时间（秒）- 3天
     */
    private static final long EXPIRE_SECONDS = 3 * 24 * 60 * 60;

    public RedisChatMemory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (StrUtil.isBlank(conversationId) || messages == null || messages.isEmpty()) {
            return;
        }

        String key = buildKey(conversationId);

        try {
            // 逐条存入 Redis List
            for (Message message : messages) {
                redisTemplate.opsForList().rightPush(key, JSONUtil.toJsonStr(message));
            }

            // 裁剪上下文（只保留最近 MAX_CONTEXT_SIZE 条）
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_CONTEXT_SIZE) {
                redisTemplate.opsForList().trim(key, size - MAX_CONTEXT_SIZE, -1);
            }

            // 设置过期时间
            redisTemplate.expire(key, EXPIRE_SECONDS, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return new ArrayList<>();
        }

        String key = buildKey(conversationId);

        try {
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);

            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }

            List<Message> result = new ArrayList<>(jsonList.size());

            for (String json : jsonList) {
                if (StrUtil.isNotBlank(json)) {
                    result.add(JSONUtil.toBean(json, Message.class));
                }
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void clear(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return;
        }

        try {
            redisTemplate.delete(buildKey(conversationId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }
}
