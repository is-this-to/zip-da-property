package com.zipdaproperty.domain.report.repository;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyReportAdminDetailQueryRepositoryTest {

    @Test
    void findReport_filtersByReportIdAndExcludesSoftDeletedReport() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class, RETURNS_SELF);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        PropertyReportAdminDetailQueryRepository repository =
                new PropertyReportAdminDetailQueryRepository(
                        new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
                );

        assertThat(repository.findReport(884685586571263701L)).isEmpty();

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(jpql.capture());
        assertThat(jpql.getValue())
                .contains("propertyReport.reportId = ?1")
                .contains("propertyReport.deletedAt is null");
    }
}
