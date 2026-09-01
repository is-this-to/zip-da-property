package com.zipdaproperty.domain.property.entity;

import com.zipdaproperty.domain.property.constant.RevisionChangeScope;
import com.zipdaproperty.domain.property.constant.RevisionChangeType;
import com.zipdaproperty.global.context.constant.ActorRole;
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
@Table(name = "property_revision")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyRevision extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "property_revision_id",
            nullable = false,
            updatable = false
    )
    private Long propertyRevisionId;

    @Column(
            name = "property_id",
            nullable = false,
            updatable = false
    )
    private Long propertyId;

    @Column(
            name = "property_version",
            nullable = false,
            updatable = false
    )
    private Long propertyVersion;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "change_type",
            nullable = false,
            updatable = false,
            length = 30
    )
    private RevisionChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "change_scope",
            nullable = false,
            updatable = false,
            length = 30
    )
    private RevisionChangeScope changeScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "changed_fields_json",
            nullable = false,
            updatable = false,
            columnDefinition = "JSON"
    )
    private String changedFieldsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "before_snapshot_json",
            updatable = false,
            columnDefinition = "JSON"
    )
    private String beforeSnapshotJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "after_snapshot_json",
            nullable = false,
            updatable = false,
            columnDefinition = "JSON"
    )
    private String afterSnapshotJson;

    @Column(
            name = "snapshot_schema_version",
            nullable = false,
            updatable = false
    )
    private Integer snapshotSchemaVersion;

    @Column(
            name = "actor_member_id",
            updatable = false
    )
    private Long actorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "actor_role",
            updatable = false,
            length = 30
    )
    private ActorRole actorRole;

    @Column(
            name = "change_reason",
            updatable = false,
            length = 1000
    )
    private String changeReason;

    @Column(
            name = "trace_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String traceId;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant occurredAt;
}