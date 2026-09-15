package com.zipdaproperty.domain.property.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PropertyWriteApiObservationRecorder {

    private static final int CLIENT_ERROR_STATUS = 400;

    public void record(
            String method,
            int status,
            long durationMillis
    ) {
        String outcome = status < CLIENT_ERROR_STATUS
                ? "SUCCESS"
                : "ERROR";

        if (status < CLIENT_ERROR_STATUS) {
            log.info(
                    "property_write_api completed method={} "
                            + "status={} outcome={} durationMs={}",
                    method,
                    status,
                    outcome,
                    durationMillis
            );
            return;
        }

        log.warn(
                "property_write_api completed method={} "
                        + "status={} outcome={} durationMs={}",
                method,
                status,
                outcome,
                durationMillis
        );
    }
}
