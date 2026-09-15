package com.zipdaproperty.domain.location.service;

import com.zipdaproperty.domain.location.command.PublicLocationGenerationCommand;
import com.zipdaproperty.domain.location.model.GeneratedPublicLocation;
import com.zipdaproperty.domain.location.model.VerifiedPropertyLocation;
import com.zipdaproperty.domain.region.entity.RegionBoundary;
import com.zipdaproperty.domain.region.repository.RegionBoundaryRepository;
import com.zipdaproperty.global.config.location.PropertyPublicLocationProperties;
import com.zipdaproperty.global.error.custom.business.PropertyPublicLocationGenerationException;
import com.zipdaproperty.global.error.custom.business.PropertyRegionBoundaryNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@Service
@Validated
@RequiredArgsConstructor
public class PropertyPublicLocationGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int WGS84_SRID = 4326;
    private static final int ORIGINAL_SIMPLIFICATION_LEVEL = 0;
    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private static final double[] SAFE_INWARD_RATIOS = {
            0.001,
            0.005,
            0.01,
            0.02,
            0.05,
            0.10
    };

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(
                    new PrecisionModel(),
                    WGS84_SRID
            );

    private final RegionBoundaryRepository regionBoundaryRepository;
    private final PropertyPublicLocationProperties properties;

    @Transactional(readOnly = true)
    public GeneratedPublicLocation generate(
            @Valid
            @NotNull
            PublicLocationGenerationCommand command
    ) {
        VerifiedPropertyLocation verifiedLocation =
                command.verifiedLocation();

        Point exactLocation =
                verifiedLocation.exactLocation();

        validateExactLocation(exactLocation);

        MultiPolygon boundary =
                findOriginalBoundary(
                        verifiedLocation.regionId()
                );

        byte[] digest = generateHmacDigest(command);

        double bearingRadians =
                determineBearingRadians(digest);

        int distanceMeters =
                determineDistanceMeters(digest);

        Point movedLocation = moveLocation(
                exactLocation,
                bearingRadians,
                distanceMeters
        );

        Point publicLocation;

        if (boundary.covers(movedLocation)) {
            publicLocation = movedLocation;
        } else {
            publicLocation = correctInsideBoundary(
                    boundary,
                    movedLocation,
                    exactLocation
            );
        }

        validateGeneratedLocation(
                boundary,
                publicLocation
        );

        return new GeneratedPublicLocation(
                publicLocation
        );
    }

    private MultiPolygon findOriginalBoundary(Long regionId) {
        RegionBoundary regionBoundary =
                regionBoundaryRepository
                        .findByRegionIdAndSimplificationLevelAndDeletedAtIsNull(
                                regionId,
                                ORIGINAL_SIMPLIFICATION_LEVEL
                        )
                        .orElseThrow(
                                () -> new PropertyRegionBoundaryNotFoundException(
                                        "공개 위치 생성에 필요한 원본 Region 경계가 존재하지 않습니다."
                                )
                        );

        MultiPolygon boundary =
                regionBoundary.getBoundaryGeometry();

        if (boundary == null || boundary.isEmpty()) {
            throw new PropertyRegionBoundaryNotFoundException(
                    "공개 위치 생성에 필요한 Region 경계 Geometry가 비어 있습니다."
            );
        }

        if (boundary.getSRID() != WGS84_SRID) {
            throw new PropertyRegionBoundaryNotFoundException(
                    "공개 위치 생성에 사용하는 Region 경계의 SRID가 4326이 아닙니다."
            );
        }

        return boundary;
    }

    private void validateExactLocation(Point exactLocation) {
        if (exactLocation == null || exactLocation.isEmpty()) {
            throw new PropertyPublicLocationGenerationException(
                    "공개 위치 생성에 필요한 정확 좌표가 존재하지 않습니다."
            );
        }

        if (exactLocation.getSRID() != WGS84_SRID) {
            throw new PropertyPublicLocationGenerationException(
                    "정확 좌표의 SRID가 4326이 아닙니다."
            );
        }

        if (!Double.isFinite(exactLocation.getX())
                || !Double.isFinite(exactLocation.getY())) {
            throw new PropertyPublicLocationGenerationException(
                    "공개 위치 생성에 사용할 수 없는 좌표입니다."
            );
        }
    }

    private byte[] generateHmacDigest(
            PublicLocationGenerationCommand command
    ) {
        String payload =
                properties.secretVersion()
                        + "|"
                        + command.propertyId()
                        + "|"
                        + command.normalizedAddressHash();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec key = new SecretKeySpec(
                    properties.secret()
                            .getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );

            mac.init(key);

            return mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "공개 위치 생성을 위한 HMAC 처리를 완료하지 못했습니다.",
                    exception
            );
        }
    }

    private double determineBearingRadians(byte[] digest) {
        byte[] bearingBytes =
                Arrays.copyOfRange(digest, 0, 8);

        BigInteger bearingValue =
                new BigInteger(1, bearingBytes);

        BigInteger maximumValue =
                BigInteger.ONE.shiftLeft(64);

        double ratio =
                bearingValue.doubleValue()
                        / maximumValue.doubleValue();

        return ratio * Math.PI * 2.0;
    }

    private int determineDistanceMeters(byte[] digest) {
        byte[] distanceBytes =
                Arrays.copyOfRange(digest, 8, 16);

        BigInteger distanceValue =
                new BigInteger(1, distanceBytes);

        int minimum = properties.minDistanceMeters();
        int maximum = properties.maxDistanceMeters();
        int distanceRange = maximum - minimum + 1;

        int offset = distanceValue
                .mod(BigInteger.valueOf(distanceRange))
                .intValue();

        return minimum + offset;
    }

    private Point moveLocation(
            Point exactLocation,
            double bearingRadians,
            int distanceMeters
    ) {
        double latitudeRadians =
                Math.toRadians(exactLocation.getY());

        double longitudeRadians =
                Math.toRadians(exactLocation.getX());

        double angularDistance =
                distanceMeters / EARTH_RADIUS_METERS;

        double movedLatitudeRadians = Math.asin(
                Math.sin(latitudeRadians)
                        * Math.cos(angularDistance)
                        + Math.cos(latitudeRadians)
                        * Math.sin(angularDistance)
                        * Math.cos(bearingRadians)
        );

        double movedLongitudeRadians =
                longitudeRadians
                        + Math.atan2(
                        Math.sin(bearingRadians)
                                * Math.sin(angularDistance)
                                * Math.cos(latitudeRadians),
                        Math.cos(angularDistance)
                                - Math.sin(latitudeRadians)
                                * Math.sin(movedLatitudeRadians)
                );

        double movedLongitude = normalizeLongitude(
                Math.toDegrees(movedLongitudeRadians)
        );

        double movedLatitude =
                Math.toDegrees(movedLatitudeRadians);

        return createPoint(
                movedLongitude,
                movedLatitude
        );
    }

    private double normalizeLongitude(double longitude) {
        return (longitude + 540.0) % 360.0 - 180.0;
    }

    private Point correctInsideBoundary(
            MultiPolygon boundary,
            Point movedLocation,
            Point exactLocation
    ) {
        Geometry boundaryLine = boundary.getBoundary();

        Coordinate[] nearestCoordinates =
                DistanceOp.nearestPoints(
                        boundaryLine,
                        movedLocation
                );

        if (nearestCoordinates == null
                || nearestCoordinates.length == 0) {
            throw new PropertyPublicLocationGenerationException(
                    "Region 경계 내부의 공개 좌표를 결정할 수 없습니다."
            );
        }

        Coordinate nearestBoundaryCoordinate =
                nearestCoordinates[0];

        for (double ratio : SAFE_INWARD_RATIOS) {
            double correctedLongitude =
                    nearestBoundaryCoordinate.getX()
                            + (
                            exactLocation.getX()
                                    - nearestBoundaryCoordinate.getX()
                    ) * ratio;

            double correctedLatitude =
                    nearestBoundaryCoordinate.getY()
                            + (
                            exactLocation.getY()
                                    - nearestBoundaryCoordinate.getY()
                    ) * ratio;

            Point correctedLocation = createPoint(
                    correctedLongitude,
                    correctedLatitude
            );

            if (boundary.covers(correctedLocation)) {
                return correctedLocation;
            }
        }

        Point nearestBoundaryPoint = createPoint(
                nearestBoundaryCoordinate.getX(),
                nearestBoundaryCoordinate.getY()
        );

        if (boundary.covers(nearestBoundaryPoint)) {
            return nearestBoundaryPoint;
        }

        throw new PropertyPublicLocationGenerationException(
                "생성한 공개 좌표를 Region 경계 내부로 보정할 수 없습니다."
        );
    }

    private Point createPoint(
            double longitude,
            double latitude
    ) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(
                        longitude,
                        latitude
                )
        );

        point.setSRID(WGS84_SRID);

        return point;
    }

    private void validateGeneratedLocation(
            MultiPolygon boundary,
            Point publicLocation
    ) {
        if (publicLocation == null
                || publicLocation.isEmpty()) {
            throw new PropertyPublicLocationGenerationException(
                    "생성된 공개 좌표가 비어 있습니다."
            );
        }

        if (publicLocation.getSRID() != WGS84_SRID) {
            throw new PropertyPublicLocationGenerationException(
                    "생성된 공개 좌표의 SRID가 4326이 아닙니다."
            );
        }

        if (!boundary.covers(publicLocation)) {
            throw new PropertyPublicLocationGenerationException(
                    "생성된 공개 좌표가 Region 경계 내부에 존재하지 않습니다."
            );
        }
    }
}
