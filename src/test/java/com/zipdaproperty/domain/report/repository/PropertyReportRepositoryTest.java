package com.zipdaproperty.domain.report.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportRepositoryTest {

    @Test
    void countDailyReportsIncludingDeleted_usesDatabaseCurrentDateAndCountsDeletedRows()
            throws Exception {
        Method method = PropertyReportRepository.class.getDeclaredMethod(
                "countDailyReportsIncludingDeleted",
                Long.class
        );
        Query query = method.getAnnotation(Query.class);
        String normalizedQuery = query.value()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        assertThat(query.nativeQuery()).isTrue();
        assertThat(normalizedQuery).contains(
                "created_at >= current_date",
                "created_at < current_date + interval 1 day"
        );
        assertThat(normalizedQuery).doesNotContain("deleted_at");
    }
}
