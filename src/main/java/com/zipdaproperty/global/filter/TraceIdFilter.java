package com.zipdaproperty.global.filter;

import com.zipdaproperty.global.context.TraceIdContext;
import com.zipdaproperty.global.context.constant.InternalHeaderName;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 스프링에 존재하는 수많은 필터(시큐리티 필터 포함) 중에서 가장 먼저 실행
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(
                request.getHeader(InternalHeaderName.X_TRACE_ID)
        );

        TraceIdContext.set(traceId);

        request.setAttribute(
                TraceIdContext.REQUEST_ATTRIBUTE,
                traceId
        );

        response.setHeader(
                InternalHeaderName.X_TRACE_ID,
                traceId
        );

        try {
            filterChain.doFilter(
                    request,
                    response
            );
        } finally {
            TraceIdContext.clear();
        }
    }

    private String resolveTraceId(String traceIdHeader) {
        // 1. 헤더에 번호가 아예 없거나 비어있으면 새로 발급
        if (traceIdHeader == null || traceIdHeader.isBlank()) {
            return TraceIdContext.generate();
        }

        String trimmedTraceId = traceIdHeader.trim();

        // 2. 누군가 해킹이나 장난을 치려고 번호에 이상한 짓을 했는지 정규식으로 검사
        if (!TRACE_ID_PATTERN.matcher(trimmedTraceId).matches()) {
            return TraceIdContext.generate(); // 이상하면 새로 발급
        }

        return trimmedTraceId; // 정상이면 그대로 사용
    }
}