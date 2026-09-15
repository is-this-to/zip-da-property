package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActionSource;
import com.zipdaproperty.global.context.constant.ActorRole;
import jakarta.persistence.Column;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyReportActionTest {

    private static final Long REPORT_ID = 884685586571263701L;
    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long ADMIN_ID = 3001L;
    private static final String REASON = "허위 매물 신고 확인";
    private static final String TRACE_ID = "property-report-action-test";
    private static final Instant EXECUTED_AT =
            Instant.parse("2026-09-13T12:00:00Z");

    private final ActorContext adminContext = ActorContext.member(
            ADMIN_ID,
            ActorRole.CS_ADMIN,
            TRACE_ID
    );

    @Test
    void actionCodes_areExactlyTheApprovedOperationalActions() {
        assertThat(ReportActionCode.values()).containsExactly(
                ReportActionCode.HIDE_PROPERTY,
                ReportActionCode.RESTORE_PROPERTY,
                ReportActionCode.REQUEST_CORRECTION,
                ReportActionCode.REQUEST_MEMBER_SANCTION
        );
    }

    @Test
    void create_validInput_capturesActionAndActorAuditInformation() {
        PropertyReportAction action = PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                REASON,
                EXECUTED_AT,
                adminContext
        );

        assertThat(action.getActionId()).isNull();
        assertThat(action.getReportId()).isEqualTo(REPORT_ID);
        assertThat(action.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(action.getActionCode())
                .isEqualTo(ReportActionCode.HIDE_PROPERTY);
        assertThat(action.getReason()).isEqualTo(REASON);
        assertThat(action.getActorMemberId()).isEqualTo(ADMIN_ID);
        assertThat(action.getActorRole()).isEqualTo(ActorRole.CS_ADMIN);
        assertThat(action.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(action.getExecutedAt()).isEqualTo(EXECUTED_AT);
        assertThat(action.getActionSource()).isEqualTo(ActionSource.MEMBER);
        assertThat(action.getCreatedByMemberId()).isEqualTo(ADMIN_ID);
        assertThat(action.getCreatedByRole()).isEqualTo(ActorRole.CS_ADMIN);
    }

    @ParameterizedTest
    @EnumSource(ReportActionCode.class)
    void create_eachActionCode_preservesActionCode(ReportActionCode actionCode) {
        PropertyReportAction action = PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                actionCode,
                REASON,
                EXECUTED_AT,
                adminContext
        );

        assertThat(action.getActionCode()).isEqualTo(actionCode);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L})
    void create_nonPositiveReportId_throwsIllegalArgument(long reportId) {
        assertThatThrownBy(() -> PropertyReportAction.create(
                reportId,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                REASON,
                EXECUTED_AT,
                adminContext
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L})
    void create_nonPositivePropertyId_throwsIllegalArgument(long propertyId) {
        assertThatThrownBy(() -> PropertyReportAction.create(
                REPORT_ID,
                propertyId,
                ReportActionCode.HIDE_PROPERTY,
                REASON,
                EXECUTED_AT,
                adminContext
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_missingActionCode_throwsNullPointer() {
        assertThatThrownBy(() -> PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                null,
                REASON,
                EXECUTED_AT,
                adminContext
        )).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void create_missingReason_throwsIllegalArgument(String reason) {
        assertThatThrownBy(() -> PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                reason,
                EXECUTED_AT,
                adminContext
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_reasonOverMaximumLength_throwsIllegalArgument() {
        assertThatThrownBy(() -> PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                "가".repeat(1001),
                EXECUTED_AT,
                adminContext
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_missingExecutedAt_throwsNullPointer() {
        assertThatThrownBy(() -> PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                REASON,
                null,
                adminContext
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_missingActorContext_throwsNullPointer() {
        assertThatThrownBy(() -> PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                REASON,
                EXECUTED_AT,
                null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_actorWithoutMemberIdentity_throwsIllegalArgument() {
        assertThatThrownBy(() -> PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                REASON,
                EXECUTED_AT,
                ActorContext.system(TRACE_ID)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapping_isAppendOnly() {
        assertThat(PropertyReportAction.class)
                .hasAnnotation(Immutable.class);

        assertThat(Arrays.stream(PropertyReportAction.class.getDeclaredFields())
                .map(field -> field.getAnnotation(Column.class))
                .filter(column -> column != null)
                .allMatch(column -> !column.updatable()))
                .isTrue();

        assertThat(Arrays.stream(PropertyReportAction.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> !name.startsWith("get")))
                .isEmpty();
    }
}
