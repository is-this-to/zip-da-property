package com.zipdaprojecttak.global.context;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdContext {

    public static final String MDC_KEY = "traceId";

    public static final String REQUEST_ATTRIBUTE =
            TraceIdContext.class.getName() + ".traceId";

    private TraceIdContext() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static void set(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException(
                    "traceId는 비어 있을 수 없습니다."
            );
        }

        MDC.put(
                MDC_KEY,
                traceId
        );
    }

    public static String get() {
        return MDC.get(MDC_KEY);
    }

    public static String getOrCreate() {
        String traceId = get();

        if (traceId == null || traceId.isBlank()) {
            traceId = generate();
            set(traceId);
        }

        return traceId;
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}