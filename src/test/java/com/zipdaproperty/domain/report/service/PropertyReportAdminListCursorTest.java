package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyReportAdminListCursorTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-13T01:02:03.456789Z");
    private static final Long REPORT_ID = 884685586571263701L;

    @Test
    void encodeAndDecode_restoresCreatedAtAndReportId() {
        String encoded = PropertyReportAdminListCursor.from(CREATED_AT, REPORT_ID).encode();

        PropertyReportAdminListCursor decoded = PropertyReportAdminListCursor.decode(encoded);

        assertThat(decoded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(decoded.reportId()).isEqualTo(REPORT_ID);
    }

    @Test
    void decode_null_returnsFirstPageCursor() {
        PropertyReportAdminListCursor cursor = PropertyReportAdminListCursor.decode(null);

        assertThat(cursor.createdAt()).isNull();
        assertThat(cursor.reportId()).isNull();
        assertThat(cursor.encode()).isNull();
    }

    @Test
    void decode_invalidCursor_throwsInvalidRequest() {
        assertThatThrownBy(() -> PropertyReportAdminListCursor.decode("invalid-cursor"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_REQUEST));
    }
}
