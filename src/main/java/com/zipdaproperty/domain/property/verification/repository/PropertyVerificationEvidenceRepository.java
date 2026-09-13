package com.zipdaproperty.domain.property.verification.repository;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerificationEvidence;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PropertyVerificationEvidenceRepository
        extends JpaRepository<PropertyVerificationEvidence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select evidence
            from PropertyVerificationEvidence evidence
            join PropertyVerification verification
              on verification.propertyVerificationId = evidence.propertyVerificationId
            where evidence.deletedAt is null
              and verification.deletedAt is null
              and verification.status in :statuses
              and verification.reviewedAt <= :reviewedBefore
            order by verification.reviewedAt asc,
                     evidence.verificationEvidenceId asc
            """)
    List<PropertyVerificationEvidence> findDueForCleanup(
            @Param("statuses") Collection<PropertyVerificationStatus> statuses,
            @Param("reviewedBefore") Instant reviewedBefore,
            Pageable pageable
    );

    @Query("""
            select distinct evidence.propertyFileId
            from PropertyVerificationEvidence evidence
            where evidence.propertyFileId in :propertyFileIds
              and evidence.deletedAt is null
            """)
    List<Long> findActivelyReferencedFileIds(
            @Param("propertyFileIds") Collection<Long> propertyFileIds
    );
}
