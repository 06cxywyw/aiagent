package com.example.aiagent.opagent;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 错误知识库
 * 存储错误和解决方案，支持语义检索，避免再次犯错
 */
@Slf4j
@Component
public class ErrorKnowledgeBase {

    /**
     * 错误摘要
     */
    @Data
    public static class ErrorSummary {

        /**
         * 错误编码
         */
        private String errorCode;

        /**
         * 错误类型
         */
        private String errorType;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 错误分类
         */
        private String category;

        /**
         * 原因分析
         */
        private List<String> causes;

        /**
         * 解决方案
         */
        private List<String> solutions;

        /**
         * 时间戳
         */
        private Instant timestamp;

        /**
         * 创建时间
         */
        private Instant createTime;

        /**
         * 出现次数
         */
        private int count = 1;

        /**
         * 相似度分数
         */
        private double similarityScore;
    }

    private final EmbeddingModel embeddingModel;

    private final VectorStore vectorStore;

    /**
     * 相似度阈值
     */
    private static final double SIMILARITY_THRESHOLD = 0.6;

    public ErrorKnowledgeBase(
            @Qualifier("dashscopeEmbeddingModel")
            EmbeddingModel embeddingModel,

            @Qualifier("hybridVectorStoreAdapter")
            VectorStore vectorStore
    ) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    /**
     * 添加错误知识
     */
    public void addErrorKnowledge(ErrorSummary summary) {

        if (summary == null
                || StrUtil.isBlank(summary.getErrorMessage())) {
            return;
        }

        try {

            // 检查是否已有相似错误
            List<ErrorSummary> similarErrors =
                    searchSimilarErrors(
                            summary.getErrorMessage(),
                            1
                    );

            if (!similarErrors.isEmpty()) {

                double maxScore = similarErrors.stream()
                        .mapToDouble(ErrorSummary::getSimilarityScore)
                        .max()
                        .orElse(0.0);

                // 相似度太高，不重复存储
                if (maxScore > SIMILARITY_THRESHOLD) {

                    log.debug(
                            "相似错误已存在: {}",
                            summary.getErrorCode()
                    );

                    return;
                }
            }

            // 构建知识文档
            String knowledgeText =
                    buildKnowledgeText(summary);

            Document doc = new Document(knowledgeText);

            doc.getMetadata().put(
                    "error_code",
                    summary.getErrorCode()
            );

            doc.getMetadata().put(
                    "error_type",
                    summary.getErrorType()
            );

            doc.getMetadata().put(
                    "category",
                    summary.getCategory()
            );

            doc.getMetadata().put(
                    "created_at",
                    Instant.now().toString()
            );

            doc.getMetadata().put(
                    "source",
                    "error_knowledge"
            );

            vectorStore.add(List.of(doc));

            log.info(
                    "错误知识已存储: {} - {}",
                    summary.getErrorCode(),
                    summary.getErrorType()
            );

        } catch (Exception e) {

            log.error("错误知识存储失败", e);
        }
    }

    /**
     * 检索相似错误
     */
    public List<ErrorSummary> searchSimilarErrors(
            String errorMessage,
            int limit
    ) {

        List<ErrorSummary> results =
                new ArrayList<>();

        try {

            SearchRequest request =
                    SearchRequest.builder()
                            .query(errorMessage)
                            .topK(limit)
                            .build();

            List<Document> docs =
                    vectorStore.similaritySearch(request);

            if (docs == null || docs.isEmpty()) {
                return results;
            }

            for (Document doc : docs) {

                ErrorSummary summary =
                        parseDocumentToError(doc);

                if (summary != null) {

                    // Spring AI 不同版本分数获取方式不同
                    Object scoreObj =
                            doc.getMetadata()
                                    .get("distance");

                    double score = 1.0;

                    if (scoreObj instanceof Number number) {
                        score = 1 - number.doubleValue();
                    }

                    summary.setSimilarityScore(score);

                    results.add(summary);
                }
            }

        } catch (Exception e) {

            log.error("错误知识检索失败", e);
        }

        return results;
    }

    /**
     * 获取错误解决方案
     */
    public List<String> getSolution(String errorMessage) {

        List<ErrorSummary> similarErrors =
                searchSimilarErrors(errorMessage, 3);

        if (similarErrors.isEmpty()) {
            return getDefaultSolutions();
        }

        // 合并解决方案
        List<String> solutions =
                new ArrayList<>();

        for (ErrorSummary error : similarErrors) {

            if (error.getSolutions() != null) {

                solutions.addAll(
                        error.getSolutions()
                );
            }
        }

        // 去重
        return new ArrayList<>(
                new LinkedHashSet<>(solutions)
        );
    }

    /**
     * 构建知识文本
     */
    private String buildKnowledgeText(
            ErrorSummary summary
    ) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("【错误代码】")
                .append(summary.getErrorCode())
                .append("\n\n");

        sb.append("【错误类型】")
                .append(summary.getErrorType())
                .append("\n\n");

        sb.append("【错误信息】")
                .append(summary.getErrorMessage())
                .append("\n\n");

        // 原因
        if (summary.getCauses() != null
                && !summary.getCauses().isEmpty()) {

            sb.append("【原因分析】\n");

            for (String cause : summary.getCauses()) {

                sb.append("- ")
                        .append(cause)
                        .append("\n");
            }

            sb.append("\n");
        }

        // 解决方案
        if (summary.getSolutions() != null
                && !summary.getSolutions().isEmpty()) {

            sb.append("【解决方案】\n");

            for (String solution : summary.getSolutions()) {

                sb.append("- ")
                        .append(solution)
                        .append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 解析文档为错误摘要
     */
    private ErrorSummary parseDocumentToError(
            Document doc
    ) {

        try {

            ErrorSummary summary =
                    new ErrorSummary();

            String content = doc.getText();

            if (StrUtil.isBlank(content)) {
                return null;
            }

            // 提取错误代码
            Pattern pattern =
                    Pattern.compile(
                            "【错误代码】(.+?)\\n\\n"
                    );

            Matcher matcher =
                    pattern.matcher(content);

            if (matcher.find()) {

                summary.setErrorCode(
                        matcher.group(1)
                );
            }

            // 提取错误类型
            pattern = Pattern.compile(
                    "【错误类型】(.+?)\\n\\n"
            );

            matcher = pattern.matcher(content);

            if (matcher.find()) {

                summary.setErrorType(
                        matcher.group(1)
                );
            }

            // 提取错误信息
            pattern = Pattern.compile(
                    "【错误信息】(.+?)\\n\\n"
            );

            matcher = pattern.matcher(content);

            if (matcher.find()) {

                summary.setErrorMessage(
                        matcher.group(1)
                );
            }

            // 提取原因
            pattern = Pattern.compile(
                    "【原因分析】\\n((?:- .*\\n?)*)"
            );

            matcher = pattern.matcher(content);

            if (matcher.find()) {

                String causesText =
                        matcher.group(1);

                List<String> causes =
                        Arrays.stream(
                                        causesText.split("\n")
                                )
                                .filter(StrUtil::isNotBlank)
                                .map(s -> s.replace("- ", ""))
                                .toList();

                summary.setCauses(causes);
            }

            // 提取解决方案
            pattern = Pattern.compile(
                    "【解决方案】\\n((?:- .*\\n?)*)"
            );

            matcher = pattern.matcher(content);

            if (matcher.find()) {

                String solutionsText =
                        matcher.group(1);

                List<String> solutions =
                        Arrays.stream(
                                        solutionsText.split("\n")
                                )
                                .filter(StrUtil::isNotBlank)
                                .map(s -> s.replace("- ", ""))
                                .toList();

                summary.setSolutions(solutions);
            }

            return summary;

        } catch (Exception e) {

            log.error("解析错误文档失败", e);

            return null;
        }
    }

    /**
     * 默认解决方案
     */
    private List<String> getDefaultSolutions() {

        return Arrays.asList(
                "查看详细日志定位问题",
                "检查相关依赖是否正常",
                "确认参数是否正确",
                "联系技术支持"
        );
    }
}