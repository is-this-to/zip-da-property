package com.zipdaproperty.domain.region.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.region.constant.RegionType;
import com.zipdaproperty.domain.region.response.RegionSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.zipdaproperty.domain.region.entity.QRegion.region;

@Repository
@RequiredArgsConstructor
public class RegionQueryDSLRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public List<RegionSummaryResponse> findRootRegions(){
        return jpaQueryFactory
                // DB에서 조회된 데이터를 DTO로 바로 가져가는 역할
                .select(Projections.constructor(
                        RegionSummaryResponse.class,
                        region.regionId,
                        region.regionCode,
                        region.regionName,
                        region.regionLevel,
                        region.regionType,
                        region.hasChildren
                ))
                .from(region)
                .where(
                        region.parentRegionId.isNull(),
                        region.regionLevel.eq(1),
                        region.regionType.eq(RegionType.SIDO),
                        region.isActive.isTrue()
                )
                .orderBy(region.regionName.asc())
                .fetch();
    }
}
