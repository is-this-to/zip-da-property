package com.zipdaproperty.domain.property.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PropertyWriteObservabilityFilter
        extends OncePerRequestFilter {

    private static final String PROPERTY_API_PREFIX =
            "/api/property/properties";

    private static final Set<String> WRITE_METHODS = Set.of(
            "POST",
            "PUT",
            "PATCH",
            "DELETE"
    );

    private static final int CLIENT_ERROR_STATUS = 400;

    private static final int INTERNAL_SERVER_ERROR = 500;

    private final PropertyWriteApiObservationRecorder recorder;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAtNanos = System.nanoTime();
        boolean completed = false;

        try {
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            int status = completed
                    ? response.getStatus()
                    : normalizeFailureStatus(response.getStatus());

            recorder.record(
                    request.getMethod(),
                    status,
                    elapsedMillis(startedAtNanos)
            );
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isPropertyWriteRequest(request);
    }

    private boolean isPropertyWriteRequest(
            HttpServletRequest request
    ) {
        String requestUri = request.getRequestURI();
        boolean propertyApi = requestUri.equals(PROPERTY_API_PREFIX)
                || requestUri.startsWith(PROPERTY_API_PREFIX + "/");

        return propertyApi
                && WRITE_METHODS.contains(request.getMethod());
    }

    private int normalizeFailureStatus(int responseStatus) {
        return responseStatus >= CLIENT_ERROR_STATUS
                ? responseStatus
                : INTERNAL_SERVER_ERROR;
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAtNanos
        );
    }
}
