package com.example.aiagent.filter;

import com.example.aiagent.config.InMemoryRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * API 限流过滤器
 */
@Slf4j
@Component
@Order(100)
public class RateLimitFilter implements WebFilter {

    private final InMemoryRateLimiter rateLimiter;

    @Autowired
    public RateLimitFilter(InMemoryRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clientId = getClientId(exchange);

        if (!rateLimiter.allowRequest(clientId)) {
            log.warn("请求被限流: clientId={}", clientId);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", "60");
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            return exchange.getResponse().setComplete();
        }

        // 添加限流头信息
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", "60");
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(60 - rateLimiter.getCurrentRequests(clientId)));

        return chain.filter(exchange);
    }

    private String getClientId(ServerWebExchange exchange) {
        // 优先使用 API Key
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return "api:" + apiKey;
        }

        // 其次使用 IP
        String clientIp = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        }

        return "ip:" + clientIp;
    }
}
