package com.example.aiagent.memory;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实体记忆：提取和存储关键实体信息
 * 如：人名、地点、职业、偏好等
 */
@Slf4j
@Component
public class EntityMemory {

    private final StringRedisTemplate redisTemplate;

    /**
     * key 前缀
     */
    private static final String KEY_PREFIX = "chat:memory:entity:";

    /**
     * 过期时间（秒）- 30天
     */
    private static final long EXPIRE_SECONDS = 30 * 24 * 60 * 60;

    public EntityMemory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 从消息中提取实体
     */
    public Map<String, String> extractEntities(String content) {
        Map<String, String> entities = new LinkedHashMap<>();

        if (StrUtil.isBlank(content)) {
            return entities;
        }

        // 提取人名（中文姓名）
        extractChineseNames(content, entities);

        // 提取联系方式
        extractContactInfo(content, entities);

        // 提取地点
        extractLocations(content, entities);

        // 提取时间表达式
        extractTimeExpressions(content, entities);

        // 提取偏好/喜好
        extractPreferences(content, entities);

        return entities;
    }

    /**
     * 存储实体
     */
    public void storeEntities(String userId, Map<String, String> entities) {
        if (entities.isEmpty()) {
            return;
        }

        String key = buildKey(userId);

        try {
            // 使用 Redis Hash 存储
            for (Map.Entry<String, String> entry : entities.entrySet()) {
                redisTemplate.opsForHash().put(key, entry.getKey(), entry.getValue());
            }

            // 设置过期时间
            redisTemplate.expire(key, EXPIRE_SECONDS);

            log.info("实体记忆已存储: {} -> {}", userId, entities);

        } catch (Exception e) {
            log.error("实体记忆存储失败", e);
        }
    }

    /**
     * 获取用户所有实体
     */
    public Map<String, String> getEntities(String userId) {
        String key = buildKey(userId);

        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

            if (entries == null || entries.isEmpty()) {
                return new HashMap<>();
            }

            return entries.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> e.getValue().toString()
                    ));

        } catch (Exception e) {
            log.error("获取实体记忆失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 更新实体
     */
    public void updateEntity(String userId, String entityName, String value) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().put(key, entityName, value);
        redisTemplate.expire(key, EXPIRE_SECONDS);
    }

    /**
     * 从对话中提取并存储实体
     */
    public void extractAndStore(String userId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        Map<String, String> allEntities = new HashMap<>();

        for (Message message : messages) {
            // 只处理用户消息
            if (message instanceof UserMessage) {
                Map<String, String> entities = extractEntities(message.getText());
                allEntities.putAll(entities);
            }
        }

        if (!allEntities.isEmpty()) {
            storeEntities(userId, allEntities);
        }
    }

    // ==================== 实体提取规则 ====================

    /**
     * 提取中文姓名
     */
    private void extractChineseNames(String content, Map<String, String> entities) {
        // 匹配中文姓名（2-3个汉字）
        Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,3})(?=[的了吗呢啊哦])");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String name = matcher.group(1);
            if (!entities.containsKey("name")) {
                entities.put("name", name);
            }
        }
    }

    /**
     * 提取联系方式
     */
    private void extractContactInfo(String content, Map<String, String> entities) {
        // 提取手机号
        Pattern phonePattern = Pattern.compile("1[3-9]\\d{9}");
        Matcher phoneMatcher = phonePattern.matcher(content);
        if (phoneMatcher.find()) {
            entities.put("phone", phoneMatcher.group());
        }

        // 提取邮箱
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher emailMatcher = emailPattern.matcher(content);
        if (emailMatcher.find()) {
            entities.put("email", emailMatcher.group());
        }
    }

    /**
     * 提取地点
     */
    private void extractLocations(String content, Map<String, String> entities) {
        // 常见地点关键词
        String[] locationKeywords = {"北京", "上海", "广州", "深圳", "杭州", "成都", "南京", "武汉",
                "西安", "重庆", "天津", "苏州", "厦门", "郑州", "长沙", "沈阳"};
        for (String location : locationKeywords) {
            if (content.contains(location)) {
                entities.put("location", location);
                break;
            }
        }
    }

    /**
     * 提取时间表达式
     */
    private void extractTimeExpressions(String content, Map<String, String> entities) {
        // 日期
        Pattern datePattern = Pattern.compile("(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}日?)");
        Matcher dateMatcher = datePattern.matcher(content);
        if (dateMatcher.find()) {
            entities.put("date", dateMatcher.group());
        }

        // 时间点
        Pattern timePattern = Pattern.compile("(\\d{1,2}[:：]\\d{2}(?::\\d{2})?)");
        Matcher timeMatcher = timePattern.matcher(content);
        if (timeMatcher.find()) {
            entities.put("time", timeMatcher.group());
        }
    }

    /**
     * 提取偏好/喜好
     */
    private void extractPreferences(String content, Map<String, String> entities) {
        // 喜欢/爱好
        Pattern likePattern = Pattern.compile("(喜欢|爱好|热爱|钟爱|偏爱|欣赏|喜欢的)\\s*([\\u4e00-\\u9fa5a-zA-Z0-9]+)");
        Matcher likeMatcher = likePattern.matcher(content);
        while (likeMatcher.find()) {
            String preference = likeMatcher.group(2);
            if (!entities.containsKey("preference")) {
                entities.put("preference", preference);
            }
        }

        // 不喜欢
        Pattern dislikePattern = Pattern.compile("(不喜欢|讨厌|厌恶|反感)\\s*([\\u4e00-\\u9fa5a-zA-Z0-9]+)");
        Matcher dislikeMatcher = dislikePattern.matcher(content);
        while (dislikeMatcher.find()) {
            String dislike = dislikeMatcher.group(2);
            entities.put("dislike", dislike);
        }
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId;
    }
}
