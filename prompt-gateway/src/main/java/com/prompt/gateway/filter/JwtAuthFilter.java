package com.prompt.gateway.filter;

import com.prompt.common.constant.CommonConstants;
import com.prompt.common.util.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private List<String> excludePaths;
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthFilter(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (!path.startsWith("/api/") || isExcluded(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(CommonConstants.TOKEN_HEADER);
        if (authHeader == null || !authHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return unauthorized(exchange, "未登录或Token缺失");
        }

        String token = authHeader.substring(CommonConstants.TOKEN_PREFIX.length());

        return reactiveRedisTemplate.hasKey("blacklist:" + token)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return unauthorized(exchange, "Token已失效，请重新登录");
                    }
                    if (!JwtUtil.validate(token)) {
                        return unauthorized(exchange, "Token无效或已过期");
                    }
                    Long userId = JwtUtil.getUserId(token);
                    String username = JwtUtil.getUsername(token);
                    String role = JwtUtil.getRole(token);

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header(CommonConstants.USER_ID_HEADER, String.valueOf(userId))
                            .header("X-Username", username)
                            .header(CommonConstants.USER_ROLE_HEADER, role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean isExcluded(String path) {
        if (excludePaths == null || excludePaths.isEmpty()) {
            return false;
        }
        return excludePaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message, System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
