package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.AppealStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PropertyReportAppealAdminReviewRequest(

        @NotNull(message = "targetStatus는 필수입니다.")
        AppealStatus targetStatus,

        @Size(
                max = 1000,
                message = "reviewReason은 1000자 이하여야 합니다."
        )
        String reviewReason,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version
) {

    @AssertTrue(message = "승인 또는 기각 시 reviewReason은 필수입니다.")
    public boolean isFinalReviewReasonValid() {
        if (targetStatus != AppealStatus.ACCEPTED
                && targetStatus != AppealStatus.REJECTED) {
            return true;
        }
        return reviewReason != null && !reviewReason.isBlank();
    }
}
