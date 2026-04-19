package com.example.aiagent.tools;

import com.example.aiagent.rag.HybridRetriever;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagTool {

    @Resource
    private HybridRetriever hybridRetriever;

    @Tool(name = "knowledge_search", description = "从知识库中检索相关信息")
    public String search(String query) {

        List<Document> docs = hybridRetriever.retrieve(query);

        return docs.stream()
                .limit(5)
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);
    }
}
