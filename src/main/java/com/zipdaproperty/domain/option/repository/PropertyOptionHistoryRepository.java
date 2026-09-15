package com.zipdaproperty.domain.option.repository;

import com.zipdaproperty.domain.option.entity.PropertyOptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyOptionHistoryRepository
        extends JpaRepository<PropertyOptionHistory, Long> {
}