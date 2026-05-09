package com.example.aiagent.rag;

import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.RerankDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 重排序器 - 基于阿里云 rerank 模型
 */
@Slf4j
@Component
public class Reranker {

    private final DashScopeRerankModel rerankModel;

    public Reranker(DashScopeRerankModel rerankModel) {
        this.rerankModel = rerankModel;
    }

    /**
     * 重排序文档
     *
     * @param query   原始查询
     * @param docs    召回的文档列表
     * @return        按相关性得分排序后的文档
     */
    public List<Document> rerank(String query, List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return docs;
        }

        try {
            // 转换为 rerank 格式
            List<RerankDocument> rerankDocs = docs.stream()
                    .map(doc -> new RerankDocument(doc.getText()))
                    .collect(Collectors.toList());

            // 调用 rerank API
            var response = rerankModel.call(query, rerankDocs);

            if (response == null || response.getOutput() == null) {
                log.warn("rerank API 返回空，返回原始顺序");
                return docs;
            }

            // 解析结果并排序
            var results = response.getOutput().getResults();
            if (results == null || results.isEmpty()) {
                log.warn("rerank 结果为空，返回原始顺序");
                return docs;
            }

            // 将得分映射回原文档
            List<Document> resultDocs = new ArrayList<>();
            for (var result : results) {
                int index = result.getIndex();
                if (index >= 0 && index < docs.size()) {
                    Document doc = docs.get(index);
                    doc.getMetadata().put("rerank_score", result.getRelevanceScore());
                    resultDocs.add(doc);
                }
            }

            // 按得分降序排序
            resultDocs.sort(Comparator.comparingDouble(
                    d -> (Double) d.getMetadata().getOrDefault("rerank_score", 0.0)
            ).reversed());

            log.info("rerank 完成: {} 个文档", resultDocs.size());
            return resultDocs;

        } catch (Exception e) {
            log.error("rerank 失败，返回原始顺序", e);
            return docs;
        }
    }
}
