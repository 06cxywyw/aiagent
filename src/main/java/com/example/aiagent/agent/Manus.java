package com.example.aiagent.agent;


import com.example.aiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class Manus extends ToolCallAgent {

    public Manus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("manus");
        String SYSTEM_PROMPT = """
                你是Manus，一个简洁高效的AI助手。
                
                核心原则：
                1. 回答必须简洁清晰，一句话能说清的千万不要用两句
                2. 不要解释你的决策过程、不要啰嗦、不要过度论证
                3. 用户问候时直接问候，知识问题直接回答，不要冗长讨论
                4. 当需要调用工具时，立即执行，本步立即结束
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                用户的请求需要工具吗？
                • 是 → 立即调用最合适的工具，然后立即结束
                • 否 → 给出简洁的直接回答，然后立即结束
                一句话足够，不要冗长解释。
                
                
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
