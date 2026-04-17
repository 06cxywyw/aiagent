package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
@Service
@Slf4j
public class RagService {

    @Resource
    private HybridRetriever hybridRetriever;

    /**
     * 🔥 RAG 核心入口（统一）
     */
    public String buildContextPrompt(String query) {

        List<Document> docs = hybridRetriever.retrieve(query);

        return buildPrompt(query, buildContext(docs));
    }

    /**
     * Context 构建
     */
    private String buildContext(List<Document> docs) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < Math.min(docs.size(), 5); i++) {

            Document doc = docs.get(i);
            Map<String, Object> meta = doc.getMetadata();

            sb.append("【文档 ").append(i + 1).append("】\n")
                    .append("标题：").append(meta.getOrDefault("title", "未知")).append("\n")
                    .append("分类：").append(meta.getOrDefault("category", "未知")).append("\n")
                    .append("关键词：").append(meta.getOrDefault("keywords", "")).append("\n")
                    .append("内容：\n")
                    .append(doc.getText())
                    .append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Prompt
     */
    private String buildPrompt(String query, String context) {

        return """
你是一个严谨的知识助手，只能基于资料回答。

如果资料不足，请回答：资料中未找到相关信息。

----------------
【资料】
%s

【问题】
%s
----------------
""".formatted(context, query);
    }
}
