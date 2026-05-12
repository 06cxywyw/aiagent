package com.example.aiagent.tools;

import com.example.aiagent.config.SecurityProperties;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 *
 * 改进：
 * - API Key 从 SecurityProperties 读取（支持环境变量）
 */
@Configuration
public class ToolRegistration {

    private final SecurityProperties securityProperties;

    public ToolRegistration(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();

        // 从 SecurityProperties 获取 API Key（支持环境变量）
        String searchApiKey = securityProperties.getApiKeys().getSearchApiKey();
        if (searchApiKey == null || searchApiKey.isEmpty()) {
            throw new IllegalStateException("SEARCH_API_KEY 未配置，请设置环境变量或配置文件");
        }
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);

        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        RagTool ragTool = new RagTool();

        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool,
                ragTool
        );
    }
}
