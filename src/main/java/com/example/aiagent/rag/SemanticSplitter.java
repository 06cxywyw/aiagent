package com.example.aiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 语意切分器
 */
@Component
public class SemanticSplitter {

    /**
     * 按Markdown结构和段落进行语义切片
     * 优化了中文处理，保供更完整的语义单位
     */
    public List<Document> splitByMarkdown(List<Document> documents) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isEmpty()) continue;

            // 🔧 改进：先按标题(# ## ###)切分，然后按段落细分
            String[] titleSections = text.split("(?m)^#{1,3}\\s+");
            
            for (String section : titleSections) {
                if (section.trim().isEmpty()) continue;

                // 再按段落切分（两个或以上换行符为分隔）
                String[] paragraphs = section.split("\n\n+");
                
                for (String paragraph : paragraphs) {
                    String trimmedPara = paragraph.trim();
                    if (trimmedPara.isEmpty() || trimmedPara.length() < 10) continue;

                    // ⭐ 创建新文档，保留原始元数据
                    Document newDoc = new Document(trimmedPara);
                    newDoc.getMetadata().putAll(doc.getMetadata());
                    
                    result.add(newDoc);
                }
            }
        }

        return result;
    }
    
    /**
     * 激进切片（按句号切分）
     */
    public List<Document> splitBySentence(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        
        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isEmpty()) continue;

            // 按中文句号、英文句号、感叹号、问号切分
            String[] sentences = text.split("[。.!！?？]");
            
            StringBuilder buffer = new StringBuilder();
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                if (trimmed.isEmpty()) continue;
                
                buffer.append(trimmed);
                
                // 累积到足够长度才创建文档
                if (buffer.length() >= 200) {
                    Document newDoc = new Document(buffer.toString());
                    newDoc.getMetadata().putAll(doc.getMetadata());
                    result.add(newDoc);
                    buffer = new StringBuilder();
                }
            }
            
            // 处理剩余内容
            if (buffer.length() > 0) {
                Document newDoc = new Document(buffer.toString());
                newDoc.getMetadata().putAll(doc.getMetadata());
                result.add(newDoc);
            }
        }
        
        return result;
    }
}
