package com.zipdaproperty.domain.property.event;

import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyKafkaEventListenerTest {

    private static final String TOPIC_NAME =
            "zipda.property.events.v1";

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate =
            mock(KafkaTemplate.class);

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final PropertyKafkaEventListener listener =
            new PropertyKafkaEventListener(
                    kafkaTemplate,
                    objectMapper,
                    TOPIC_NAME
            );

    @Test
    void publish_validEvent_sendsJsonToKafka() {
        PropertyKafkaEvent event = new PropertyKafkaEvent(
                884800000000000001L,
                PropertyEventType.PROPERTY_CREATED,
                1,
                "PROPERTY",
                884700000000000001L,
                0L,
                Instant.parse("2026-09-10T08:00:00Z"),
                "property-kafka-listener-test",
                1001L,
                "USER",
                Map.of("title", "Kafka 직접 발행 테스트 매물")
        );

        String payloadJson = "{\"eventType\":\"PROPERTY_CREATED\"}";

        CompletableFuture<SendResult<String, String>> sendFuture =
                CompletableFuture.completedFuture(null);

        when(objectMapper.writeValueAsString(event))
                .thenReturn(payloadJson);

        when(
                kafkaTemplate.send(
                        TOPIC_NAME,
                        event.aggregateId().toString(),
                        payloadJson
                )
        ).thenReturn(sendFuture);

        listener.publish(event);

        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(
                TOPIC_NAME,
                event.aggregateId().toString(),
                payloadJson
        );
    }
}
