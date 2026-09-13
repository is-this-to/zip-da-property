package com.zipdaproperty.domain.file.repository;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyFileReportEvidenceRepositoryTest {

    @Test
    void findAllForReportEvidenceLink_locksActiveFilesInStableOrder()
            throws Exception {
        Method method = PropertyFileRepository.class.getDeclaredMethod(
                "findAllForReportEvidenceLink",
                Collection.class
        );
        Lock lock = method.getAnnotation(Lock.class);
        Query query = method.getAnnotation(Query.class);
        String normalizedQuery = query.value()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(normalizedQuery)
                .contains("propertyfile.propertyfileid in :propertyfileids")
                .contains("propertyfile.deletedat is null")
                .contains("order by propertyfile.propertyfileid asc");
    }
}
