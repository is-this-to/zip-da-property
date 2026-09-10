package com.zipdaproperty.domain.property.audit.repository;

import com.zipdaproperty.domain.property.audit.entity.PropertyAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyAuditEventRepository
        extends JpaRepository<PropertyAuditEvent, Long> {
}