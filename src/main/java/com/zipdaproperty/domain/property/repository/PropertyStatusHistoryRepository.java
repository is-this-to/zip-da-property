package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.entity.PropertyStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyStatusHistoryRepository
        extends JpaRepository<PropertyStatusHistory, Long> {

    Optional<PropertyStatusHistory> findByPropertyRevisionIdAndStatusType(
            Long propertyRevisionId,
            PropertyStatusType statusType
    );

    List<PropertyStatusHistory> findAllByPropertyIdOrderByOccurredAtDesc(
            Long propertyId
    );
}