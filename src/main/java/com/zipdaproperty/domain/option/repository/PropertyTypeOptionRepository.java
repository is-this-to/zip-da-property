package com.zipdaproperty.domain.option.repository;

import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.property.constant.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyTypeOptionRepository
        extends JpaRepository<PropertyTypeOption, Long> {

    List<PropertyTypeOption>
    findAllByPropertyTypeAndDeletedAtIsNullAndOptionCodeDeletedAtIsNullAndOptionCodeActiveTrueOrderByDisplayOrderAsc(
            PropertyType propertyType
    );
}