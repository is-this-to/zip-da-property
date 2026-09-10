package com.zipdaproperty.domain.property.event;

import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.id.TsidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PropertyKafkaEventPublisher {

    private static final String PROPERTY_AGGREGATE_TYPE = "PROPERTY";
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final TsidGenerator tsidGenerator;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishAfterCommit(
            Long propertyId,
            Long aggregateVersion,
            String eventType,
            Map<String, Object> payload,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        ActorContext requiredActorContext =
                Objects.requireNonNull(
                        actorContext,
                        "Kafka 이벤트에는 ActorContext가 필요합니다."
                );

        String actorRole =
                requiredActorContext.role() == null
                        ? requiredActorContext.actionSource().name()
                        : requiredActorContext.role().name();

        PropertyKafkaEvent event =
                new PropertyKafkaEvent(
                        tsidGenerator.generate(),
                        eventType,
                        CURRENT_SCHEMA_VERSION,
                        PROPERTY_AGGREGATE_TYPE,
                        propertyId,
                        aggregateVersion,
                        occurredAt,
                        requiredActorContext.traceId(),
                        requiredActorContext.memberId(),
                        actorRole,
                        payload
                );

        applicationEventPublisher.publishEvent(event);
    }
}
