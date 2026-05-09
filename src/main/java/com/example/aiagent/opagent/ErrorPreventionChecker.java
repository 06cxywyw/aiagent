package com.example.aiagent.opagent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 错误预防检查器
 * 在执行前检查可能的错误，提前规避
 */
@Slf4j
@Component
public class ErrorPreventionChecker {

    /**
     * 检查用户输入
     */
    public InputCheckResult checkInput(String input) {
        InputCheckResult result = new InputCheckResult();
        result.setInput(input);

        if (input == null || input.trim().isEmpty()) {
            result.setValid(false);
            result.setReason("输入为空");
            return result;
        }

        // 检查长度
        if (input.length() > 10000) {
            result.setValid(false);
            result.setReason("输入过长（超过10000字符）");
            return result;
        }

        // 检查特殊字符
        if (containsDangerousPattern(input)) {
            result.setValid(false);
            result.setReason("包含危险字符或命令");
            return result;
        }

        result.setValid(true);
        return result;
    }

    /**
     * 检查工具调用参数
     */
    public ToolCheckResult checkToolCall(String toolName, Map<String, Object> params) {
        ToolCheckResult result = new ToolCheckResult();
        result.setToolName(toolName);

        if (params == null || params.isEmpty()) {
            result.setValid(true); // 有些工具可能不需要参数
            return result;
        }

        // 检查参数值
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();

            if (paramValue == null) {
                result.getWarnings().add("参数 " + paramName + " 为空");
            } else if (paramValue instanceof String) {
                String strValue = (String) paramValue;
                if (strValue.length() > 5000) {
                    result.getWarnings().add("参数 " + paramName + " 过长");
                }
                if (containsDangerousPattern(strValue)) {
                    result.getWarnings().add("参数 " + paramName + " 包含危险内容");
                }
            }
        }

        result.setValid(result.getWarnings().isEmpty());
        return result;
    }

    /**
     * 检查危险模式
     */
    private boolean containsDangerousPattern(String input) {
        // SQL 注入
        Pattern sqlPattern = Pattern.compile(
                "(select|insert|update|delete|drop|union|exec|execute|xp_|sp_|;--)",
                Pattern.CASE_INSENSITIVE
        );
        if (sqlPattern.matcher(input).find()) {
            return true;
        }

        // 命令注入
        Pattern cmdPattern = Pattern.compile(
                "(rm -rf|/etc/passwd|/etc/shadow|eval\\(|exec\\(|system\\()",
                Pattern.CASE_INSENSITIVE
        );
        if (cmdPattern.matcher(input).find()) {
            return true;
        }

        // XSS 攻击
        Pattern xssPattern = Pattern.compile(
                "<script|javascript:|onerror=|onload=|onclick=",
                Pattern.CASE_INSENSITIVE
        );
        if (xssPattern.matcher(input).find()) {
            return true;
        }

        return false;
    }

    // ==================== 内部类 ====================

    @Data
    public static class InputCheckResult {
        private String input;
        private boolean valid;
        private String reason;
        private List<String> warnings = new ArrayList<>();
    }

    @Data
    public static class ToolCheckResult {
        private String toolName;
        private boolean valid;
        private List<String> warnings = new ArrayList<>();
    }
}
