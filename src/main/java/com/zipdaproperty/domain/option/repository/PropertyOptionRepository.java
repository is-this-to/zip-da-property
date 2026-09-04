package com.zipdaproperty.domain.option.repository;

import com.zipdaproperty.domain.option.entity.PropertyOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyOptionRepository
        extends JpaRepository<PropertyOption, Long> {

    List<PropertyOption>
    findAllByProperty_PropertyIdAndDeletedAtIsNullOrderByDisplayOrderAsc(
            Long propertyId
    );

    Optional<PropertyOption>
    findByProperty_PropertyIdAndOptionCode_OptionCodeIdAndDeletedAtIsNull(
            Long propertyId,
            Long optionCodeId
    );
}