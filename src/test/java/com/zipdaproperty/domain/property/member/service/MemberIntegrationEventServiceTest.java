package com.zipdaproperty.domain.property.member.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.member.entity.MemberEventConsumption;
import com.zipdaproperty.domain.property.member.entity.PropertyMemberState;
import com.zipdaproperty.domain.property.member.event.MemberIntegrationEvent;
import com.zipdaproperty.domain.property.member.repository.MemberEventConsumptionRepository;
import com.zipdaproperty.domain.property.member.repository.PropertyMemberStateRepository;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MemberIntegrationEventServiceTest {

    private static final Long MEMBER_ID = 1001L;
    private static final Long PROPERTY_ID = 884685586571263701L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-09-11T03:00:00Z");

    private final MemberEventConsumptionRepository consumptionRepository =
            mock(MemberEventConsumptionRepository.class);
    private final PropertyMemberStateRepository memberStateRepository =
            mock(PropertyMemberStateRepository.class);
    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final PropertyRevisionRepository propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);
    private final PropertyStatusHistoryRepository propertyStatusHistoryRepository =
            mock(PropertyStatusHistoryRepository.class);
    private final PropertyAuditEventRecorder propertyAuditEventRecorder =
            mock(PropertyAuditEventRecorder.class);
    private final PropertyKafkaEventPublisher propertyKafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private final MemberIntegrationEventService service =
            new MemberIntegrationEventService(
                    consumptionRepository,
                    memberStateRepository,
                    propertyRepository,
                    propertyRevisionRepository,
                    propertyStatusHistoryRepository,
                    propertyAuditEventRecorder,
                    propertyKafkaEventPublisher,
                    objectMapper
            );

    @Test
    void handle_duplicateEvent_returnsWithoutDomainChanges() {
        MemberIntegrationEvent event = event("evt-duplicate", "MemberSanctioned");
        when(consumptionRepository.existsById(event.eventId()))
                .thenReturn(true);

        boolean handled = service.handle(event);

        assertThat(handled).isFalse();
        verifyNoInteractions(memberStateRepository, propertyRepository);
        verify(consumptionRepository, never()).saveAndFlush(any());
    }

    @Test
    void handle_memberWithdrawn_hidesActivePropertiesAndRecordsEvents() {
        MemberIntegrationEvent event = event("evt-withdrawn", "MemberWithdrawn");
        Property property = mock(Property.class);
        PropertyRevision savedRevision = mock(PropertyRevision.class);

        when(consumptionRepository.existsById(event.eventId()))
                .thenReturn(false);
        when(memberStateRepository.findById(MEMBER_ID))
                .thenReturn(Optional.empty());
        when(propertyRepository.findAllByAuthorMemberIdAndDeletedAtIsNull(MEMBER_ID))
                .thenReturn(List.of(property));
        when(property.getPublicationStatus())
                .thenReturn(PublicationStatus.PUBLISHED);
        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getVersion()).thenReturn(4L);
        when(propertyRepository.saveAndFlush(property)).thenReturn(property);
        when(objectMapper.writeValueAsString(property))
                .thenReturn("{\"publicationStatus\":\"PUBLISHED\"}")
                .thenReturn("{\"publicationStatus\":\"HIDDEN\"}");
        when(objectMapper.writeValueAsString(List.of("publicationStatus")))
                .thenReturn("[\"publicationStatus\"]");
        when(propertyRevisionRepository.saveAndFlush(any()))
                .thenReturn(savedRevision);
        when(savedRevision.getPropertyRevisionId()).thenReturn(77L);

        boolean handled = service.handle(event);

        assertThat(handled).isTrue();
        verify(property).changePublicationStatus(
                eq(PublicationStatus.HIDDEN),
                any(),
                eq(OCCURRED_AT)
        );
        verify(propertyRevisionRepository).saveAndFlush(any());
        verify(propertyStatusHistoryRepository).save(any());
        verify(propertyAuditEventRecorder).recordPropertyAction(
                eq(PROPERTY_ID),
                eq(PropertyAuditActionCode.PROPERTY_HIDDEN_BY_MEMBER_WITHDRAWAL),
                any(),
                eq(null),
                eq(OCCURRED_AT),
                any()
        );
        verify(propertyKafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID),
                eq(4L),
                eq(PropertyEventType.PROPERTY_HIDDEN),
                any(),
                eq(OCCURRED_AT),
                any()
        );

        ArgumentCaptor<PropertyMemberState> stateCaptor =
                ArgumentCaptor.forClass(PropertyMemberState.class);
        verify(memberStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().isWithdrawn()).isTrue();

        ArgumentCaptor<MemberEventConsumption> consumptionCaptor =
                ArgumentCaptor.forClass(MemberEventConsumption.class);
        verify(consumptionRepository).saveAndFlush(consumptionCaptor.capture());
        assertThat(consumptionCaptor.getValue().getEventId())
                .isEqualTo("evt-withdrawn");
    }

    @Test
    void handle_agentApproved_updatesAgentReadModel() {
        MemberIntegrationEvent event = new MemberIntegrationEvent(
                "evt-agent-approved",
                "AgentApproved",
                1,
                MEMBER_ID,
                3001L,
                4001L,
                null,
                OCCURRED_AT,
                "member-agent-approved-test"
        );
        when(consumptionRepository.existsById(event.eventId()))
                .thenReturn(false);
        when(memberStateRepository.findById(MEMBER_ID))
                .thenReturn(Optional.empty());

        boolean handled = service.handle(event);

        assertThat(handled).isTrue();
        ArgumentCaptor<PropertyMemberState> stateCaptor =
                ArgumentCaptor.forClass(PropertyMemberState.class);
        verify(memberStateRepository).save(stateCaptor.capture());

        PropertyMemberState state = stateCaptor.getValue();
        assertThat(state.getAgentId()).isEqualTo(3001L);
        assertThat(state.getAgencyId()).isEqualTo(4001L);
        assertThat(state.isAgentActive()).isTrue();
        verifyNoInteractions(propertyRepository);
    }

    @Test
    void handle_memberSanctioned_updatesSanctionState() {
        MemberIntegrationEvent event = event(
                "evt-sanctioned",
                "MemberSanctioned"
        );
        when(consumptionRepository.existsById(event.eventId()))
                .thenReturn(false);
        when(memberStateRepository.findById(MEMBER_ID))
                .thenReturn(Optional.empty());

        assertThat(service.handle(event)).isTrue();

        ArgumentCaptor<PropertyMemberState> stateCaptor =
                ArgumentCaptor.forClass(PropertyMemberState.class);
        verify(memberStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().isSanctioned()).isTrue();
        assertThat(stateCaptor.getValue().getSanctionScope())
                .isEqualTo("PROPERTY_WRITE");
    }

    @Test
    void handle_withdrawn_hidesOnlyPublishedProperties() {
        MemberIntegrationEvent event = event(
                "evt-withdrawn-non-public",
                "MemberWithdrawn"
        );
        Property inReviewProperty = mock(Property.class);
        when(inReviewProperty.getPublicationStatus())
                .thenReturn(PublicationStatus.IN_REVIEW);
        when(consumptionRepository.existsById(event.eventId()))
                .thenReturn(false);
        when(memberStateRepository.findById(MEMBER_ID))
                .thenReturn(Optional.empty());
        when(propertyRepository.findAllByAuthorMemberIdAndDeletedAtIsNull(MEMBER_ID))
                .thenReturn(List.of(inReviewProperty));

        assertThat(service.handle(event)).isTrue();

        verify(inReviewProperty, never()).changePublicationStatus(
                any(),
                any(),
                any()
        );
        verifyNoInteractions(
                propertyRevisionRepository,
                propertyStatusHistoryRepository,
                propertyAuditEventRecorder,
                propertyKafkaEventPublisher
        );
    }

    private MemberIntegrationEvent event(
            String eventId,
            String eventType
    ) {
        return new MemberIntegrationEvent(
                eventId,
                eventType,
                1,
                MEMBER_ID,
                null,
                null,
                eventType.equals("MemberSanctioned")
                        ? "PROPERTY_WRITE"
                        : null,
                OCCURRED_AT,
                "member-event-test"
        );
    }
}
