package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class TransactionStatusPolicy {

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    TransactionStatus.AVAILABLE,
                    Set.of(TransactionStatus.RESERVED),

                    TransactionStatus.RESERVED,
                    Set.of(
                            TransactionStatus.AVAILABLE,
                            TransactionStatus.COMPLETED
                    ),

                    TransactionStatus.COMPLETED,
                    Set.of()
            );

    public void validateTransition(
            TransactionStatus currentStatus,
            TransactionStatus targetStatus
    ) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_STATUS_TRANSITION,
                    "허용되지 않은 거래 상태 변경입니다."
            );
        }
    }

    public boolean canTransition(
            TransactionStatus currentStatus,
            TransactionStatus targetStatus
    ) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        return ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(targetStatus);
    }
}