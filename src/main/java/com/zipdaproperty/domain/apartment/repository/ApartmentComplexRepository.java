package com.zipdaproperty.domain.apartment.repository;

import com.zipdaproperty.domain.apartment.entity.ApartmentComplex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApartmentComplexRepository extends JpaRepository<ApartmentComplex, Long> {

    Optional<ApartmentComplex> findByApartmentComplexIdAndIsActiveTrueAndDeletedAtIsNull(
            Long apartmentComplexId
    );
}
