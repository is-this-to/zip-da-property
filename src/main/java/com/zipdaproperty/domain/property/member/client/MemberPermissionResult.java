package com.zipdaproperty.domain.property.member.client;

public record MemberPermissionResult(
        boolean allowed,
        String reason
) {
}
