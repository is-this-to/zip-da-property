package com.zipdaproperty.domain.property.member.repository;

import com.zipdaproperty.domain.property.member.entity.MemberEventConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberEventConsumptionRepository
        extends JpaRepository<MemberEventConsumption, String> {
}
