package com.example.aiagent.opagent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 运维 Agent - 健康检查器
 */
@Slf4j
@Component
public class HealthChecker {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final DataSource dataSource;

    public HealthChecker(VectorStore vectorStore,
                         JdbcTemplate jdbcTemplate,
                         StringRedisTemplate redisTemplate,
                         DataSource dataSource) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.dataSource = dataSource;
    }

    /**
     * 执行全面健康检查
     */
    public HealthReport checkAll() {
        HealthReport report = new HealthReport();
        report.setTimestamp(System.currentTimeMillis());

        // 检查各组件
        checkVectorStore(report);
        checkDatabase(report);
        checkRedis(report);

        // 计算总体状态
        int failedCount = (int) report.getChecks().values().stream()
                .filter(status -> !status.equals("healthy"))
                .count();

        if (failedCount == 0) {
            report.setStatus("healthy");
            report.setMessage("所有组件正常");
        } else {
            report.setStatus("unhealthy");
            report.setMessage(failedCount + " 个组件异常");
        }

        log.info("健康检查完成: {}", report.getStatus());
        return report;
    }

    /**
     * 检查向量库
     */
    private void checkVectorStore(HealthReport report) {
        try {
            // 尝试执行一个简单的搜索
            vectorStore.similaritySearch("test");
            report.getChecks().put("vector_store", "healthy");
            report.getDetails().put("vector_store", "向量库连接正常");
        } catch (Exception e) {
            report.getChecks().put("vector_store", "unhealthy");
            report.getDetails().put("vector_store", "向量库异常: " + e.getMessage());
            log.error("向量库检查失败", e);
        }
    }

    /**
     * 检查数据库
     */
    private void checkDatabase(HealthReport report) {
        try {
            try (Connection conn = dataSource.getConnection()) {
                conn.isValid(3);
                // 检查 vector_store 表是否存在
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vector_store", Integer.class);
                report.getChecks().put("database", "healthy");
                report.getDetails().put("database", "数据库连接正常, 向量记录数: " + count);
            }
        } catch (Exception e) {
            report.getChecks().put("database", "unhealthy");
            report.getDetails().put("database", "数据库异常: " + e.getMessage());
            log.error("数据库检查失败", e);
        }
    }

    /**
     * 检查 Redis
     */
    private void checkRedis(HealthReport report) {
        try {
            String pong = redisTemplate.opsForValue().get("health:check");
            // 尝试设置一个值
            redisTemplate.opsForValue().set("health:check", "pong", 10);
            report.getChecks().put("redis", "healthy");
            report.getDetails().put("redis", "Redis 连接正常");
        } catch (Exception e) {
            report.getChecks().put("redis", "unhealthy");
            report.getDetails().put("redis", "Redis 异常: " + e.getMessage());
            log.error("Redis 检查失败", e);
        }
    }

    /**
     * 健康检查报告
     */
    @Data
    public static class HealthReport {
        private String status; // healthy, unhealthy, degraded
        private String message;
        private long timestamp;
        private Map<String, String> checks = new HashMap<>();
        private Map<String, String> details = new HashMap<>();
    }
}
