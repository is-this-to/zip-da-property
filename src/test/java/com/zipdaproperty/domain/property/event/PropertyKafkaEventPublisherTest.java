package com.zipdaproperty.domain.property.event;

import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.id.TsidGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyKafkaEventPublisherTest {

    private static final Long EVENT_ID = 884800000000000001L;
    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long MEMBER_ID = 1001L;
    private static final Long VERSION = 3L;
    private static final String TRACE_ID = "property-kafka-publisher-test";
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-09-10T08:00:00Z");

    private final TsidGenerator tsidGenerator =
            mock(TsidGenerator.class);

    private final ApplicationEventPublisher applicationEventPublisher =
            mock(ApplicationEventPublisher.class);

    private final PropertyKafkaEventPublisher publisher =
            new PropertyKafkaEventPublisher(
                    tsidGenerator,
                    applicationEventPublisher
            );

    @Test
    void publishAfterCommit_validInput_publishesApplicationEvent() {
        when(tsidGenerator.generate()).thenReturn(EVENT_ID);

        ActorContext actorContext = ActorContext.member(
                MEMBER_ID,
                ActorRole.USER,
                TRACE_ID
        );

        Map<String, Object> payload = Map.of(
                "propertyId",
                PROPERTY_ID,
                "version",
                VERSION
        );

        publisher.publishAfterCommit(
                PROPERTY_ID,
                VERSION,
                PropertyEventType.PROPERTY_UPDATED,
                payload,
                OCCURRED_AT,
                actorContext
        );

        ArgumentCaptor<PropertyKafkaEvent> eventCaptor =
                ArgumentCaptor.forClass(PropertyKafkaEvent.class);

        verify(applicationEventPublisher)
                .publishEvent(eventCaptor.capture());

        PropertyKafkaEvent event = eventCaptor.getValue();

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.eventType())
                .isEqualTo(PropertyEventType.PROPERTY_UPDATED);
        assertThat(event.schemaVersion()).isEqualTo(1);
        assertThat(event.aggregateType()).isEqualTo("PROPERTY");
        assertThat(event.aggregateId()).isEqualTo(PROPERTY_ID);
        assertThat(event.aggregateVersion()).isEqualTo(VERSION);
        assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.traceId()).isEqualTo(TRACE_ID);
        assertThat(event.actorMemberId()).isEqualTo(MEMBER_ID);
        assertThat(event.actorRole()).isEqualTo("USER");
        assertThat(event.payload()).containsAllEntriesOf(payload);
    }
}
