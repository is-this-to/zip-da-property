package com.zipdaproperty.domain.property.member.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.entity.PropertyStatusHistory;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.member.constant.MemberIntegrationEventType;
import com.zipdaproperty.domain.property.member.entity.MemberEventConsumption;
import com.zipdaproperty.domain.property.member.entity.PropertyMemberState;
import com.zipdaproperty.domain.property.member.event.MemberIntegrationEvent;
import com.zipdaproperty.domain.property.member.repository.MemberEventConsumptionRepository;
import com.zipdaproperty.domain.property.member.repository.PropertyMemberStateRepository;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberIntegrationEventService {

    private static final String WITHDRAWAL_REASON =
            "회원 탈퇴 이벤트로 공개 매물을 숨김 처리했습니다.";

    private final MemberEventConsumptionRepository consumptionRepository;
    private final PropertyMemberStateRepository memberStateRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyRevisionRepository propertyRevisionRepository;
    private final PropertyStatusHistoryRepository propertyStatusHistoryRepository;
    private final PropertyAuditEventRecorder propertyAuditEventRecorder;
    private final PropertyKafkaEventPublisher propertyKafkaEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public boolean handle(MemberIntegrationEvent event) {
        MemberIntegrationEventType eventType =
                MemberIntegrationEventType.from(event.eventType());

        if (consumptionRepository.existsById(event.eventId())) {
            return false;
        }

        Instant processedAt = Instant.now();
        PropertyMemberState state = memberStateRepository
                .findById(event.memberId())
                .orElseGet(
                        () -> PropertyMemberState.create(
                                event.memberId(),
                                event.eventId(),
                                event.occurredAt(),
                                processedAt
                        )
                );

        boolean stateChanged = state.accepts(
                event.eventId(),
                event.occurredAt()
        ) || state.getLastEventId().equals(event.eventId());

        if (stateChanged) {
            applyEvent(eventType, event, state, processedAt);
            memberStateRepository.save(state);
        }

        consumptionRepository.saveAndFlush(
                new MemberEventConsumption(
                        event.eventId(),
                        eventType.name(),
                        event.memberId(),
                        event.occurredAt(),
                        processedAt
                )
        );

        return true;
    }

    private void applyEvent(
            MemberIntegrationEventType eventType,
            MemberIntegrationEvent event,
            PropertyMemberState state,
            Instant processedAt
    ) {
        switch (eventType) {
            case MEMBER_WITHDRAWN -> {
                state.withdraw(
                        event.eventId(),
                        event.occurredAt(),
                        processedAt
                );
                hideMemberProperties(event);
            }
            case MEMBER_SANCTIONED -> state.sanction(
                    event.sanctionScope(),
                    event.eventId(),
                    event.occurredAt(),
                    processedAt
            );
            case MEMBER_SANCTION_RELEASED -> state.releaseSanction(
                    event.eventId(),
                    event.occurredAt(),
                    processedAt
            );
            case AGENT_APPROVED -> state.approveAgent(
                    event.agentId(),
                    event.agencyId(),
                    event.eventId(),
                    event.occurredAt(),
                    processedAt
            );
            case AGENT_SUSPENDED -> state.suspendAgent(
                    event.eventId(),
                    event.occurredAt(),
                    processedAt
            );
        }
    }

    private void hideMemberProperties(MemberIntegrationEvent event) {
        ActorContext actorContext = ActorContext.system(event.traceId());
        List<Property> properties = propertyRepository
                .findAllByAuthorMemberIdAndDeletedAtIsNull(event.memberId());

        for (Property property : properties) {
            if (property.getPublicationStatus() != PublicationStatus.PUBLISHED) {
                continue;
            }

            hideProperty(property, event, actorContext);
        }
    }

    private void hideProperty(
            Property property,
            MemberIntegrationEvent event,
            ActorContext actorContext
    ) {
        PublicationStatus beforeStatus = property.getPublicationStatus();
        String beforeSnapshot = objectMapper.writeValueAsString(property);

        property.changePublicationStatus(
                PublicationStatus.HIDDEN,
                actorContext,
                event.occurredAt()
        );
        Property savedProperty = propertyRepository.saveAndFlush(property);

        String afterSnapshot = objectMapper.writeValueAsString(savedProperty);
        String changedFields = objectMapper.writeValueAsString(
                List.of("publicationStatus")
        );

        PropertyRevision revision = PropertyRevision.statusChanged(
                savedProperty.getPropertyId(),
                savedProperty.getVersion(),
                changedFields,
                beforeSnapshot,
                afterSnapshot,
                WITHDRAWAL_REASON,
                actorContext,
                event.occurredAt()
        );
        PropertyRevision savedRevision =
                propertyRevisionRepository.saveAndFlush(revision);

        propertyStatusHistoryRepository.save(
                new PropertyStatusHistory(
                        savedProperty.getPropertyId(),
                        savedRevision.getPropertyRevisionId(),
                        PropertyStatusType.PUBLICATION,
                        beforeStatus.name(),
                        PublicationStatus.HIDDEN.name(),
                        PropertyAuditActionCode.PROPERTY_HIDDEN_BY_MEMBER_WITHDRAWAL,
                        WITHDRAWAL_REASON,
                        savedProperty.getVersion(),
                        event.occurredAt(),
                        actorContext
                )
        );

        propertyAuditEventRecorder.recordPropertyAction(
                savedProperty.getPropertyId(),
                PropertyAuditActionCode.PROPERTY_HIDDEN_BY_MEMBER_WITHDRAWAL,
                WITHDRAWAL_REASON,
                null,
                event.occurredAt(),
                actorContext
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("propertyId", savedProperty.getPropertyId().toString());
        payload.put("version", savedProperty.getVersion());
        payload.put("beforePublicationStatus", beforeStatus.name());
        payload.put("publicationStatus", PublicationStatus.HIDDEN.name());
        payload.put("reasonCode", PropertyAuditActionCode.PROPERTY_HIDDEN_BY_MEMBER_WITHDRAWAL);

        propertyKafkaEventPublisher.publishAfterCommit(
                savedProperty.getPropertyId(),
                savedProperty.getVersion(),
                PropertyEventType.PROPERTY_HIDDEN,
                payload,
                event.occurredAt(),
                actorContext
        );
    }
}
