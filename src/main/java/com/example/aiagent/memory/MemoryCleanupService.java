package com.example.aiagent.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 记忆清理服务
 * 定期清理低价值的长期记忆，防止数据库无限增长
 */
@Slf4j
@Service
public class MemoryCleanupService {

    private final VectorStore vectorStore;

    @Value("${memory.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${memory.cleanup.dry-run:false}")
    private boolean dryRun;

    public MemoryCleanupService(@Qualifier("hybridVectorStoreAdapter") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 定期清理任务
     * 每天凌晨 2 点执行
     */
    @Scheduled(cron = "${memory.cleanup.cron:0 0 2 * * ?}")
    public void scheduledCleanup() {
        if (!cleanupEnabled) {
            log.debug("记忆清理已禁用，跳过");
            return;
        }

        log.info("========== 开始记忆清理任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            int totalDeleted = 0;

            // 规则1：低重要性 + 长时间未访问
            totalDeleted += cleanupByRule(0.3, 30, "低重要性30天未访问");
            totalDeleted += cleanupByRule(0.5, 90, "中重要性90天未访问");
            totalDeleted += cleanupByRule(0.7, 180, "高重要性180天未访问");

            // 规则2：所有记忆超过1年未访问
            totalDeleted += cleanupByAge(365, "所有记忆1年未访问");

            long duration = System.currentTimeMillis() - startTime;
            log.info("========== 记忆清理完成 ==========");
            log.info("总计删除: {} 条记忆", totalDeleted);
            log.info("耗时: {} ms", duration);

        } catch (Exception e) {
            log.error("记忆清理任务失败", e);
        }
    }

    /**
     * 按规则清理
     *
     * @param maxImportance 最大重要性阈值
     * @param daysInactive  未访问天数
     * @param ruleName      规则名称
     * @return 删除数量
     */
    private int cleanupByRule(double maxImportance, int daysInactive, String ruleName) {
        try {
            Instant cutoffTime = Instant.now().minus(daysInactive, ChronoUnit.DAYS);

            log.info("执行清理规则: {} (importance < {}, inactive > {} days)",
                    ruleName, maxImportance, daysInactive);

            // 注意：当前 Spring AI VectorStore 不支持按 metadata 条件删除
            // 这里提供伪代码，实际需要根据你的 VectorStore 实现调整

            // 方案1：如果 VectorStore 支持 filter 查询
            // List<Document> toDelete = vectorStore.similaritySearch(
            //     SearchRequest.builder()
            //         .filter(Filter.builder()
            //             .expression(Expression.and(
            //                 Expression.lt("importance", maxImportance),
            //                 Expression.lt("last_access", cutoffTime.toString())
            //             ))
            //             .build())
            //         .build()
            // );

            // 方案2：如果不支持，需要全量扫描（性能差，不推荐）
            // 这里只是示例，实际使用需要优化

            if (dryRun) {
                log.info("[DRY RUN] 规则 {} 将删除 X 条记忆（实际未删除）", ruleName);
                return 0;
            }

            // vectorStore.delete(toDelete.stream().map(Document::getId).toList());
            // int deleted = toDelete.size();
            int deleted = 0; // 占位符

            log.info("规则 {} 删除了 {} 条记忆", ruleName, deleted);
            return deleted;

        } catch (Exception e) {
            log.error("清理规则 {} 执行失败", ruleName, e);
            return 0;
        }
    }

    /**
     * 按年龄清理
     *
     * @param daysOld  记忆年龄（天）
     * @param ruleName 规则名称
     * @return 删除数量
     */
    private int cleanupByAge(int daysOld, String ruleName) {
        try {
            Instant cutoffTime = Instant.now().minus(daysOld, ChronoUnit.DAYS);

            log.info("执行清理规则: {} (created_at < {} days ago)", ruleName, daysOld);

            if (dryRun) {
                log.info("[DRY RUN] 规则 {} 将删除 X 条记忆（实际未删除）", ruleName);
                return 0;
            }

            // 实际删除逻辑（需要根据 VectorStore 实现）
            int deleted = 0;

            log.info("规则 {} 删除了 {} 条记忆", ruleName, deleted);
            return deleted;

        } catch (Exception e) {
            log.error("清理规则 {} 执行失败", ruleName, e);
            return 0;
        }
    }

    /**
     * 手动触发清理（用于测试）
     */
    public void manualCleanup() {
        log.info("手动触发记忆清理");
        scheduledCleanup();
    }
}
