package com.example.aiagent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存限流器
 * 基于滑动窗口算法实现的简单限流器
 * 生产环境建议使用 RedisRateLimiter
 */
@Slf4j
@Data
public class InMemoryRateLimiter {

    private final int maxRequests;
    private final int windowSizeSeconds;

    private final ConcurrentHashMap<String, RequestBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxRequests, int windowSizeSeconds) {
        this.maxRequests = maxRequests;
        this.windowSizeSeconds = windowSizeSeconds;
        log.info("内存限流器初始化: maxRequests={}, windowSize={}s", maxRequests, windowSizeSeconds);
    }

    /**
     * 检查是否允许请求
     *
     * @param clientId 客户端标识（IP 或 API Key）
     * @return true 允许，false 拒绝
     */
    public boolean allowRequest(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return false;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSizeSeconds * 1000L);

        RequestBucket bucket = buckets.computeIfAbsent(clientId, k -> new RequestBucket());

        synchronized (bucket) {
            // 清理过期记录
            bucket.requests.removeIf(timestamp -> timestamp < windowStart);

            // 检查是否超过限制
            if (bucket.requests.size() >= maxRequests) {
                log.debug("限流触发: clientId={}, 当前请求数={}", clientId, bucket.requests.size());
                return false;
            }

            // 记录请求
            bucket.requests.add(now);
            return true;
        }
    }

    /**
     * 获取当前请求数
     */
    public int getCurrentRequests(String clientId) {
        RequestBucket bucket = buckets.get(clientId);
        if (bucket == null) {
            return 0;
        }
        return bucket.requests.size();
    }

    /**
     * 请求桶
     */
    @Data
    private static class RequestBucket {
        private final java.util.concurrent.ConcurrentLinkedQueue<Long> requests = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }
}
