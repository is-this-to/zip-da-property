package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.repository.PropertyReportMyListQueryRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportMyListQueryRow;
import com.zipdaproperty.domain.report.request.PropertyReportMyListRequest;
import com.zipdaproperty.domain.report.response.PropertyReportMyListResponse;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyReportMyListServiceTest {

    private static final Long MEMBER_ID = 1001L;
    private static final Long FIRST_REPORT_ID = 884685586571263702L;
    private static final Long SECOND_REPORT_ID = 884685586571263701L;
    private static final Instant CREATED_AT = Instant.parse("2026-09-13T01:02:03.456789Z");

    private final PropertyReportMyListQueryRepository repository =
            mock(PropertyReportMyListQueryRepository.class);
    private final PropertyReportMyListService service = new PropertyReportMyListService(repository);
    private final ActorContext actorContext =
            ActorContext.member(MEMBER_ID, ActorRole.USER, "report-my-list-test");

    @Test
    void findMyReports_emptyResult_returnsEmptyPageShape() {
        when(repository.findMyReports(MEMBER_ID, null, null, 21)).thenReturn(List.of());

        PropertyReportMyListResponse response = service.findMyReports(
                new PropertyReportMyListRequest(null, null), actorContext
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
        verify(repository).findMyReports(MEMBER_ID, null, null, 21);
    }

    @Test
    void findMyReports_includesRejectedAndClosedAndMapsOnlyResponseFields() {
        PropertyReportMyListQueryRow rejected = row(FIRST_REPORT_ID, ReportStatus.REJECTED);
        PropertyReportMyListQueryRow closed = row(SECOND_REPORT_ID, ReportStatus.CLOSED);
        when(repository.findMyReports(MEMBER_ID, null, null, 21))
                .thenReturn(List.of(rejected, closed));

        PropertyReportMyListResponse response = service.findMyReports(
                new PropertyReportMyListRequest(null, 20), actorContext
        );

        assertThat(response.items()).extracting("reportId", "propertyId", "reasonCode", "status", "createdAt")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                FIRST_REPORT_ID, 2001L, ReportReasonCode.FALSE_INFO,
                                ReportStatus.REJECTED, CREATED_AT
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                SECOND_REPORT_ID, 2001L, ReportReasonCode.FALSE_INFO,
                                ReportStatus.CLOSED, CREATED_AT
                        )
                );
        assertThat(response.items().get(0).getClass().getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("reportId", "propertyId", "reasonCode", "status", "createdAt");
    }

    @Test
    void findMyReports_sizePlusOne_returnsNextCursorFromLastVisibleRow() {
        PropertyReportMyListQueryRow first = row(FIRST_REPORT_ID, ReportStatus.RECEIVED);
        PropertyReportMyListQueryRow extra = row(SECOND_REPORT_ID, ReportStatus.TRIAGED);
        when(repository.findMyReports(MEMBER_ID, null, null, 2))
                .thenReturn(List.of(first, extra));

        PropertyReportMyListResponse response = service.findMyReports(
                new PropertyReportMyListRequest(null, 1), actorContext
        );

        assertThat(response.items()).extracting(item -> item.reportId())
                .containsExactly(FIRST_REPORT_ID);
        assertThat(response.hasNext()).isTrue();
        PropertyReportMyListCursor cursor = PropertyReportMyListCursor.decode(response.nextCursor());
        assertThat(cursor.createdAt()).isEqualTo(CREATED_AT);
        assertThat(cursor.reportId()).isEqualTo(FIRST_REPORT_ID);
    }

    @Test
    void findMyReports_cursorPage_passesMemberAndTieBreakValuesToRepository() {
        String cursor = PropertyReportMyListCursor.from(CREATED_AT, FIRST_REPORT_ID).encode();
        when(repository.findMyReports(MEMBER_ID, CREATED_AT, FIRST_REPORT_ID, 51))
                .thenReturn(List.of());

        service.findMyReports(new PropertyReportMyListRequest(cursor, 50), actorContext);

        verify(repository).findMyReports(MEMBER_ID, CREATED_AT, FIRST_REPORT_ID, 51);
    }

    private PropertyReportMyListQueryRow row(Long reportId, ReportStatus status) {
        return new PropertyReportMyListQueryRow(
                reportId,
                2001L,
                ReportReasonCode.FALSE_INFO,
                status,
                CREATED_AT
        );
    }
}
