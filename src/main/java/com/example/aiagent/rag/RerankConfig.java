package com.example.aiagent.rag;

import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云重排序模型配置
 */
@Slf4j
@Configuration
public class RerankConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Bean
    public DashScopeRerankModel dashScopeRerankModel() {
        DashScopeRerankOptions options = DashScopeRerankOptions.builder()
                .modelName("bge-rerank-large")
                .build();

        return DashScopeRerankModel.builder()
                .apiKey(dashScopeApiKey)
                .dashScopeRerankOptions(options)
                .build();
    }
}
