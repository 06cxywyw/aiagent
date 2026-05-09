package com.example.aiagent.memory;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 模型配置
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel dashscopeEmbeddingModel() {
        return new DashScopeEmbeddingModel();
    }
}
