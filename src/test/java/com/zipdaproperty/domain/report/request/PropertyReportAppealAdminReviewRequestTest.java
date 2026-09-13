package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.AppealStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAppealAdminReviewRequestTest {

    private static final ValidatorFactory FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void inReview_withoutReason_isValid() {
        PropertyReportAppealAdminReviewRequest request =
                new PropertyReportAppealAdminReviewRequest(
                        AppealStatus.IN_REVIEW,
                        null,
                        0L
                );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = AppealStatus.class, names = {"ACCEPTED", "REJECTED"})
    void finalStatus_withReason_isValid(AppealStatus targetStatus) {
        PropertyReportAppealAdminReviewRequest request =
                new PropertyReportAppealAdminReviewRequest(
                        targetStatus,
                        "최종 처리 사유",
                        1L
                );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void accepted_withoutReason_isInvalid(String reason) {
        PropertyReportAppealAdminReviewRequest request =
                new PropertyReportAppealAdminReviewRequest(
                        AppealStatus.ACCEPTED,
                        reason,
                        1L
                );

        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }
}
