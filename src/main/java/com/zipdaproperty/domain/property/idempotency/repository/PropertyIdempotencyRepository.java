package com.zipdaproperty.domain.property.idempotency.repository;

import com.zipdaproperty.domain.property.idempotency.entity.PropertyIdempotency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PropertyIdempotencyRepository
        extends JpaRepository<PropertyIdempotency, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PropertyIdempotency>
    findByMemberIdAndEndpointKeyAndIdempotencyKey(
            Long memberId,
            String endpointKey,
            String idempotencyKey
    );
}