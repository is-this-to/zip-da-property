package com.zipdaproperty.domain.property.member.service;

import com.zipdaproperty.domain.property.member.client.MemberPermissionClient;
import com.zipdaproperty.domain.property.member.client.MemberPermissionResult;
import com.zipdaproperty.domain.property.member.constant.MemberPermissionAction;
import com.zipdaproperty.domain.property.member.entity.PropertyMemberState;
import com.zipdaproperty.domain.property.member.repository.PropertyMemberStateRepository;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MemberWritePermissionService {

    private final MemberPermissionClient memberPermissionClient;
    private final PropertyMemberStateRepository memberStateRepository;

    public void validate(
            Long memberId,
            ActorRole actorRole,
            MemberPermissionAction action
    ) {
        MemberPermissionResult permission =
                memberPermissionClient.getPermission(
                        memberId,
                        action
                );

        if (!permission.allowed()) {
            throw denied();
        }

        memberStateRepository
                .findById(memberId)
                .ifPresent(state -> validateLocalState(
                        state,
                        actorRole,
                        action
                ));
    }

    private void validateLocalState(
            PropertyMemberState state,
            ActorRole actorRole,
            MemberPermissionAction action
    ) {
        if (state.isWithdrawn()
                || isSanctionedFor(state, action)
                || isKnownSuspendedAgent(state, actorRole)) {
            throw denied();
        }
    }

    private boolean isSanctionedFor(
            PropertyMemberState state,
            MemberPermissionAction action
    ) {
        if (!state.isSanctioned()) {
            return false;
        }

        String scope = state.getSanctionScope();
        if (scope == null || scope.isBlank()) {
            return true;
        }

        String normalizedScope = scope
                .trim()
                .toUpperCase(Locale.ROOT);

        return normalizedScope.equals("ALL")
                || normalizedScope.equals("PROPERTY")
                || normalizedScope.equals("PROPERTY_WRITE")
                || normalizedScope.equals(action.name());
    }

    private boolean isKnownSuspendedAgent(
            PropertyMemberState state,
            ActorRole actorRole
    ) {
        return actorRole == ActorRole.AGENT
                && state.getAgentId() != null
                && !state.isAgentActive();
    }

    private BusinessException denied() {
        return new BusinessException(
                CustomResponseCode.MEMBER_PERMISSION_DENIED,
                "Member 정책에 따라 요청한 매물 작업을 수행할 수 없습니다."
        );
    }
}
