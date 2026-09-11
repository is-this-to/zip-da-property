package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PublicationStatusPolicy {

    private static final Map<PublicationStatus, Set<PublicationStatus>>
            ALLOWED_TRANSITIONS =
            Map.of(
                    PublicationStatus.IN_REVIEW,
                    Set.of(
                            PublicationStatus.PUBLISHED,
                            PublicationStatus.REJECTED
                    ),

                    PublicationStatus.PUBLISHED,
                    Set.of(PublicationStatus.HIDDEN),

                    PublicationStatus.HIDDEN,
                    Set.of(PublicationStatus.PUBLISHED),

                    PublicationStatus.REJECTED,
                    Set.of(PublicationStatus.IN_REVIEW)
            );

    public void validateTransition(
            PublicationStatus currentStatus,
            PublicationStatus targetStatus,
            TransactionStatus transactionStatus
    ) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw invalidTransition();
        }

        if (
                currentStatus == PublicationStatus.HIDDEN
                        && targetStatus == PublicationStatus.PUBLISHED
                        && transactionStatus != TransactionStatus.AVAILABLE
        ) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_STATUS_TRANSITION,
                    "거래 가능한 매물만 다시 공개할 수 있습니다."
            );
        }
    }

    public boolean canTransition(
            PublicationStatus currentStatus,
            PublicationStatus targetStatus
    ) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        return ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(targetStatus);
    }

    private BusinessException invalidTransition() {
        return new BusinessException(
                CustomResponseCode.INVALID_STATUS_TRANSITION,
                "허용되지 않은 공개 상태 변경입니다."
        );
    }
}
