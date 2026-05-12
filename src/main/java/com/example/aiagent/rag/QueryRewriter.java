package com.example.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class QueryRewriter {

    /**
     * 基础 Query Rewrite
     */
    private final QueryTransformer queryTransformer;

    /**
     * 用于 Multi Query / HyDE
     */
    private final ChatClient chatClient;

    public QueryRewriter(ChatModel dashscopeChatModel) {

        ChatClient.Builder builder =
                ChatClient.builder(dashscopeChatModel);

        this.queryTransformer =
                RewriteQueryTransformer.builder()
                        .chatClientBuilder(builder)
                        .build();

        this.chatClient = builder.build();

        log.info("QueryRewriter 初始化完成");
    }

    /**
     * 基础 Query Rewrite
     */
    public String doQueryRewrite(String prompt) {

        if (prompt == null || prompt.trim().isEmpty()) {
            return "";
        }

        try {

            Query transformedQuery =
                    queryTransformer.transform(new Query(prompt));

            String rewritten = transformedQuery.text();

            log.info("Query Rewrite: [{}] -> [{}]",
                    prompt, rewritten);

            return rewritten;

        } catch (Exception e) {

            log.error("查询重写失败", e);

            return prompt;
        }
    }

    /**
     * Multi Query Rewrite
     * 一个问题生成多个检索Query
     */
    public List<String> generateMultiQueries(String prompt) {

        List<String> result = new ArrayList<>();

        try {

            String rewritePrompt = """
                    你是RAG检索优化助手。

                    请基于用户问题，
                    生成5个适合知识库检索的不同Query。

                    要求：
                    1. 保持语义相关
                    2. 尽量覆盖不同方向
                    3. 每行一个Query
                    4. 不要编号

                    用户问题：
                    %s
                    """.formatted(prompt);

            String response = chatClient.prompt()
                    .user(rewritePrompt)
                    .call()
                    .content();

            result = Arrays.stream(response.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();

            log.info("Multi Query Rewrite结果: {}", result);

        } catch (Exception e) {

            log.error("Multi Query Rewrite失败", e);

            result = List.of(prompt);
        }

        return result;
    }

    /**
     * HyDE
     * 生成假设性答案用于Embedding检索
     */
    public String generateHyDEDocument(String prompt) {

        try {

            String hydePrompt = """
                    你是技术知识库助手。

                    请根据用户问题，
                    生成一段专业、完整、
                    适合知识库检索的技术回答。

                    回答尽量包含：
                    - 专业术语
                    - 实现流程
                    - 核心技术点

                    用户问题：
                    %s
                    """.formatted(prompt);

            String response = chatClient.prompt()
                    .user(hydePrompt)
                    .call()
                    .content();

            log.info("HyDE生成完成");

            return response;

        } catch (Exception e) {

            log.error("HyDE生成失败", e);

            return prompt;
        }
    }
}