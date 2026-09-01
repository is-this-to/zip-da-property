package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.entity.PropertyPublisherSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyPublisherSnapshotRepository
        extends JpaRepository<PropertyPublisherSnapshot, Long> {

    Optional<PropertyPublisherSnapshot> findByPropertyRevisionId(
            Long propertyRevisionId
    );

    Optional<PropertyPublisherSnapshot> findTopByPropertyIdOrderByCapturedAtDesc(
            Long propertyId
    );
}