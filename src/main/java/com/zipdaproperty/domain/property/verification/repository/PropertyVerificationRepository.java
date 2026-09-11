package com.zipdaproperty.domain.property.verification.repository;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
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
}
