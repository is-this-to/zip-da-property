package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.entity.PropertyRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRevisionRepository
        extends JpaRepository<PropertyRevision, Long> {

    Optional<PropertyRevision> findByPropertyIdAndPropertyVersion(
            Long propertyId,
            Long propertyVersion
    );

    Optional<PropertyRevision> findTopByPropertyIdOrderByPropertyVersionDesc(
            Long propertyId
    );
}