package com.zipdaproperty.domain.property.idempotency.constant;

public enum IdempotencyProcessingStatus {

    PROCESSING,  // 해당 키의 요청을 현재 처리중인상태
    COMPLETED, // 요청 처리 완료
    FAILED // 요청 처리 실패
}