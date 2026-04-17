package com.example.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 元数据增强器
 */
@Component
@Slf4j
public class MyKeywordEnricher {

    @Resource
    private ChatModel chatModel;

    public List<Document> enrichDocuments(List<Document> documents) {

        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {

            try {
                String text = doc.getText();

                if (text == null || text.isEmpty()) {
                    result.add(doc);
                    continue;
                }

                // ✅ 自定义 Prompt（核心）
                String promptText = """
请从以下文本中提取5个中文关键词，要求：
1. 每个关键词不超过6个字
2. 用逗号分隔
3. 只返回关键词，不要解释

文本：
""" + text;

                Prompt prompt = new Prompt(promptText);

                String keywords = chatModel.call(prompt)
                        .getResult()
                        .getOutput()
                        .getText();

                doc.getMetadata().put("keywords", keywords);

            } catch (Exception e) {

                log.warn("❌关键词提取失败，使用降级策略");

                // ✅ fallback
                String text = doc.getText();
                if (text != null) {
                    String keyword = text.length() > 10
                            ? text.substring(0, 10)
                            : text;

                    doc.getMetadata().put("keywords", keyword);
                }
            }

            result.add(doc);
        }

        return result;
    }
}