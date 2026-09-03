package com.zipdaproperty.domain.region.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
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

    /*
     *  상위 지역이 없는 활성 시·도 목록을 조회한다.
     *
     *  - parentRegionId IS NULL: 최상위 지역
     *  - regionLevel = 1: 시·도 계층
     *  - regionType = SIDO: 시·도 유형
     *  - isActive = true: 현재 선택 가능한 지역
     */
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
                        publicHasChildren()
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

    /*
     *  전달받은 부모 지역의 활성 직계 자식을 조회한다.
     *
     *  공개 선택 범위는 읍·면·동(level 3)까지이므로
     *  regionLevel <= 3 조건으로 RI(level 4)를 제외한다.
     *
     *  읍·면·동의 하위 RI는 DB에 존재하더라도 공개 목록에는 반환하지 않는다.
     */
    public List<RegionSummaryResponse> findChildRegions(
            Long parentRegionId
    ){
        return jpaQueryFactory
                .select(Projections.constructor(
                        RegionSummaryResponse.class,
                        region.regionId,
                        region.regionCode,
                        region.regionName,
                        region.regionLevel,
                        region.regionType,
                        publicHasChildren()
                ))
                .from(region)
                .where(
                        region.parentRegionId.eq(parentRegionId),
                        region.regionLevel.loe(3),
                        region.isActive.isTrue()
                )
                .orderBy(region.regionName.asc())
                .fetch();
    }

    /*
     *  읍면동에 연결된 매물을 검색할 때 사용할 범위:
     *  선택한 읍면동 자체 + 직계 활성 RI
     */
    public List<Long> findPropertySearchRegionIds(
        Long selectedRegionId
    ){
        return jpaQueryFactory
                .select(region.regionId)
                .from(region)
                .where(
                        region.regionId.eq(selectedRegionId)
                                .or(
                                        region.parentRegionId.eq(selectedRegionId)
                                ),
                        region.isActive.isTrue()
                )
                .fetch();
    }

    /*
     *   공개 지역 선택은 시·도(1) -> 시·군·구(2) -> 읍·면·동(3)까지만 허용한다.
     *
     *   DB의 hasChildren은 RI(level 4)까지 포함한 실제 자식 존재 여부이므로,
     *   읍·면·동도 RI 자식이 있으면 true일 수 있다.
     *
     *   API의 hasChildren은 "다음 공개 지역 목록으로 이동할 수 있는가"를 의미한다.
     *   따라서 level 1, 2 이면서 실제 자식이 있는 경우에만 true를 반환한다.
     */
    private BooleanExpression publicHasChildren(){
        return new CaseBuilder()
                .when(
                        region.regionLevel.lt(3)
                                .and(region.hasChildren.isTrue())
                )
                .then(true)
                .otherwise(false);
    }


}
