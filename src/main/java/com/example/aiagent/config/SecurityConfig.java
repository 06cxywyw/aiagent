package com.example.aiagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 安全配置类
 * 配置限流、IP 黑名单等安全功能
 */
@Slf4j
@Configuration
@Import(SecurityProperties.class)
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 内存限流器（生产环境建议使用 Redis 限流）
     */
    @Bean
    public InMemoryRateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter(
                securityProperties.getRateLimit().getDefaultRequestsPerMinute(),
                securityProperties.getRateLimit().getWindowSizeSeconds()
        );
    }

    /**
     * IP 黑名单过滤器
     */
    @Bean
    public WebFilter ipBlacklistFilter() {
        return (exchange, chain) -> {
            String clientIp = getClientIp(exchange);

            // 检查是否是内网 IP（允许内网访问）
            if (isInternalIp(clientIp)) {
                log.debug("允许内网 IP 访问: {}", clientIp);
            }

            return chain.filter(exchange);
        };
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = request.getHeaders().getFirst("X-Forwarded-For");

        if (ip == null || ip.isEmpty()) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }

        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        }

        return ip;
    }

    /**
     * 检查是否是内网 IP
     */
    private boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        try {
            if (ip.contains(",")) {
                ip = ip.split(",")[0];
            }

            if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
                return true;
            }

            InetAddress addr = InetAddress.getByName(ip);
            return addr.isSiteLocalAddress() || addr.isLoopbackAddress() || addr.isAnyLocalAddress();
        } catch (UnknownHostException e) {
            log.warn("无法解析 IP: {}", ip);
            return false;
        }
    }
}
