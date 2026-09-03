package com.zipdaproperty.domain.region.entity;

import com.zipdaproperty.domain.region.constant.RegionGeometryType;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.locationtech.jts.geom.MultiPolygon;

import java.math.BigDecimal;


@Getter
@Entity
@Table(name = "region_boundary")
@Filter(name  = "softDelete")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionBoundary extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "region_boundary_id",
            nullable = false,
            updatable = false
    )
    private Long regionBoundaryId;

    /*
     *  실제 DDL에는 물리 FK가 없다.
     *  Region Entity와 @ManyToOne으로 묶지 않고 ID로 논리 참조함.
     */
    @Column(
            name = "region_id",
            nullable = false
    )
    private Long regionId;

    /*
     *  원본 도형 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "geometry_type",
            nullable = false,
            length = 20
    )
    private RegionGeometryType geometryType;

    /*
     *  DB에는 항상 MultiPolygon으로 정규화 저장
     */
    @Column(
            name = "boundary_geometry",
            nullable = false,
            columnDefinition = "MULTIPOLYGON SRID 4326"
    )
    private MultiPolygon boundaryGeometry;

    @Column(
            name = "south_west_latitude",
            nullable = false,
            precision = 10, // 전체 숫자 자릿수
            scale = 7 // 소수점 아래 자릿수
    )
    private BigDecimal southWestLatitude;

    @Column(
            name = "south_west_longitude",
            nullable = false,
            precision = 10,
            scale = 7
    )
    private BigDecimal southWestLongitude;

    @Column(
            name = "north_east_latitude",
            nullable = false,
            precision = 10,
            scale = 7
    )
    private BigDecimal northEastLatitude;

    @Column(
            name = "north_east_longitude",
            nullable = false,
            precision = 10,
            scale = 7
    )
    private BigDecimal northEastLongitude;

    @Column(
            name = "simplification_level",
            nullable = false
    )
    private Integer simplificationLevel;

    /*
     *  MySQL generated column
     */
    @Column(
            name = "active_region_boundary_key",
            insertable = false, // JPA INSERT SQL에 이 컬럼을 포함하지 않음
            updatable = false // JPA UPDATE SQL에 이 컬럼을 포함하지 않음
    )
    private Long activeRegionBoundaryKey;
}
