package com.example.aiagent.rag;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class Reranker {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private final RestClient restClient =
            RestClient.builder()
                    .baseUrl("https://dashscope.aliyuncs.com")
                    .build();

    /**
     * rerank 重排序
     */
    public List<Document> rerank(
            String query,
            List<Document> docs
    ) {

        if (docs == null || docs.isEmpty()) {
            return docs;
        }

        try {

            // 文档内容
            List<String> contents = docs.stream()
                    .map(Document::getText)
                    .toList();

            // 构建请求
            RerankRequest request = new RerankRequest();

            request.setModel("gte-rerank");

            request.setInput(
                    new Input(query, contents)
            );

            // 调用 DashScope rerank API
            RerankResponse response =
                    restClient.post()
                            .uri("/api/v1/services/rerank/text-rerank/text-rerank")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + apiKey
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(RerankResponse.class);

            // 判空
            if (response == null
                    || response.getOutput() == null
                    || response.getOutput().getResults() == null
                    || response.getOutput().getResults().isEmpty()) {

                log.warn("rerank 返回为空");

                return docs;
            }

            List<Document> resultDocs = new ArrayList<>();

            // 映射 rerank 分数
            response.getOutput()
                    .getResults()
                    .forEach(result -> {

                        int index = result.getIndex();

                        if (index >= 0 && index < docs.size()) {

                            Document doc = docs.get(index);

                            doc.getMetadata().put(
                                    "rerank_score",
                                    result.getRelevanceScore()
                            );

                            resultDocs.add(doc);
                        }
                    });

            // 按分数排序
            resultDocs.sort(
                    Comparator.comparingDouble(
                            (Document d) -> {

                                Object score =
                                        d.getMetadata()
                                                .getOrDefault(
                                                        "rerank_score",
                                                        0.0
                                                );

                                return score instanceof Number
                                        ? ((Number) score).doubleValue()
                                        : 0.0;
                            }
                    ).reversed()
            );

            log.info("rerank 完成，文档数={}", resultDocs.size());

            return resultDocs;

        } catch (Exception e) {

            log.error("rerank 失败", e);

            return docs;
        }
    }

    /**
     * 请求体
     */
    @Data
    static class RerankRequest {

        private String model;

        private Input input;
    }

    /**
     * input
     */
    @Data
    static class Input {

        private String query;

        private List<String> documents;

        public Input(
                String query,
                List<String> documents
        ) {
            this.query = query;
            this.documents = documents;
        }
    }

    /**
     * 响应体
     */
    @Data
    static class RerankResponse {

        private Output output;
    }

    /**
     * output
     */
    @Data
    static class Output {

        private List<Result> results;
    }

    /**
     * result
     */
    @Data
    static class Result {

        private Integer index;

        private Double relevanceScore;
    }
}