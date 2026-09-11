package com.zipdaproperty.domain.property.verification.repository;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyVerificationRepository extends JpaRepository<PropertyVerification, Long> {

    boolean existsByPropertyIdAndVerificationTypeAndStatusInAndDeletedAtIsNull(
            Long propertyId,
            PropertyVerificationType verificationType,
            Collection<PropertyVerificationStatus> statuses
    );

    Optional<PropertyVerification> findTopByPropertyIdAndVerificationTypeOrderByVerificationVersionDesc(
            Long propertyId,
            PropertyVerificationType verificationType
    );

    Optional<PropertyVerification> findByPropertyVerificationIdAndPropertyIdAndDeletedAtIsNull(
            Long propertyVerificationId,
            Long propertyId
    );

    @Query("""
            select new com.zipdaproperty.domain.property.verification.repository.PropertyVerificationExpirationCandidate(
                verification.propertyVerificationId,
                verification.propertyId
            )
            from PropertyVerification verification
            where verification.status = :status
              and verification.expiresAt <= :currentTime
              and verification.deletedAt is null
              and exists (
                  select property.propertyId
                  from Property property
                  where property.propertyId = verification.propertyId
                    and property.deletedAt is null
              )
            order by verification.expiresAt asc,
                     verification.propertyVerificationId asc
            """)
    List<PropertyVerificationExpirationCandidate> findDueForExpiration(
            @Param("status") PropertyVerificationStatus status,
            @Param("currentTime") Instant currentTime,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select verification
            from PropertyVerification verification
            where verification.propertyVerificationId = :verificationId
              and verification.deletedAt is null
            """)
    Optional<PropertyVerification> findForExpiration(
            @Param("verificationId") Long verificationId
    );
}
