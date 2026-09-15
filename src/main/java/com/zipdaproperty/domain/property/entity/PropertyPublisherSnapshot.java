package com.zipdaproperty.domain.property.entity;

import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Entity
@Table(name = "property_publisher_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyPublisherSnapshot extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "publisher_snapshot_id",
            nullable = false,
            updatable = false
    )
    private Long publisherSnapshotId;

    @Column(
            name = "property_id",
            nullable = false,
            updatable = false
    )
    private Long propertyId;

    @Column(
            name = "property_revision_id",
            nullable = false,
            updatable = false
    )
    private Long propertyRevisionId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "publisher_type",
            nullable = false,
            updatable = false,
            length = 30
    )
    private PublisherType publisherType;

    @Column(
            name = "publisher_member_id",
            nullable = false,
            updatable = false
    )
    private Long publisherMemberId;

    @Column(
            name = "agent_id",
            updatable = false
    )
    private Long agentId;

    @Column(
            name = "agency_id",
            updatable = false
    )
    private Long agencyId;

    @Column(
            name = "office_name",
            updatable = false,
            length = 200
    )
    private String officeName;

    @Column(
            name = "office_address",
            updatable = false,
            length = 300
    )
    private String officeAddress;

    @Column(
            name = "office_contact",
            updatable = false,
            length = 50
    )
    private String officeContact;

    @Column(
            name = "registration_number",
            updatable = false,
            length = 100
    )
    private String registrationNumber;

    @Column(
            name = "broker_name",
            updatable = false,
            length = 100
    )
    private String brokerName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "snapshot_json",
            updatable = false,
            columnDefinition = "JSON"
    )
    private String snapshotJson;

    @Column(
            name = "captured_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant capturedAt; // 등록 주체 정보를 확인한 시각

    public PropertyPublisherSnapshot(
            Long propertyId,
            Long propertyRevisionId,
            PublisherType publisherType,
            Long publisherMemberId,
            Long agentId,
            Long agencyId,
            String officeName,
            String officeAddress,
            String officeContact,
            String registrationNumber,
            String brokerName,
            String snapshotJson,
            Instant capturedAt,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.propertyId = propertyId;
        this.propertyRevisionId = propertyRevisionId;
        this.publisherType = publisherType;
        this.publisherMemberId = publisherMemberId;
        this.agentId = agentId;
        this.agencyId = agencyId;
        this.officeName = officeName;
        this.officeAddress = officeAddress;
        this.officeContact = officeContact;
        this.registrationNumber = registrationNumber;
        this.brokerName = brokerName;
        this.snapshotJson = snapshotJson;
        this.capturedAt = capturedAt;
    }
}