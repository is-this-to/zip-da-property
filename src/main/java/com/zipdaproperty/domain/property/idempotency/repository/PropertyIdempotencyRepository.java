package com.zipdaproperty.domain.property.idempotency.repository;

import com.zipdaproperty.domain.property.idempotency.entity.PropertyIdempotency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PropertyIdempotencyRepository
        extends JpaRepository<PropertyIdempotency, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select idempotency
            from PropertyIdempotency idempotency
            where idempotency.memberId = :memberId
              and idempotency.endpointKey = :endpointKey
              and idempotency.idempotencyKey = :idempotencyKey
              and idempotency.deletedAt is null
            """)
    Optional<PropertyIdempotency>
    findByMemberIdAndEndpointKeyAndIdempotencyKey(
            @Param("memberId") Long memberId,
            @Param("endpointKey") String endpointKey,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select idempotency
            from PropertyIdempotency idempotency
            where idempotency.deletedAt is null
              and idempotency.expiresAt <= :currentTime
            order by idempotency.expiresAt asc,
                     idempotency.idempotencyId asc
            """)
    List<PropertyIdempotency> findDueForCleanup(
            @Param("currentTime") Instant currentTime,
            Pageable pageable
    );
}
