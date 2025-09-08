package com.doomole.uptime.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // CorrelationIdFilter 다음(있다면)
public class HttpLoggingFilter extends OncePerRequestFilter {
    private static final long   SLOW_MS            = 1000;   // 느린 요청 기준
    private static final int    MAX_LOG_BYTES      = 2048;   // 본문 최대 저장
    private static final double DEBUG_SAMPLE_RATE  = 0.10;   // 10%만 상세(헤더/바디)
    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/actuator", "/favicon", "/assets", "/static", "/webjars"
    );
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie"
    );
    private static final Set<String> BINARY_CONTENT_TYPES = Set.of(
            "application/octet-stream", "application/zip", "image/", "audio/", "video/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();
        if (shouldSkip(uri)) { // 정적/헬스체크 등은 패스
            chain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();

        // 본문 로깅을 위해 캐싱 래퍼 사용
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(req, res);
        } finally {
            long took = System.currentTimeMillis() - start;

            // 인증 주체 로깅 (principal을 Long id로 세팅했다고 가정)
            Long userId = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long p) {
                userId = p;
            }

            // 요약 1줄: 성공은 INFO, 느림/오류는 WARN
            if (took > SLOW_MS || res.getStatus() >= 400) {
                log.warn("HTTP {} {} [{}] uid={} ip={} took={}ms status={}",
                        req.getMethod(),
                        uri,
                        req.getQueryString() == null ? "" : req.getQueryString(),
                        userId,
                        clientIp(req),
                        took,
                        res.getStatus());
            } else {
                log.info("HTTP {} {} took={}ms status={}",
                        req.getMethod(), uri, took, res.getStatus());
            }

            // DEBUG 샘플링: 헤더/바디
            if (log.isDebugEnabled() && Math.random() < DEBUG_SAMPLE_RATE) {
                log.debug("REQ headers={}", safeHeaders(req));

                // 바이너리/멀티파트/큰 본문은 스킵
                if (!isBinary(req.getContentType())) {
                    byte[] rb = req.getContentAsByteArray();
                    if (rb != null && rb.length > 0) {
                        log.debug("REQ body={}", safeTrim(new String(rb, StandardCharsets.UTF_8), MAX_LOG_BYTES));
                    }
                }

                // 응답 바디
                String resCt = res.getContentType();
                if (!isBinary(resCt)) {
                    byte[] sb = res.getContentAsByteArray();
                    if (sb != null && sb.length > 0) {
                        log.debug("RES body={}", safeTrim(new String(sb, StandardCharsets.UTF_8), MAX_LOG_BYTES));
                    }
                }
            }

            // 반드시 원 응답으로 복사
            res.copyBodyToResponse();
        }
    }

    private boolean shouldSkip(String uri) {
        for (String p : SKIP_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        return false;
    }

    private String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        String xr = req.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) return xr;
        return req.getRemoteAddr();
    }

    private String safeHeaders(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        for (Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements(); ) {
            String name = e.nextElement();
            String lower = name.toLowerCase();
            if (SENSITIVE_HEADERS.contains(lower)) {
                sb.append(name).append("=").append("***").append(' ');
            } else {
                sb.append(name).append("=").append(req.getHeader(name)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private boolean isBinary(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        if (ct.startsWith("multipart/")) return true;
        for (String prefix : BINARY_CONTENT_TYPES) {
            if (prefix.endsWith("/")) { // image/, audio/ ...
                if (ct.startsWith(prefix)) return true;
            } else { // 정확 매치
                if (ct.startsWith(prefix)) return true;
            }
        }
        return false;
    }

    private String safeTrim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
