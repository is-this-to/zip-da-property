package com.zipdaproperty.domain.favorite.repository;

import com.zipdaproperty.domain.favorite.entity.PropertyFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyFavoriteRepository
        extends JpaRepository<PropertyFavorite, Long> {

    Optional<PropertyFavorite> findByMemberIdAndPropertyId(
            Long memberId,
            Long propertyId
    );

    long countByPropertyId(Long propertyId);

}
