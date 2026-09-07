package com.zipdaproperty.domain.option.repository;

import com.zipdaproperty.domain.option.entity.PropertyOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyOptionRepository extends JpaRepository<PropertyOption, Long> {
}
