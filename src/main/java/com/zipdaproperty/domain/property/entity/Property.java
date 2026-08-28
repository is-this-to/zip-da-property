package com.zipdaproperty.domain.property.entity;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "property")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Property extends BaseAuditEntity {

    @Id
    @Column(
            name = "property_id",
            nullable = false,
            updatable = false
    )
    private Long propertyId;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @Column(
            name = "region_id",
            nullable = false
    )
    private Long regionId;

    @Column(name = "apartment_complex_id")
    private Long apartmentComplexId;

    @Column(
            name = "author_member_id",
            nullable = false,
            updatable = false
    )
    private Long authorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "publisher_type",
            nullable = false,
            length = 30
    )
    private PublisherType publisherType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "property_type",
            nullable = false,
            length = 30
    )
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_type",
            nullable = false,
            length = 30
    )
    private TransactionType transactionType;

    @Column(name = "sale_price")
    private Long salePrice;

    @Column(name = "deposit")
    private Long deposit;

    @Column(name = "monthly_rent")
    private Long monthlyRent;

    @Column(name = "maintenance_fee")
    private Long maintenanceFee;

    @Column(
            name = "supply_area",
            precision = 10,
            scale = 2
    )
    private BigDecimal supplyArea;

    @Column(
            name = "exclusive_area",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal exclusiveArea;

    @Column(name = "room_count")
    private Integer roomCount;

    @Column(name = "bathroom_count")
    private Integer bathroomCount;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "total_floor")
    private Integer totalFloor;

    @Column(
            name = "floor_condition",
            length = 30
    )
    private String floorCondition;

    @Column(
            name = "direction",
            length = 20
    )
    private String direction;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(
            name = "building_use",
            length = 100
    )
    private String buildingUse;

    @Column(name = "is_parking_available")
    private Boolean isParkingAvailable;

    @Column(name = "has_elevator")
    private Boolean hasElevator;

    @Column(name = "is_pet_allowed")
    private Boolean isPetAllowed;

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "publication_status",
            nullable = false,
            length = 30
    )
    private PublicationStatus publicationStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "transaction_status",
            nullable = false,
            length = 30
    )
    private TransactionStatus transactionStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "verification_status",
            nullable = false,
            length = 30
    )
    private VerificationStatus verificationStatus;

    @Column(
            name = "risk_score",
            precision = 5,
            scale = 2
    )
    private BigDecimal riskScore;

    @Column(
            name = "published_at",
            columnDefinition = "DATETIME(6)"
    )
    private Instant publishedAt;
}