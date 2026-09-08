package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.entity.PropertyAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyAddressRepository  extends JpaRepository<PropertyAddress, Long> {

    /**
     * 매물 ID를 기준으로 삭제되지 않은 현재 주소를 조회한다.
     */
    Optional<PropertyAddress> findByProperty_PropertyIdAndDeletedAtIsNull(
            Long propertyId
    );

    /**
     * 특정 매물의 활성 주소가 존재하는지 확인한다.
     */
    boolean existsByProperty_PropertyIdAndDeletedAtIsNull(
            Long propertyId
    );

    /**
     * 필터가 비활성화된 상태에서 삭제된 주소까지 조회할 때 사용한다.
     *
     * 일반 호출에서는 softDelete 필터가 적용되므로 삭제 행은 보이지 않는다.
     */
    Optional<PropertyAddress> findByProperty_PropertyId(
            Long propertyId
    );
}
