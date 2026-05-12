package com.example.aiagent.filter;

import com.example.aiagent.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API Key 认证过滤器
 *
 * 认证方式：
 * 1. 请求头 X-API-Key: <api_key>
 * 2. 查询参数 api_key=<api_key>
 *
 * 允许的 API Key：
 * - 从配置文件或环境变量读取的合法 API Key
 * - 内置的测试密钥（仅开发环境）
 */
@Slf4j
@Component
public class ApiKeyAuthFilter implements WebFilter {

    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String PARAM_API_KEY = "api_key";

    private static final Set<String> ALLOWED_ORIGINS = new HashSet<>(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://127.0.0.1:3000"
    ));

    private final SecurityProperties securityProperties;

    public ApiKeyAuthFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String apiKey = extractApiKey(exchange);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("认证失败：未提供 API Key - URL: {}", exchange.getRequest().getURI());
            return unauthorized(exchange, "未提供 API Key");
        }

        // 验证 API Key
        if (!isValidApiKey(apiKey)) {
            log.warn("认证失败：无效的 API Key - URL: {}", exchange.getRequest().getURI());
            return unauthorized(exchange, "无效的 API Key");
        }

        log.debug("认证成功：API Key 验证通过 - URL: {}", exchange.getRequest().getURI());

        // 添加认证信息到请求头
        exchange.getRequest().mutate()
                .header("X-Authenticated-User", "api-user")
                .build();

        return chain.filter(exchange);
    }

    /**
     * 从请求中提取 API Key
     */
    private String extractApiKey(ServerWebExchange exchange) {
        // 1. 从请求头读取
        String apiKey = exchange.getRequest().getHeaders().getFirst(HEADER_API_KEY);
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }

        // 2. 从查询参数读取
        apiKey = exchange.getRequest().getQueryParams().getFirst(PARAM_API_KEY);
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }

        return null;
    }

    /**
     * 验证 API Key 是否有效
     */
    private boolean isValidApiKey(String apiKey) {
        // 开发环境：允许测试密钥
        if (isDevMode() && "test-api-key".equals(apiKey)) {
            return true;
        }

        // 生产环境：从配置读取合法的 API Key
        String dashscopeKey = securityProperties.getApiKeys().getDashscopeKey();
        String searchApiKey = securityProperties.getApiKeys().getSearchApiKey();

        // 验证 API Key（实际应用中应该验证哈希值）
        return (dashscopeKey != null && dashscopeKey.equals(apiKey)) ||
               (searchApiKey != null && searchApiKey.equals(apiKey));
    }

    /**
     * 检查是否是开发模式
     */
    private boolean isDevMode() {
        String activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        return "dev".equals(activeProfile) || activeProfile == null;
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("WWW-Authenticate", "API Key");

        String response = String.format("{\"error\":\"%s\",\"code\":401}", message);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(response.getBytes()))
        );
    }
}
