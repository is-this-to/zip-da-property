package com.zipdaproperty.domain.region.service;

import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.entity.RegionBoundary;
import com.zipdaproperty.domain.region.mapper.RegionGeometryMapper;
import com.zipdaproperty.domain.region.policy.RegionBoundarySimplificationPolicy;
import com.zipdaproperty.domain.region.repository.RegionBoundaryRepository;
import com.zipdaproperty.domain.region.repository.RegionQueryDSLRepository;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.domain.region.response.RegionDetailResponse;
import com.zipdaproperty.domain.region.response.RegionSummaryResponse;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionQueryDSLRepository regionQueryDSLRepository;
    private final RegionRepository regionRepository;
    private final RegionBoundarySimplificationPolicy simplificationPolicy;
    private final RegionBoundaryRepository regionBoundaryRepository;
    private final RegionGeometryMapper regionGeometryMapper;

    public List<RegionSummaryResponse> getRootRegions(){
        return regionQueryDSLRepository.findRootRegions();
    }

    public List<RegionSummaryResponse> getChildRegions(
            Long parentRegionId
    ){
        Region parentRegion = regionRepository
                .findByRegionIdAndIsActiveTrue(parentRegionId) // 활성화 된 지역만 가져오는 코드
                .orElseThrow(() -> new NotFoundResourceException(
                        "활성 상태의 부모 지역을 찾을 수 없습니다. parentRegionId = " + parentRegionId
                ));

        /*
         * RI는 공개 선택 대상이 아님.
         */
        if(parentRegion.getRegionLevel() > 3){
            throw new NotFoundResourceException(
                    "선택 가능한 지역을 찾을 수 없습니다."
            );
        }

        /*
         *  읍면동은 사용자 선택의 마지막 단계다.
         *  실제 DB의 RI 자식은 공개하지 않는다.
         */
        if(parentRegion.getRegionLevel() == 3){
            return List.of();
        }
        return regionQueryDSLRepository.findChildRegions(parentRegionId);
    }

    public RegionDetailResponse getRegionDetail(
            Long regionId,
            Integer kakaoMapLevel
    ){
        Region region = regionRepository
                .findByRegionIdAndIsActiveTrue(regionId)
                .orElseThrow(()-> new NotFoundResourceException(
                        "활성 상태의 지역을 찾을 수 없습니다. " + "regionId = " +regionId
                ));
        /*
         *  공개 지도 선택은 level 1~3만 허용
         */
        if(region.getRegionLevel() > 3){
            throw new NotFoundResourceException(
                    "선택 가능한 지역을 찾을 수 없습니다."
            );
        }

        int requestedSimplificationLevel = simplificationPolicy.resolve(kakaoMapLevel);

        RegionBoundary boundary =
                findBoundaryWithFallback(
                        regionId,
                        requestedSimplificationLevel
                ).orElse(null);

        if(boundary == null){
            log.warn(
                    "REGION_GEOMETRY_UNAVAILABLE: "
                        +  "regionId={}, simplificationLevel={}",
                    regionId,
                    requestedSimplificationLevel
            );
        }

        return createDetailResponse(
                region,
                boundary
        );
    }

    private Optional<RegionBoundary> findBoundaryWithFallback(
            Long regionId,
            int requestedLevel
    ){
        Optional<RegionBoundary> requested =
                regionBoundaryRepository
                        .findByRegionIdAndSimplificationLevelAndDeletedAtIsNull(
                                regionId,
                                requestedLevel
                        );
        if(requested.isPresent()){
            return requested;
        }

        /*
         *  이미 원본을 요청했다면 반복 조회하지 않는다.
         */
        if (requestedLevel == 0){
            return Optional.empty();
        }

        /*
         *  요청 단계가 없으면 원본 level 0으로 fallback
         */
        return regionBoundaryRepository
                .findByRegionIdAndSimplificationLevelAndDeletedAtIsNull(
                        regionId,
                        0
                );
    }

    private RegionDetailResponse createDetailResponse(
            Region region,
            RegionBoundary boundary
    ) {
        RegionDetailResponse.CenterResponse center =
            createCenter(region);

        if(boundary == null){
            return new RegionDetailResponse(
                    region.getRegionId(),
                    region.getRegionCode(),
                    region.getRegionName(),
                    region.getRegionLevel(),
                    region.getRegionType(),
                    center,
                    null,
                    null,
                    null
            );
        }

        return new RegionDetailResponse(
                region.getRegionId(),
                region.getRegionCode(),
                region.getRegionName(),
                region.getRegionLevel(),
                region.getRegionType(),
                center,
                boundary.getSimplificationLevel(),
                regionGeometryMapper.toGeoJson(boundary),
                createBounds(boundary)
        );
    }

    private RegionDetailResponse.CenterResponse createCenter(
            Region region
    ){
        if (region.getCenterLocation() == null){
            return null;
        }

        return new RegionDetailResponse.CenterResponse(
                region.getCenterLocation().getY(),
                region.getCenterLocation().getX()
        );
    }

    private RegionDetailResponse.BoundsResponse createBounds(
            RegionBoundary boundary
    ){
        var southWest =
                new RegionDetailResponse.CoordinateResponse(
                        boundary.getSouthWestLatitude(),
                        boundary.getSouthWestLongitude()
                );
        var northEast =
                new RegionDetailResponse.CoordinateResponse(
                        boundary.getNorthEastLatitude(),
                        boundary.getNorthEastLongitude()
                );
        return new RegionDetailResponse.BoundsResponse(
                southWest,
                northEast
        );

    }
}
