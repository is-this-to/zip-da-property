package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.type.AppealStatus;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyReportAppealAdminTransitionPolicyTest {

    private final PropertyReportAppealAdminTransitionPolicy policy =
            new PropertyReportAppealAdminTransitionPolicy();

    @Test
    void validate_allowsDefinedTransitions() {
        assertThatCode(() -> policy.validate(
                AppealStatus.SUBMITTED,
                AppealStatus.IN_REVIEW
        )).doesNotThrowAnyException();
        assertThatCode(() -> policy.validate(
                AppealStatus.IN_REVIEW,
                AppealStatus.ACCEPTED
        )).doesNotThrowAnyException();
        assertThatCode(() -> policy.validate(
                AppealStatus.IN_REVIEW,
                AppealStatus.REJECTED
        )).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsUndefinedTransition() {
        assertThatThrownBy(() -> policy.validate(
                AppealStatus.SUBMITTED,
                AppealStatus.ACCEPTED
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.APPEAL_NOT_ALLOWED)
        );
    }
}
