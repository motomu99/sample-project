package com.example.boxwrapper.config;

import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.exception.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * APIキー認証フィルター
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private final BoxClientManager clientManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // Generate request ID for tracing
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);

        // Skip authentication for public endpoints
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate API key
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Request without API key to: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"APIキーが指定されていません\"}");
            return;
        }

        if (!clientManager.isValidApiKey(apiKey)) {
            log.warn("Invalid API key attempted for: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"無効なAPIキーです\"}");
            return;
        }

        // Store API key in request for later use
        request.setAttribute("apiKey", apiKey);

        log.debug("Authenticated request {} with API key", requestId);
        filterChain.doFilter(request, response);
    }

    /**
     * 公開エンドポイントかどうかを判定
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/actuator");
    }
}
