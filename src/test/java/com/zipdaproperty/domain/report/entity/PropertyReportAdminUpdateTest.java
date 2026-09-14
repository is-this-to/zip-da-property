package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyReportAdminUpdateTest {

    private final ActorContext creator =
            ActorContext.member(1001L, ActorRole.USER, "report-create-test");
    private final ActorContext admin =
            ActorContext.member(3001L, ActorRole.CS_ADMIN, "report-admin-update-test");

    @Test
    void assignAdmin_changesAssigneeAndRecordsUpdater() {
        PropertyReport report = report();

        report.assignAdmin(3002L, admin);

        assertThat(report.getAssignedAdminId()).isEqualTo(3002L);
        assertThat(report.getUpdatedByMemberId()).isEqualTo(3001L);
        assertThat(report.getUpdatedByRole()).isEqualTo(ActorRole.CS_ADMIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "100.00"})
    void changeRiskScore_acceptsBoundaryValues(String value) {
        PropertyReport report = report();

        report.changeRiskScore(new BigDecimal(value), admin);

        assertThat(report.getRiskScore()).isEqualByComparingTo(value);
        assertThat(report.getUpdatedByMemberId()).isEqualTo(3001L);
        assertThat(report.getUpdatedByRole()).isEqualTo(ActorRole.CS_ADMIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01"})
    void changeRiskScore_rejectsOutOfRangeValues(String value) {
        PropertyReport report = report();

        assertThatThrownBy(() -> report.changeRiskScore(new BigDecimal(value), admin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PropertyReport report() {
        return PropertyReport.create(
                884685586571263701L,
                884700000000000001L,
                1001L,
                ReportReasonCode.FALSE_INFO,
                "허위 매물 신고",
                creator
        );
    }
}
