package com.zipdaproperty.domain.property.member.client.response;

public record MemberPermissionApiResponse(
        String code,
        String message,
        PermissionData data,
        String traceId
) {
    public record PermissionData(
            Boolean allowed,
            String reason
    ) {
    }
}
