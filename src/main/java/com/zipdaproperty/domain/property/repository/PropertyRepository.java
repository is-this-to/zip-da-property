package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findByPropertyIdAndDeletedAtIsNull(
            Long propertyId
    );

    Optional<Property> findByPropertyIdAndAuthorMemberIdAndDeletedAtIsNull(
            Long propertyId,
            Long authorMemberId
    );
}