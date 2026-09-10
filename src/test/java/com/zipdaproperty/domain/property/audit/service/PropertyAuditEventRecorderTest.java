package com.zipdaproperty.domain.property.audit.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.entity.PropertyAuditEvent;
import com.zipdaproperty.domain.property.audit.repository.PropertyAuditEventRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActionSource;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyAuditEventRecorderTest {

    private static final Long PROPERTY_ID =
            884700000000000001L;

    private static final Long MEMBER_ID = 1001L;

    private static final String TRACE_ID =
            "property-audit-recorder-test";

    private static final String REASON =
            "매물 등록 감사 이벤트 테스트";

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-09-10T04:00:00Z");

    private final PropertyAuditEventRepository
            propertyAuditEventRepository =
            mock(PropertyAuditEventRepository.class);

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder =
            new PropertyAuditEventRecorder(
                    propertyAuditEventRepository
            );

    private final ActorContext ownerContext =
            ActorContext.member(
                    MEMBER_ID,
                    ActorRole.USER,
                    TRACE_ID
            );

    @Test
    void recordPropertyAction_validInput_savesPropertyAuditEvent() {
        when(
                propertyAuditEventRepository.save(
                        any(PropertyAuditEvent.class)
                )
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        PropertyAuditEvent savedAuditEvent =
                propertyAuditEventRecorder
                        .recordPropertyAction(
                                PROPERTY_ID,
                                PropertyAuditActionCode
                                        .PROPERTY_CREATED,
                                REASON,
                                null,
                                OCCURRED_AT,
                                ownerContext
                        );

        assertThat(savedAuditEvent.getTargetType())
                .isEqualTo("PROPERTY");

        assertThat(savedAuditEvent.getTargetId())
                .isEqualTo(PROPERTY_ID.toString());

        assertThat(savedAuditEvent.getActionCode())
                .isEqualTo(
                        PropertyAuditActionCode
                                .PROPERTY_CREATED
                );

        assertThat(savedAuditEvent.getActorMemberId())
                .isEqualTo(MEMBER_ID);

        assertThat(savedAuditEvent.getActorRole())
                .isEqualTo(ActorRole.USER);

        assertThat(savedAuditEvent.getActionSource())
                .isEqualTo(ActionSource.MEMBER);

        assertThat(savedAuditEvent.getReason())
                .isEqualTo(REASON);

        assertThat(savedAuditEvent.getTraceId())
                .isEqualTo(TRACE_ID);

        assertThat(savedAuditEvent.getClientIpHash())
                .isNull();

        assertThat(savedAuditEvent.getOccurredAt())
                .isEqualTo(OCCURRED_AT);

        verify(propertyAuditEventRepository)
                .save(same(savedAuditEvent));
    }

    @Test
    void recordAction_systemAction_savesWithoutMemberInformation() {
        String systemTraceId =
                "property-audit-system-test";

        String clientIpHash =
                "0123456789abcdef"
                        + "0123456789abcdef"
                        + "0123456789abcdef"
                        + "0123456789abcdef";

        ActorContext systemContext =
                ActorContext.system(systemTraceId);

        when(
                propertyAuditEventRepository.save(
                        any(PropertyAuditEvent.class)
                )
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        PropertyAuditEvent savedAuditEvent =
                propertyAuditEventRecorder.recordAction(
                        "EXACT_LOCATION",
                        PROPERTY_ID.toString(),
                        "VIEW_EXACT_LOCATION",
                        "정확 위치 조회 감사 테스트",
                        clientIpHash,
                        OCCURRED_AT,
                        systemContext
                );

        assertThat(savedAuditEvent.getTargetType())
                .isEqualTo("EXACT_LOCATION");

        assertThat(savedAuditEvent.getTargetId())
                .isEqualTo(PROPERTY_ID.toString());

        assertThat(savedAuditEvent.getActionCode())
                .isEqualTo("VIEW_EXACT_LOCATION");

        assertThat(savedAuditEvent.getActorMemberId())
                .isNull();

        assertThat(savedAuditEvent.getActorRole())
                .isNull();

        assertThat(savedAuditEvent.getActionSource())
                .isEqualTo(ActionSource.SYSTEM);

        assertThat(savedAuditEvent.getTraceId())
                .isEqualTo(systemTraceId);

        assertThat(savedAuditEvent.getClientIpHash())
                .isEqualTo(clientIpHash);

        assertThat(savedAuditEvent.getOccurredAt())
                .isEqualTo(OCCURRED_AT);

        verify(propertyAuditEventRepository)
                .save(same(savedAuditEvent));
    }

    @Test
    void recordPropertyAction_invalidPropertyId_throwsBeforeSaving() {
        assertThatThrownBy(
                () -> propertyAuditEventRecorder
                        .recordPropertyAction(
                                0L,
                                PropertyAuditActionCode
                                        .PROPERTY_CREATED,
                                REASON,
                                null,
                                OCCURRED_AT,
                                ownerContext
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "감사 대상 propertyId는 0보다 커야 합니다."
                );

        verify(propertyAuditEventRepository, never())
                .save(any(PropertyAuditEvent.class));
    }
}