package com.zipdaproperty.domain.property.member.event;

import com.zipdaproperty.domain.property.member.service.MemberIntegrationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberKafkaEventListener {

    private final ObjectMapper objectMapper;
    private final MemberIntegrationEventService memberIntegrationEventService;

    @KafkaListener(
            topics = "${member.kafka.topic:zipda.member.events.v1}",
            groupId = "${member.kafka.consumer-group:zip-da-property-member-events}",
            autoStartup = "${member.kafka.enabled:false}"
    )
    public void consume(String message) {
        MemberIntegrationEvent event = objectMapper.readValue(
                message,
                MemberIntegrationEvent.class
        );

        memberIntegrationEventService.handle(event);

        log.info(
                "Member Kafka 이벤트 처리 완료. eventId={}, eventType={}, memberId={}",
                event.eventId(),
                event.eventType(),
                event.memberId()
        );
    }
}
