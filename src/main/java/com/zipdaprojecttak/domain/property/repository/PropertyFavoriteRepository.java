package com.zipdaprojecttak.domain.property.repository;

import com.zipdaprojecttak.domain.property.entity.PropertyFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyFavoriteRepository extends JpaRepository<PropertyFavorite,Long> {
    // 특정 회원이 특정 매물을 현재 찜하고 있는지 조회
    Optional<PropertyFavorite> findMyMemberIdAndPropertyIdAndDeletedAtIsNull(
            Long memberId,
            Long propertyId
    );

    long countByPropertyIdAndDeletedAtIsNull(Long propertyId);  // 활성 찜 개수
    List<PropertyFavorite> findAllByMemberIdAndDeletedAtIsNull(Long memberId); // 활성 찜 목록


}
