package com.example.aiagent.opagent;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 错误总结器
 * 自动分析错误原因，提取关键信息，避免再次犯错
 */
@Slf4j
@Component
public class ErrorSummarizer {

    private final EmbeddingModel embeddingModel;

    public ErrorSummarizer(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 总结错误
     */
    public ErrorSummary summarize(String errorType, String errorMessage, String stackTrace) {
        ErrorSummary summary = new ErrorSummary();
        summary.setErrorType(errorType);
        summary.setErrorMessage(errorMessage);
        summary.setTimestamp(Instant.now());
        summary.setCreateTime(Instant.now());

        // 分析错误原因
        analyzeError(summary, errorMessage, stackTrace);

        // 提取解决方案
        extractSolution(summary, errorMessage, stackTrace);

        // 生成唯一标识
        summary.setErrorCode(generateErrorCode(summary));

        return summary;
    }

    /**
     * 分析错误原因
     */
    private void analyzeError(ErrorSummary summary, String errorMessage, String stackTrace) {
        List<String> causes = new ArrayList<>();

        // 1. 检查网络相关错误
        if (containsAny(errorMessage, stackTrace, "timeout", "connection refused", "network", "socket")) {
            causes.add("网络连接问题");
            summary.setCategory("network");
        }

        // 2. 检查参数错误
        if (containsAny(errorMessage, stackTrace, "invalid", "parameter", "argument", "null pointer", "NPE")) {
            causes.add("参数或空指针错误");
            summary.setCategory("parameter");
        }

        // 3. 检查资源不存在
        if (containsAny(errorMessage, stackTrace, "not found", "missing", "does not exist", "404")) {
            causes.add("资源不存在");
            summary.setCategory("resource");
        }

        // 4. 检查权限问题
        if (containsAny(errorMessage, stackTrace, "permission", "access denied", "unauthorized", "403")) {
            causes.add("权限问题");
            summary.setCategory("permission");
        }

        // 5. 检查配置错误
        if (containsAny(errorMessage, stackTrace, "configuration", "config", "property", "environment")) {
            causes.add("配置错误");
            summary.setCategory("configuration");
        }

        // 6. 检查服务异常
        if (containsAny(errorMessage, stackTrace, "service", "server", "500", "internal error")) {
            causes.add("服务端异常");
            summary.setCategory("service");
        }

        // 7. 检查API调用错误
        if (containsAny(errorMessage, stackTrace, "api", "dashscope", "token", "quota", "limit")) {
            causes.add("API调用错误");
            summary.setCategory("api");
        }

        // 默认分类
        if (causes.isEmpty()) {
            causes.add("未知原因");
            summary.setCategory("unknown");
        }

        summary.setCauses(causes);
    }

    /**
     * 提取解决方案
     */
    private void extractSolution(ErrorSummary summary, String errorMessage, String stackTrace) {
        List<String> solutions = new ArrayList<>();

        String errorLower = (errorMessage + " " + stackTrace).toLowerCase();

        // 1. 网络问题解决方案
        if (summary.getCategory().equals("network")) {
            solutions.add("检查网络连接是否正常");
            solutions.add("确认目标服务是否可访问");
            solutions.add("增加超时时间或重试机制");
        }

        // 2. 参数问题解决方案
        if (summary.getCategory().equals("parameter")) {
            solutions.add("检查参数是否为空或无效");
            solutions.add("确认参数类型和格式是否正确");
            solutions.add("添加参数校验逻辑");
        }

        // 3. 资源问题解决方案
        if (summary.getCategory().equals("resource")) {
            solutions.add("确认资源是否存在");
            solutions.add("检查资源ID或路径是否正确");
            solutions.add("添加资源存在性检查");
        }

        // 4. 权限问题解决方案
        if (summary.getCategory().equals("permission")) {
            solutions.add("检查访问凭证是否有效");
            solutions.add("确认用户权限是否足够");
            solutions.add("更新或刷新访问令牌");
        }

        // 5. 配置问题解决方案
        if (summary.getCategory().equals("configuration")) {
            solutions.add("检查配置文件是否正确");
            solutions.add("确认环境变量是否设置");
            solutions.add("验证配置项的格式和值");
        }

        // 6. API问题解决方案
        if (summary.getCategory().equals("api")) {
            solutions.add("检查API密钥是否有效");
            solutions.add("确认配额是否充足");
            solutions.add("查看API文档确认调用方式");
        }

        // 默认解决方案
        if (solutions.isEmpty()) {
            solutions.add("查看详细日志定位问题");
            solutions.add("检查相关依赖是否正常");
            solutions.add("联系技术支持");
        }

        summary.setSolutions(solutions);
    }

    /**
     * 生成错误代码
     */
    private String generateErrorCode(ErrorSummary summary) {
        String timestamp = Instant.now().toString().substring(0, 10); // YYYY-MM-DD
        String categoryCode = getCategoryCode(summary.getCategory());
        String hash = String.valueOf(summary.getErrorMessage().hashCode()).substring(0, 4);
        return String.format("ERR-%s-%s-%s", timestamp, categoryCode, Math.abs(hash));
    }

    /**
     * 获取分类代码
     */
    private String getCategoryCode(String category) {
        Map<String, String> codes = new HashMap<>();
        codes.put("network", "NET");
        codes.put("parameter", "PAR");
        codes.put("resource", "RES");
        codes.put("permission", "PER");
        codes.put("configuration", "CFG");
        codes.put("service", "SVC");
        codes.put("api", "API");
        codes.put("unknown", "UNK");
        return codes.getOrDefault(category, "UNK");
    }

    /**
     * 检查是否包含任意关键词
     */
    private boolean containsAny(String msg, String stack, String... keywords) {
        String combined = (msg + " " + stack).toLowerCase();
        for (String keyword : keywords) {
            if (combined.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // ==================== 内部类 ====================

    @Data
    public static class ErrorSummary {
        private String errorCode;
        private String errorType;
        private String errorMessage;
        private String category; // network, parameter, resource, permission, configuration, service, api
        private List<String> causes;
        private List<String> solutions;
        private Instant timestamp;
        private Instant createTime;
        private int count = 1; // 错误出现次数
    }
}
