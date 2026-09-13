package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.repository.PropertyReportAdminListQueryRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportAdminListQueryRow;
import com.zipdaproperty.domain.report.request.PropertyReportAdminListRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAdminListResponse;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportAdminListServiceTest {

    private static final Long FIRST_REPORT_ID = 884685586571263702L;
    private static final Long SECOND_REPORT_ID = 884685586571263701L;
    private static final Instant CREATED_AT = Instant.parse("2026-09-13T01:02:03.456789Z");

    private final PropertyReportAdminListQueryRepository repository =
            mock(PropertyReportAdminListQueryRepository.class);
    private final PropertyReportAdminListService service =
            new PropertyReportAdminListService(repository);

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"CS_ADMIN", "SUPER_ADMIN"})
    void findReports_adminRoles_returnReports(ActorRole role) {
        when(repository.findReports(null, null, 21)).thenReturn(List.of(row(FIRST_REPORT_ID)));

        PropertyReportAdminListResponse response = service.findReports(
                new PropertyReportAdminListRequest(null, null),
                ActorContext.member(3001L, role, "report-admin-list-test")
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).reportId()).isEqualTo(FIRST_REPORT_ID);
        assertThat(response.items().get(0).propertyId()).isEqualTo(2001L);
        assertThat(response.items().get(0).reporterMemberId()).isEqualTo(1001L);
        assertThat(response.items().get(0).reasonCode()).isEqualTo(ReportReasonCode.FALSE_INFO);
        assertThat(response.items().get(0).status()).isEqualTo(ReportStatus.REJECTED);
        assertThat(response.items().get(0).riskScore()).isEqualByComparingTo("42.50");
        assertThat(response.items().get(0).assignedAdminId()).isEqualTo(3001L);
        assertThat(response.items().get(0).createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"USER", "AGENT"})
    void findReports_nonAdminRoles_throwForbidden(ActorRole role) {
        ActorContext context = ActorContext.member(1001L, role, "report-admin-list-test");

        assertThatThrownBy(() -> service.findReports(
                new PropertyReportAdminListRequest(null, 20), context
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.FORBIDDEN));
        verifyNoInteractions(repository);
    }

    @Test
    void findReports_sizePlusOne_returnsNextCursorFromLastVisibleRow() {
        when(repository.findReports(null, null, 2))
                .thenReturn(List.of(row(FIRST_REPORT_ID), row(SECOND_REPORT_ID)));

        PropertyReportAdminListResponse response = service.findReports(
                new PropertyReportAdminListRequest(null, 1),
                ActorContext.member(3001L, ActorRole.CS_ADMIN, "report-admin-list-test")
        );

        assertThat(response.items()).extracting(item -> item.reportId())
                .containsExactly(FIRST_REPORT_ID);
        assertThat(response.hasNext()).isTrue();
        PropertyReportAdminListCursor cursor =
                PropertyReportAdminListCursor.decode(response.nextCursor());
        assertThat(cursor.createdAt()).isEqualTo(CREATED_AT);
        assertThat(cursor.reportId()).isEqualTo(FIRST_REPORT_ID);
        verify(repository).findReports(null, null, 2);
    }

    @Test
    void findReports_cursorPage_passesDecodedValuesToRepository() {
        String cursor = PropertyReportAdminListCursor.from(CREATED_AT, FIRST_REPORT_ID).encode();
        when(repository.findReports(CREATED_AT, FIRST_REPORT_ID, 21)).thenReturn(List.of());

        service.findReports(
                new PropertyReportAdminListRequest(cursor, 20),
                ActorContext.member(3001L, ActorRole.SUPER_ADMIN, "report-admin-list-test")
        );

        verify(repository).findReports(CREATED_AT, FIRST_REPORT_ID, 21);
    }

    private PropertyReportAdminListQueryRow row(Long reportId) {
        return new PropertyReportAdminListQueryRow(
                reportId,
                2001L,
                1001L,
                ReportReasonCode.FALSE_INFO,
                ReportStatus.REJECTED,
                new BigDecimal("42.50"),
                3001L,
                CREATED_AT
        );
    }
}
