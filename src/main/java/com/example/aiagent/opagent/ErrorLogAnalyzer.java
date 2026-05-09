package com.example.aiagent.opagent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运维 Agent - 错误日志聚合分析器
 * 自动聚类相同错误，提取高频问题
 */
@Slf4j
@Component
public class ErrorLogAnalyzer {

    /**
     * 错误记录存储
     */
    private final Map<String, ErrorRecord> errorRecords = new ConcurrentHashMap<>();

    /**
     * 最大保留错误数量
     */
    private static final int MAX_ERROR_RECORDS = 1000;

    /**
     * 错误去重相似度阈值
     */
    private static final double SIMILARITY_THRESHOLD = 0.8;

    /**
     * 记录错误
     */
    public void recordError(String errorType, String errorMessage, String stackTrace) {
        String errorKey = generateErrorKey(errorType, errorMessage);

        ErrorRecord record = errorRecords.computeIfAbsent(errorKey,
                k -> new ErrorRecord(errorType, errorMessage));

        record.incrementCount();
        record.setLastSeen(Instant.now().toEpochMilli());
        record.addStackTrace(stackTrace);

        // 限制每个错误的堆栈数量
        if (record.getStackTraces().size() > 10) {
            record.getStackTraces().remove(0);
        }

        // 限制总记录数
        if (errorRecords.size() > MAX_ERROR_RECORDS) {
            cleanupOldRecords();
        }

        // 每 10 次错误输出一次聚合日志
        if (record.getCount() % 10 == 0) {
            log.warn("错误聚合: [{}] 出现 {} 次, 最近: {}", errorType, record.getCount(),
                    new Date(record.getLastSeen()));
        }
    }

    /**
     * 生成错误键（用于去重）
     */
    private String generateErrorKey(String errorType, String errorMessage) {
        // 简单去重：错误类型 + 错误消息的哈希
        String key = errorType + ":" + errorMessage.hashCode();
        return key;
    }

    /**
     * 清理旧记录
     */
    private void cleanupOldRecords() {
        long now = Instant.now().toEpochMilli();
        long oneDayAgo = now - 24 * 60 * 60 * 1000;

        errorRecords.entrySet().removeIf(entry ->
                entry.getValue().getLastSeen() < oneDayAgo);
    }

    /**
     * 获取错误统计
     */
    public ErrorStats getErrorStats() {
        ErrorStats stats = new ErrorStats();
        stats.setTotalErrors(errorRecords.size());

        List<ErrorRecord> sortedRecords = new ArrayList<>(errorRecords.values());
        sortedRecords.sort(Comparator.comparing(ErrorRecord::getCount).reversed());

        stats.setTopErrors(sortedRecords.subList(0, Math.min(5, sortedRecords.size())));
        stats.setLast24HourCount(calculateLast24HourCount());
        return stats;
    }

    /**
     * 计算最近 24 小时的错误数
     */
    private int calculateLast24HourCount() {
        long now = Instant.now().toEpochMilli();
        long oneDayAgo = now - 24 * 60 * 60 * 1000;

        return (int) errorRecords.values().stream()
                .filter(record -> record.getLastSeen() > oneDayAgo)
                .count();
    }

    /**
     * 清空错误记录
     */
    public void clear() {
        errorRecords.clear();
        log.info("错误记录已清空");
    }

    // ==================== 内部类 ====================

    @Data
    public static class ErrorRecord {
        private String errorType;
        private String errorMessage;
        private int count = 0;
        private long firstSeen;
        private long lastSeen;
        private List<String> stackTraces = new ArrayList<>();

        public ErrorRecord(String errorType, String errorMessage) {
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.firstSeen = Instant.now().toEpochMilli();
            this.lastSeen = this.firstSeen;
        }

        public void incrementCount() {
            this.count++;
            this.lastSeen = Instant.now().toEpochMilli();
        }

        public void addStackTrace(String stackTrace) {
            if (stackTrace != null && !stackTrace.trim().isEmpty()) {
                stackTraces.add(stackTrace);
            }
        }
    }

    @Data
    public static class ErrorStats {
        private int totalErrors;
        private int last24HourCount;
        private List<ErrorRecord> topErrors = new ArrayList<>();
    }
}
