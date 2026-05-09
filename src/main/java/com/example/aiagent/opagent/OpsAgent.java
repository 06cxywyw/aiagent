package com.example.aiagent.opagent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 运维 Agent - 对外暴露的运维入口
 * 提供健康检查、错误分析、系统状态等接口
 */
@Slf4j
@Component
public class OpsAgent {

    private final HealthChecker healthChecker;
    private final RagMonitor ragMonitor;
    private final ErrorLogAnalyzer errorLogAnalyzer;

    @Autowired
    public OpsAgent(HealthChecker healthChecker,
                    RagMonitor ragMonitor,
                    ErrorLogAnalyzer errorLogAnalyzer) {
        this.healthChecker = healthChecker;
        this.ragMonitor = ragMonitor;
        this.errorLogAnalyzer = errorLogAnalyzer;
    }

    /**
     * 获取系统健康状态
     */
    public HealthChecker.HealthReport getHealthStatus() {
        return healthChecker.checkAll();
    }

    /**
     * 获取 RAG 系统监控指标
     */
    public RagMonitor.RagMetrics getRagMetrics() {
        return ragMonitor.getMetrics();
    }

    /**
     * 获取错误统计
     */
    public ErrorLogAnalyzer.ErrorStats getErrorStats() {
        return errorLogAnalyzer.getErrorStats();
    }

    /**
     * 获取详细错误信息
     */
    public List<ErrorLogAnalyzer.ErrorRecord> getErrorDetails() {
        return errorLogAnalyzer.getErrorStats().getTopErrors();
    }

    /**
     * 手动触发健康检查
     */
    public HealthChecker.HealthReport manualCheck() {
        log.info("手动触发健康检查...");
        return healthChecker.checkAll();
    }

    /**
     * 清空错误记录
     */
    public void clearErrorRecords() {
        errorLogAnalyzer.clear();
        log.info("错误记录已清空");
    }

    /**
     * 获取运维摘要
     */
    public OpsSummary getSummary() {
        OpsSummary summary = new OpsSummary();
        summary.setHealthStatus(healthChecker.checkAll());
        summary.setRagMetrics(ragMonitor.getMetrics());
        summary.setErrorStats(errorLogAnalyzer.getErrorStats());
        return summary;
    }

    // ==================== 内部类 ====================

    @Data
    public static class OpsSummary {
        private HealthChecker.HealthReport healthStatus;
        private RagMonitor.RagMetrics ragMetrics;
        private ErrorLogAnalyzer.ErrorStats errorStats;
    }
}
