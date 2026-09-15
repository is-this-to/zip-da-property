package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.ReportStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAdminStatusChangeRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(
                new PropertyReportAdminStatusChangeRequest(ReportStatus.TRIAGED, 0L)
        )).isEmpty();
    }

    @Test
    void missingStatusOrVersion_hasViolations() {
        assertThat(validator.validate(
                new PropertyReportAdminStatusChangeRequest(null, null)
        )).hasSize(2);
    }

    @Test
    void negativeVersion_hasViolation() {
        assertThat(validator.validate(
                new PropertyReportAdminStatusChangeRequest(ReportStatus.TRIAGED, -1L)
        )).isNotEmpty();
    }
}
