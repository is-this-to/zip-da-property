package com.zipdaproperty.domain.property.member.repository;

import com.zipdaproperty.domain.property.member.entity.PropertyMemberState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyMemberStateRepository
        extends JpaRepository<PropertyMemberState, Long> {
}
