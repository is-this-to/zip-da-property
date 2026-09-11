package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.entity.Property;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository
        extends JpaRepository<Property, Long> {

    Optional<Property> findByPropertyIdAndDeletedAtIsNull(
            Long propertyId
    );

    Optional<Property>
    findByPropertyIdAndAuthorMemberIdAndDeletedAtIsNull(
            Long propertyId,
            Long authorMemberId
    );

    Optional<Property> findByPropertyIdAndDeletedAtIsNotNull(
            Long propertyId
    );

    List<Property> findAllByAuthorMemberIdAndDeletedAtIsNull(
            Long authorMemberId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select property
            from Property property
            where property.propertyId = :propertyId
              and property.deletedAt is null
            """)
    Optional<Property> findForVerificationExpiration(
            @Param("propertyId") Long propertyId
    );
}
