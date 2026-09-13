package com.zipdaproperty.domain.property.audit.service;

import com.zipdaproperty.domain.property.audit.entity.PropertyAuditEvent;
import com.zipdaproperty.domain.property.audit.repository.PropertyAuditEventRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyAuditEventRecorder {

    private static final String PROPERTY_TARGET_TYPE = "PROPERTY";

    private final PropertyAuditEventRepository propertyAuditEventRepository;

    public PropertyAuditEvent recordPropertyAction(
            Long propertyId,
            String actionCode,
            String reason,
            String clientIpHash,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        Long requiredPropertyId = Objects.requireNonNull(
                propertyId,
                "감사 대상 propertyId는 필수입니다."
        );

        if (requiredPropertyId <= 0) {
            throw new IllegalArgumentException(
                    "감사 대상 propertyId는 0보다 커야 합니다."
            );
        }

        return recordAction(
                PROPERTY_TARGET_TYPE,
                requiredPropertyId.toString(),
                actionCode,
                reason,
                clientIpHash,
                occurredAt,
                actorContext
        );
    }

    public PropertyAuditEvent recordAction(
            String targetType,
            String targetId,
            String actionCode,
            String reason,
            String clientIpHash,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        PropertyAuditEvent auditEvent =
                PropertyAuditEvent.capture(
                        targetType,
                        targetId,
                        actionCode,
                        reason,
                        clientIpHash,
                        occurredAt,
                        actorContext
                );

        return propertyAuditEventRepository.save(auditEvent);
    }
}