package com.zipdaproperty.domain.file.repository;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyFileRepository extends JpaRepository<PropertyFile, Long> {
}
