package com.example.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.example.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文件操作工具类（已加固安全限制）
 * 提供文件读写功能，但限制在安全目录内
 */
@Component
public class FileOperationTool {

    // 允许访问的根目录
    private static final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    // 允许的文件扩展名白名单
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".txt", ".md", ".log", ".json", ".xml", ".csv", ".yml", ".yaml",
            ".java", ".py", ".js", ".ts", ".html", ".css", ".sql"
    ));

    // 单文件最大大小（字节）- 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 文件名长度限制
    private static final int MAX_FILENAME_LENGTH = 100;

    @Tool(description = "Read content from a file (受限模式)")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName) {
        // 1. 基础校验
        if (fileName == null || fileName.trim().isEmpty()) {
            return "❌ 读取失败：文件名不能为空";
        }

        // 2. 文件名长度校验
        if (fileName.length() > MAX_FILENAME_LENGTH) {
            return "❌ 读取失败：文件名过长";
        }

        // 3. 路径安全校验
        if (containsPathTraversal(fileName)) {
            return "❌ 读取失败：不允许路径遍历";
        }

        // 4. 扩展名校验
        if (!isAllowedExtension(fileName)) {
            return "❌ 读取失败：不允许的文件类型。允许的类型: " + String.join(", ", ALLOWED_EXTENSIONS);
        }

        // 5. 构建安全路径
        File file = new File(FILE_DIR, fileName);

        try {
            // 6. 绝对路径校验（防止符号链接攻击）
            String canonicalPath = file.getCanonicalPath();
            String allowedPath = new File(FILE_DIR).getCanonicalPath();

            if (!canonicalPath.startsWith(allowedPath)) {
                return "❌ 读取失败：不允许访问该路径";
            }

            // 7. 文件存在性检查
            if (!file.exists()) {
                return "❌ 读取失败：文件不存在";
            }

            // 8. 文件大小检查
            if (file.length() > MAX_FILE_SIZE) {
                return "❌ 读取失败：文件过大（超过10MB）";
            }

            // 9. 读取文件
            return FileUtil.readUtf8String(file);

        } catch (Exception e) {
            return "❌ 读取失败: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file (受限模式)")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content
    ) {
        // 1. 基础校验
        if (fileName == null || fileName.trim().isEmpty()) {
            return "❌ 写入失败：文件名不能为空";
        }

        if (content == null) {
            return "❌ 写入失败：内容不能为空";
        }

        // 2. 文件名长度校验
        if (fileName.length() > MAX_FILENAME_LENGTH) {
            return "❌ 写入失败：文件名过长";
        }

        // 3. 路径安全校验
        if (containsPathTraversal(fileName)) {
            return "❌ 写入失败：不允许路径遍历";
        }

        // 4. 扩展名校验
        if (!isAllowedExtension(fileName)) {
            return "❌ 写入失败：不允许的文件类型。允许的类型: " + String.join(", ", ALLOWED_EXTENSIONS);
        }

        // 5. 内容大小检查
        if (content.length() > 1000000) {  // 1MB 文本限制
            return "❌ 写入失败：内容过大（超过1MB）";
        }

        // 6. 构建安全路径
        File file = new File(FILE_DIR, fileName);

        try {
            // 7. 绝对路径校验
            String canonicalPath = file.getCanonicalPath();
            String allowedPath = new File(FILE_DIR).getCanonicalPath();

            if (!canonicalPath.startsWith(allowedPath)) {
                return "❌ 写入失败：不允许访问该路径";
            }

            // 8. 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                FileUtil.mkdir(parentDir);
            }

            // 9. 写入文件
            FileUtil.writeUtf8String(content, file);

            return "✅ 文件写入成功: " + canonicalPath;

        } catch (Exception e) {
            return "❌ 写入失败: " + e.getMessage();
        }
    }

    /**
     * 检查是否包含路径遍历模式
     */
    private boolean containsPathTraversal(String fileName) {
        // 检查 .. 路径遍历
        if (fileName.contains("..")) {
            return true;
        }

        // 检查绝对路径尝试
        if (fileName.startsWith("/") || fileName.startsWith("\\")) {
            return true;
        }

        // 检查 Windows 驱动器字母
        if (fileName.length() >= 2 && fileName.charAt(1) == ':') {
            return true;
        }

        return false;
    }

    /**
     * 检查文件扩展名是否在白名单中
     */
    private boolean isAllowedExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
            // 没有扩展名，拒绝
            return false;
        }

        String extension = fileName.substring(lastDot).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
}
