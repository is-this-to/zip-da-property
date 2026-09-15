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

class PropertyReportMyListQueryRepositoryTest {

    @Test
    void findMyReports_buildsOwnerSoftDeleteKeysetAndTieBreakQuery() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class, RETURNS_SELF);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        PropertyReportMyListQueryRepository repository = new PropertyReportMyListQueryRepository(
                new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
        );
        Instant createdAt = Instant.parse("2026-09-13T01:02:03.456789Z");

        repository.findMyReports(1001L, createdAt, 884685586571263701L, 21);

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(jpql.capture());
        assertThat(jpql.getValue())
                .contains("propertyReport.reporterMemberId = ?1")
                .contains("propertyReport.deletedAt is null")
                .contains("propertyReport.createdAt < ?2")
                .contains("propertyReport.createdAt = ?3 and propertyReport.reportId < ?4")
                .contains("order by propertyReport.createdAt desc, propertyReport.reportId desc");
        verify(query).setMaxResults(21);
    }
}
