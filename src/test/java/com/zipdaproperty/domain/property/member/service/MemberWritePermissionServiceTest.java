package com.zipdaproperty.domain.property.member.service;

import com.zipdaproperty.domain.property.member.client.MemberPermissionClient;
import com.zipdaproperty.domain.property.member.client.MemberPermissionResult;
import com.zipdaproperty.domain.property.member.constant.MemberPermissionAction;
import com.zipdaproperty.domain.property.member.entity.PropertyMemberState;
import com.zipdaproperty.domain.property.member.repository.PropertyMemberStateRepository;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberWritePermissionServiceTest {

    private static final Long MEMBER_ID = 1001L;

    private final MemberPermissionClient permissionClient =
            mock(MemberPermissionClient.class);

    private final PropertyMemberStateRepository memberStateRepository =
            mock(PropertyMemberStateRepository.class);

    private final MemberWritePermissionService service =
            new MemberWritePermissionService(
                    permissionClient,
                    memberStateRepository
            );

    @Test
    void validate_allowedWithoutLocalRestriction_completes() {
        when(permissionClient.getPermission(
                MEMBER_ID,
                MemberPermissionAction.PROPERTY_CREATE
        )).thenReturn(new MemberPermissionResult(true, null));
        when(memberStateRepository.findById(MEMBER_ID))
                .thenReturn(Optional.empty());

        service.validate(
                MEMBER_ID,
                ActorRole.USER,
                MemberPermissionAction.PROPERTY_CREATE
        );

        verify(memberStateRepository).findById(MEMBER_ID);
    }

    @Test
    void validate_memberApiDenied_throwsPermissionDenied() {
        when(permissionClient.getPermission(
                MEMBER_ID,
                MemberPermissionAction.PROPERTY_UPDATE
        )).thenReturn(new MemberPermissionResult(
                false,
                "MEMBER_SANCTIONED"
        ));

        assertDenied(() -> service.validate(
                MEMBER_ID,
                ActorRole.USER,
                MemberPermissionAction.PROPERTY_UPDATE
        ));
    }

    @Test
    void validate_withdrawnLocalState_throwsPermissionDenied() {
        PropertyMemberState state = state();
        state.withdraw("event-2", Instant.parse("2026-09-11T00:01:00Z"), Instant.now());
        prepareAllowedWithState(state);

        assertDenied(() -> service.validate(
                MEMBER_ID,
                ActorRole.USER,
                MemberPermissionAction.PROPERTY_CREATE
        ));
    }

    @Test
    void validate_propertySanctionLocalState_throwsPermissionDenied() {
        PropertyMemberState state = state();
        state.sanction(
                "PROPERTY_WRITE",
                "event-2",
                Instant.parse("2026-09-11T00:01:00Z"),
                Instant.now()
        );
        prepareAllowedWithState(state);

        assertDenied(() -> service.validate(
                MEMBER_ID,
                ActorRole.USER,
                MemberPermissionAction.PROPERTY_CREATE
        ));
    }

    @Test
    void validate_unrelatedSanctionScope_allowsPropertyWrite() {
        PropertyMemberState state = state();
        state.sanction(
                "COMMUNITY_WRITE",
                "event-2",
                Instant.parse("2026-09-11T00:01:00Z"),
                Instant.now()
        );
        prepareAllowedWithState(state);

        service.validate(
                MEMBER_ID,
                ActorRole.USER,
                MemberPermissionAction.PROPERTY_CREATE
        );

        verify(memberStateRepository).findById(MEMBER_ID);
    }

    @Test
    void validate_knownSuspendedAgent_throwsPermissionDenied() {
        PropertyMemberState state = state();
        state.approveAgent(
                501L,
                601L,
                "event-2",
                Instant.parse("2026-09-11T00:01:00Z"),
                Instant.now()
        );
        state.suspendAgent(
                "event-3",
                Instant.parse("2026-09-11T00:02:00Z"),
                Instant.now()
        );
        prepareAllowedWithState(state);

        assertDenied(() -> service.validate(
                MEMBER_ID,
                ActorRole.AGENT,
                MemberPermissionAction.PROPERTY_UPDATE
        ));
    }

    private PropertyMemberState state() {
        return PropertyMemberState.create(
                MEMBER_ID,
                "event-1",
                Instant.parse("2026-09-11T00:00:00Z"),
                Instant.now()
        );
    }

    private void prepareAllowedWithState(
            PropertyMemberState state
    ) {
        when(permissionClient.getPermission(
                MEMBER_ID,
                MemberPermissionAction.PROPERTY_CREATE
        )).thenReturn(new MemberPermissionResult(true, null));
        when(permissionClient.getPermission(
                MEMBER_ID,
                MemberPermissionAction.PROPERTY_UPDATE
        )).thenReturn(new MemberPermissionResult(true, null));
        when(memberStateRepository.findById(MEMBER_ID))
                .thenReturn(Optional.of(state));
    }

    private void assertDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception)
                                .getCustomResponseCode()
                )
                .isEqualTo(
                        CustomResponseCode.MEMBER_PERMISSION_DENIED
                );
    }
}
