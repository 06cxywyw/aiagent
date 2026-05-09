package com.example.aiagent.controller;

import com.example.aiagent.opagent.ErrorLogAnalyzer;
import com.example.aiagent.opagent.HealthChecker;
import com.example.aiagent.opagent.OpsAgent;
import com.example.aiagent.opagent.RagMonitor;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Resource
    private HealthChecker healthChecker;

    @Resource
    private RagMonitor ragMonitor;

    @Resource
    private ErrorLogAnalyzer errorLogAnalyzer;

    @Resource
    private OpsAgent opsAgent;

    /**
     * 基础健康检查
     */
    @GetMapping
    public String healthCheck() {
        return "ok";
    }

    /**
     * 详细健康状态
     */
    @GetMapping("/detail")
    public HealthChecker.HealthReport healthDetail() {
        return healthChecker.checkAll();
    }

    /**
     * RAG 系统监控指标
     */
    @GetMapping("/rag-metrics")
    public RagMonitor.RagMetrics ragMetrics() {
        return ragMonitor.getMetrics();
    }

    /**
     * 错误统计
     */
    @GetMapping("/error-stats")
    public ErrorLogAnalyzer.ErrorStats errorStats() {
        return errorLogAnalyzer.getErrorStats();
    }

    /**
     * 运维摘要
     */
    @GetMapping("/ops-summary")
    public OpsAgent.OpsSummary opsSummary() {
        return opsAgent.getSummary();
    }

    /**
     * 手动触发健康检查
     */
    @PostMapping("/manual-check")
    public HealthChecker.HealthReport manualCheck() {
        return opsAgent.manualCheck();
    }

    /**
     * 清空错误记录
     */
    @PostMapping("/clear-errors")
    public String clearErrors() {
        opsAgent.clearErrorRecords();
        return "ok";
    }
}

