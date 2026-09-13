package com.zipdaproperty.domain.report.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAppealTest {

    @Test
    void reviewReason_usesErdLengthLimit() throws Exception {
        Field field = PropertyReportAppeal.class
                .getDeclaredField("reviewReason");

        assertThat(field.getAnnotation(Column.class).length())
                .isEqualTo(1000);
    }
}
