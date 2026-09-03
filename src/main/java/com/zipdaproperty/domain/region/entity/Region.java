package com.zipdaproperty.domain.region.entity;

import com.zipdaproperty.domain.region.constant.RegionType;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;

@Getter
@Entity
@FilterDef(
        name = "softDelete",
        defaultCondition = "deleted_at IS NULL"
)
@Filter(name = "softDelete")
@Table(name = "region")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "region_id",
            nullable = false,
            updatable = false
    )
    private Long regionId;

    @Column(name = "parent_region_id")
    private Long parentRegionId;

    @Column(
            name = "region_code",
            nullable = false,
            length = 10
    )
    private String regionCode;

    @Column(
            name = "center_location",
            columnDefinition = "POINT SRID 4326"
    )
    private Point centerLocation;

    @Column(
            name = "region_name",
            nullable = false,
            length = 100
    )
    private String regionName;

    @Column(
            name = "full_region_name",
            nullable = false,
            length = 300
    )
    private String fullRegionName;

    @Column(
            name = "region_level",
            nullable = false
    )
    private Integer regionLevel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "region_type",
            nullable = false,
            length = 30
    )
    private RegionType regionType;

    @Column(
            name = "has_children",
            nullable = false
    )
    private Boolean hasChildren;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "replacement_region_id")
    private Long replacementRegionId;
}
