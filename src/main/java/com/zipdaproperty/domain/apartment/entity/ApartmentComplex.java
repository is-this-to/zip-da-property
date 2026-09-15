package com.zipdaproperty.domain.apartment.entity;

import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Entity
@Filter(name = "softDelete")
@Table(name = "apartment_complex")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApartmentComplex extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apartment_complex_id", nullable = false, updatable = false)
    private Long apartmentComplexId;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "complex_name", nullable = false, length = 200)
    private String complexName;

    @Column(name = "road_address", length = 300)
    private String roadAddress;

    @Column(name = "jibun_address", length = 300)
    private String jibunAddress;

    @Column(name = "center_location", nullable = false, columnDefinition = "POINT SRID 4326")
    private Point centerLocation;

    @Column(name = "total_buildings")
    private Integer totalBuildings;

    @Column(name = "total_households")
    private Integer totalHouseholds;

    @Column(name = "total_parking_spaces")
    private Integer totalParkingSpaces;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "source_updated_at", columnDefinition = "DATETIME(6)")
    private Instant sourceUpdatedAt;
}
