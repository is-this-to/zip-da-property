package com.zipdaprojecttak.global.filter;

import com.zipdaprojecttak.global.context.TraceIdContext;
import com.zipdaprojecttak.global.context.constant.InternalHeaderName;
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
@Order(Ordered.HIGHEST_PRECEDENCE)
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
        if (traceIdHeader == null || traceIdHeader.isBlank()) {
            return TraceIdContext.generate();
        }

        String trimmedTraceId = traceIdHeader.trim();

        if (!TRACE_ID_PATTERN.matcher(trimmedTraceId).matches()) {
            return TraceIdContext.generate();
        }

        return trimmedTraceId;
    }
}