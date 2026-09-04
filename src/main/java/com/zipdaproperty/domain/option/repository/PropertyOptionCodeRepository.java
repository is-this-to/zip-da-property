package com.zipdaproperty.domain.option.repository;

import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyOptionCodeRepository
        extends JpaRepository<PropertyOptionCode, Long> {

    Optional<PropertyOptionCode>
    findByOptionCodeAndDeletedAtIsNullAndActiveTrue(String optionCode);
}