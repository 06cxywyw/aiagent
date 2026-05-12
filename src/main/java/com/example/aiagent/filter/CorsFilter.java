package com.example.aiagent.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * CORS 过滤器
 * 允许跨域请求
 */
@Slf4j
@Component
@Order(-1)
public class CorsFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers",
                "Content-Type, X-API-Key, Authorization");
        exchange.getResponse().getHeaders().add("Access-Control-Max-Age", "3600");

        // 处理预检请求
        if ("OPTIONS".equals(exchange.getRequest().getMethod().name())) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
