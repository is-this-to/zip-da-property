package com.zipdaproperty.domain.property.constant;

public enum VerificationStatus {
    UNVERIFIED,   // 미인증
    IN_REVIEW,   // 인증 검토 중
    OWNER_VERIFIED,   // 소유자 인증
    AGENT_VERIFIED,   //중개사 인증
    REJECTED,   // 인증 거절
    EXPIRED   // 인증 만료
}