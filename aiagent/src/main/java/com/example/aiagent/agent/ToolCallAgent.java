package com.example.aiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

import com.example.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 获取AI的文本回答
            String assistantText = assistantMessage.getText();
            // 如果回答过长，截断到合理长度（避免过度详细的解释）
            if (assistantText != null && assistantText.length() > 1000) {
                assistantText = assistantText.substring(0, 1000) + "...";
            }
            this.lastThinkingResponse = assistantText;
            
            log.info(getName() + " 的思考与分析：" + assistantText);
            log.info(getName() + " 选择了 " + toolCallList.size() + " 个工具来使用");
            
            if (!toolCallList.isEmpty()) {
                String toolCallInfo = toolCallList.stream()
                        .map(toolCall -> String.format("  • %s: %s", toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info("📋 即将执行的工具：\n" + toolCallInfo);
            }
            
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                // 立即标记任务完成，不要继续循环
                setState(AgentState.FINISHED);
                // 返回 false 表示该步骤已完成，不需要执行工具
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + " 的思考过程遇到了问题：" + e.getMessage(), e);
            getMessageList().add(new AssistantMessage("❌ 处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态
            setState(AgentState.FINISHED);
        }
        
        // 生成格式化的结果
        StringBuilder formattedResult = new StringBuilder();
        formattedResult.append("✅ 任务执行完成，以下是详细结果：\n\n");
        
        toolResponseMessage.getResponses().forEach(response -> {
            String toolName = response.name();
            String responseData = response.responseData();
            
            // 根据不同的工具类型格式化输出
            formattedResult.append("◆ ").append(formatToolName(toolName)).append("\n");
            
            // 处理过长的响应内容
            if (responseData != null && responseData.length() > 200) {
                formattedResult.append("   📌 ").append(responseData.substring(0, 200)).append("...\n");
            } else if (responseData != null) {
                formattedResult.append("   📌 ").append(responseData).append("\n");
            }
            formattedResult.append("\n");
        });
        
        String results = formattedResult.toString();
        log.info(results);
        return results;
    }
    
    /**
     * 格式化工具名称为用户友好的格式
     *
     * @param toolName 工具名称
     * @return 格式化后的名称
     */
    private String formatToolName(String toolName) {
        return switch (toolName) {
            case "generatePDF" -> "📄 PDF 生成";
            case "writeFile" -> "💾 文件写入";
            case "readFile" -> "📖 文件读取";
            case "webSearch" -> "🔍 网页搜索";
            case "webScraping" -> "🕷️ 网页爬取";
            case "executeTerminal" -> "⚙️ 命令执行";
            case "download" -> "⬇️ 资源下载";
            case "doTerminate" -> "🛑 任务终止";
            default -> toolName;
        };
    }
}
