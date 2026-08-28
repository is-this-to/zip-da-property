package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.entity.PropertyFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyFavoriteRepository
        extends JpaRepository<PropertyFavorite, Long> {

    Optional<PropertyFavorite> findByMemberIdAndPropertyIdAndDeletedAtIsNull(
            Long memberId,
            Long propertyId
    );
}