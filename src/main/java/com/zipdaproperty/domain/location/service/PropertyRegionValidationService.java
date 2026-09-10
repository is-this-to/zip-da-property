package com.zipdaproperty.domain.location.service;

import com.zipdaproperty.domain.location.command.PropertyLocationValidationCommand;
import com.zipdaproperty.domain.location.model.VerifiedPropertyLocation;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.entity.RegionBoundary;
import com.zipdaproperty.domain.region.repository.RegionBoundaryRepository;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.config.location.PropertyLocationValidationProperties;
import com.zipdaproperty.global.error.custom.business.PropertyLocationRegionMismatchException;
import com.zipdaproperty.global.error.custom.business.PropertyRegionBoundaryNotFoundException;
import com.zipdaproperty.global.error.custom.business.PropertyRegionNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Service
@Validated
@RequiredArgsConstructor
public class PropertyRegionValidationService {

    private static final int WGS84_SRID = 4326;
    private static final int ORIGINAL_SIMPLIFICATION_LEVEL = 0;
    private static final int MINIMUM_INTERNAL_REGION_LEVEL = 1;
    private static final int MAXIMUM_INTERNAL_REGION_LEVEL = 4;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(
                    new PrecisionModel(),
                    WGS84_SRID
            );

    private final RegionRepository regionRepository;
    private final RegionBoundaryRepository regionBoundaryRepository;
    private final PropertyLocationValidationProperties validationProperties;

    @Transactional(readOnly = true)
    public VerifiedPropertyLocation validate(
            @Valid
            @NotNull
            PropertyLocationValidationCommand command
    ) {
        validateAllowedCoordinateRange(command);

        Region region = findValidationTargetRegion(
                command.legalDongCode()
        );

        RegionBoundary regionBoundary =
                findOriginalRegionBoundary(region.getRegionId());

        Point exactLocation = createExactLocation(
                command.longitude(),
                command.latitude()
        );

        validateSpatialReference(regionBoundary, exactLocation);
        validateLocationInsideRegion(regionBoundary, exactLocation);

        return new VerifiedPropertyLocation(
                region.getRegionId(),
                region.getRegionCode(),
                exactLocation
        );
    }

    private void validateAllowedCoordinateRange(
            PropertyLocationValidationCommand command
    ) {
        boolean allowed = validationProperties.contains(
                command.longitude(),
                command.latitude()
        );

        if (!allowed) {
            throw new PropertyLocationRegionMismatchException(
                    "입력된 좌표가 서비스에서 허용하는 대한민국 좌표 범위를 벗어났습니다."
            );
        }
    }

    private Region findValidationTargetRegion(
            String legalDongCode
    ) {
        return regionRepository
                .findByRegionCodeAndRegionLevelBetweenAndIsActiveTrueAndDeletedAtIsNull(
                        legalDongCode,
                        MINIMUM_INTERNAL_REGION_LEVEL,
                        MAXIMUM_INTERNAL_REGION_LEVEL
                )
                .orElseThrow(
                        () -> new PropertyRegionNotFoundException(
                                "법정동 코드와 일치하는 활성 Region이 존재하지 않습니다."
                        )
                );
    }

    private RegionBoundary findOriginalRegionBoundary(
            Long regionId
    ) {
        RegionBoundary regionBoundary = regionBoundaryRepository
                .findByRegionIdAndSimplificationLevelAndDeletedAtIsNull(
                        regionId,
                        ORIGINAL_SIMPLIFICATION_LEVEL
                )
                .orElseThrow(
                        () -> new PropertyRegionBoundaryNotFoundException(
                                "해당 Region의 원본 경계 정보가 존재하지 않습니다."
                        )
                );

        MultiPolygon boundaryGeometry =
                regionBoundary.getBoundaryGeometry();

        if (boundaryGeometry == null || boundaryGeometry.isEmpty()) {
            throw new PropertyRegionBoundaryNotFoundException(
                    "해당 Region의 원본 경계 Geometry가 비어 있습니다."
            );
        }

        return regionBoundary;
    }

    private Point createExactLocation(
            BigDecimal longitude,
            BigDecimal latitude
    ) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(
                        longitude.doubleValue(),
                        latitude.doubleValue()
                )
        );

        point.setSRID(WGS84_SRID);

        return point;
    }

    private void validateSpatialReference(
            RegionBoundary regionBoundary,
            Point exactLocation
    ) {
        MultiPolygon boundaryGeometry =
                regionBoundary.getBoundaryGeometry();

        if (boundaryGeometry.getSRID() != WGS84_SRID) {
            throw new PropertyRegionBoundaryNotFoundException(
                    "Region 경계의 SRID가 4326이 아닙니다."
            );
        }

        if (exactLocation.getSRID() != WGS84_SRID) {
            throw new PropertyLocationRegionMismatchException(
                    "입력 좌표의 SRID가 4326이 아닙니다."
            );
        }
    }

    private void validateLocationInsideRegion(
            RegionBoundary regionBoundary,
            Point exactLocation
    ) {
        MultiPolygon boundaryGeometry =
                regionBoundary.getBoundaryGeometry();

        if (!boundaryGeometry.covers(exactLocation)) {
            throw new PropertyLocationRegionMismatchException(
                    "입력된 좌표가 법정동 코드에 해당하는 Region 경계 내부에 존재하지 않습니다."
            );
        }
    }
}
