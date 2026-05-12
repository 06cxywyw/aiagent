package com.example.aiagent.tools;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 终端操作工具（已加固安全限制）
 *
 * 安全改进：
 * 1. 命令白名单：只允许预定义的安全命令
 * 2. 参数校验：禁止危险参数（如路径遍历、特殊字符）
 * 3. 执行超时：防止命令长时间挂起
 * 4. 输出限制：限制输出长度，防止信息泄露
 */
@Component
public class TerminalOperationTool {

    // 允许执行的命令白名单
    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
            "dir", "cd", "echo", "type", "more", "find", "findstr",
            "ipconfig", "ping", "tracert", "nslookup", "hostname",
            "whoami", "systeminfo", "ver", "date", "time",
            "tree", "calc", "notepad", "cls", "help"
    ));

    // 危险命令关键词（额外防护）
    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "rm -rf", "del /f", "format", "mkfs", "chmod", "chown",
            "eval", "exec", "system", "subprocess", "popen", "shell",
            "/etc/passwd", "/etc/shadow", "cmd.exe", "powershell",
            "base64 -d", "xxd", "od -c"
    ));

    // 单条命令输出长度限制（字符数）
    private static final int MAX_OUTPUT_LENGTH = 2000;

    // 命令执行超时时间（毫秒）
    private static final long TIMEOUT_MS = 10000;

    @Tool(description = "Execute a command in the terminal (受限模式)")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        // 1. 基础校验
        if (StrUtil.isBlank(command)) {
            return "❌ 执行失败：命令不能为空";
        }

        // 2. 长度校验
        if (command.length() > 500) {
            return "❌ 执行失败：命令过长（超过500字符）";
        }

        // 3. 危险关键词检查
        if (containsDangerousKeyword(command)) {
            return "❌ 执行失败：命令包含危险内容";
        }

        // 4. 命令白名单检查
        String baseCommand = extractBaseCommand(command);
        if (!ALLOWED_COMMANDS.contains(baseCommand)) {
            return "❌ 执行失败：命令不在白名单中。允许的命令: " + String.join(", ", ALLOWED_COMMANDS);
        }

        // 5. 执行命令
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // 使用带超时的等待
            boolean finished = process.waitFor(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "❌ 执行失败：命令超时（超过" + TIMEOUT_MS + "ms）";
            }

            // 读取输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return "❌ 命令执行失败（退出码: " + exitCode + "）\n输出: " + truncateOutput(output.toString());
            }

            return "✅ 执行成功\n输出:\n" + truncateOutput(output.toString());

        } catch (IOException e) {
            return "❌ 执行失败：IO 异常 - " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "❌ 执行失败：命令被中断";
        }
    }

    /**
     * 提取基础命令（第一个单词）
     */
    private String extractBaseCommand(String command) {
        String trimmed = command.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace > 0) {
            return trimmed.substring(0, firstSpace).toLowerCase();
        }
        return trimmed.toLowerCase();
    }

    /**
     * 检查是否包含危险关键词
     */
    private boolean containsDangerousKeyword(String command) {
        String lowerCommand = command.toLowerCase();

        // 检查危险关键词
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (lowerCommand.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // 检查路径遍历
        if (command.contains("..") && (command.contains("/") || command.contains("\\"))) {
            return true;
        }

        // 检查特殊字符
        if (command.contains(";") || command.contains("|") || command.contains("&")) {
            // 允许某些安全场景（如 dir /p），但需要严格检查
            // 这里简化处理，直接拒绝包含这些字符的命令
            return true;
        }

        return false;
    }

    /**
     * 截断输出
     */
    private String truncateOutput(String output) {
        if (output.length() <= MAX_OUTPUT_LENGTH) {
            return output;
        }
        return output.substring(0, MAX_OUTPUT_LENGTH) + "\n... (输出已截断，共 " + MAX_OUTPUT_LENGTH + " 字符)";
    }
}
