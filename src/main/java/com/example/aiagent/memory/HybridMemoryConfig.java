package com.example.aiagent.memory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 混合记忆系统配置
 * 所有 Memory 类都使用 @Component 注解，由 Spring 自动扫描注入
 */
@Configuration
public class HybridMemoryConfig {

    /**
     * 混合记忆管理器
     * 依赖注入顺序：shortTermMemory, longTermMemory, entityMemory
     */
    @Bean
    public MixedMemory mixedMemory(
            RedisChatMemory shortTermMemory,
            LongTermMemory longTermMemory,
            EntityMemory entityMemory) {
        return new MixedMemory(shortTermMemory, longTermMemory, entityMemory);
    }
}
