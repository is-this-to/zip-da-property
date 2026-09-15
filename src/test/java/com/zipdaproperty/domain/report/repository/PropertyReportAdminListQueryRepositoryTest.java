package com.zipdaproperty.domain.report.repository;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyReportAdminListQueryRepositoryTest {

    @Test
    void findReports_buildsSoftDeleteKeysetAndTieBreakQueryWithoutStatusFilter() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class, RETURNS_SELF);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        PropertyReportAdminListQueryRepository repository =
                new PropertyReportAdminListQueryRepository(
                        new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
                );
        Instant createdAt = Instant.parse("2026-09-13T01:02:03.456789Z");

        repository.findReports(createdAt, 884685586571263701L, 21);

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(jpql.capture());
        assertThat(jpql.getValue())
                .contains("propertyReport.deletedAt is null")
                .contains("propertyReport.createdAt < ?1")
                .contains("propertyReport.createdAt = ?2 and propertyReport.reportId < ?3")
                .contains("order by propertyReport.createdAt desc, propertyReport.reportId desc")
                .doesNotContain("propertyReport.status =")
                .doesNotContain("propertyReport.reasonCode =")
                .doesNotContain("propertyReport.propertyId =");
        verify(query).setMaxResults(21);
    }
}
