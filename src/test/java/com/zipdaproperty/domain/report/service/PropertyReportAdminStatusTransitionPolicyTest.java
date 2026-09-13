package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyReportAdminStatusTransitionPolicyTest {

    private final PropertyReportAdminStatusTransitionPolicy policy =
            new PropertyReportAdminStatusTransitionPolicy();

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void validate_allowedTransition_succeeds(ReportStatus current, ReportStatus target) {
        assertThatCode(() -> policy.validate(current, target)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void validate_sameReverseSkippedOrTerminalTransition_throwsConflict(
            ReportStatus current,
            ReportStatus target
    ) {
        assertThatThrownBy(() -> policy.validate(current, target))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_REPORT_TRANSITION));
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.TRIAGED),
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.REJECTED),
                Arguments.of(ReportStatus.TRIAGED, ReportStatus.IN_REVIEW),
                Arguments.of(ReportStatus.TRIAGED, ReportStatus.REJECTED),
                Arguments.of(ReportStatus.IN_REVIEW, ReportStatus.ACTIONED),
                Arguments.of(ReportStatus.IN_REVIEW, ReportStatus.REJECTED),
                Arguments.of(ReportStatus.ACTIONED, ReportStatus.CLOSED)
        );
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.RECEIVED),
                Arguments.of(ReportStatus.RECEIVED, ReportStatus.IN_REVIEW),
                Arguments.of(ReportStatus.TRIAGED, ReportStatus.RECEIVED),
                Arguments.of(ReportStatus.IN_REVIEW, ReportStatus.TRIAGED),
                Arguments.of(ReportStatus.ACTIONED, ReportStatus.REJECTED),
                Arguments.of(ReportStatus.REJECTED, ReportStatus.RECEIVED),
                Arguments.of(ReportStatus.REJECTED, ReportStatus.REJECTED),
                Arguments.of(ReportStatus.CLOSED, ReportStatus.ACTIONED),
                Arguments.of(ReportStatus.CLOSED, ReportStatus.CLOSED)
        );
    }
}
