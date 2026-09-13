package com.zipdaproperty.domain.property.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Slf4j
@Component
public class PropertyKafkaEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public PropertyKafkaEventListener(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${property.kafka.topic:zipda.property.events.v1}")
            String topicName
    ) {
        this.kafkaTemplate = Objects.requireNonNull(
                kafkaTemplate,
                "KafkaTemplate은 필수입니다."
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "ObjectMapper는 필수입니다."
        );

        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException(
                    "Property Kafka 토픽 이름은 필수입니다."
            );
        }

        this.topicName = topicName;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void publish(PropertyKafkaEvent event) {
        Objects.requireNonNull(
                event,
                "발행할 Property Kafka 이벤트는 필수입니다."
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                            topicName,
                            event.aggregateId().toString(),
                            payloadJson
                    )
                    .whenComplete(
                            (result, exception) -> {
                                if (exception != null) {
                                    log.error(
                                            "Property Kafka 이벤트 발행 실패. "
                                                    + "eventId={}, eventType={}, propertyId={}",
                                            event.eventId(),
                                            event.eventType(),
                                            event.aggregateId(),
                                            exception
                                    );
                                    return;
                                }

                                log.info(
                                        "Property Kafka 이벤트 발행 성공. "
                                                + "eventId={}, eventType={}, propertyId={}",
                                        event.eventId(),
                                        event.eventType(),
                                        event.aggregateId()
                                );
                            }
                    );
        } catch (RuntimeException exception) {
            log.error(
                    "Property Kafka 이벤트 직렬화 또는 전송 요청 실패. "
                            + "eventId={}, eventType={}, propertyId={}",
                    event.eventId(),
                    event.eventType(),
                    event.aggregateId(),
                    exception
            );
        }
    }
}
