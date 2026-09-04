package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.domain.option.type.OptionChangeType;
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

import java.time.Instant;

@Getter
@Entity
@Table(name = "property_option_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyOptionHistory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "property_option_history_id",
            nullable = false,
            updatable = false
    )
    private Long propertyOptionHistoryId;

    @Column(
            name = "property_revision_id",
            nullable = false,
            updatable = false
    )
    private Long propertyRevisionId;

    @Column(
            name = "property_option_id",
            nullable = false,
            updatable = false
    )
    private Long propertyOptionId;

    @Column(
            name = "option_code_id",
            nullable = false,
            updatable = false
    )
    private Long optionCodeId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "change_type",
            nullable = false,
            updatable = false,
            length = 30
    )
    private OptionChangeType changeType;

    @Column(name = "changed_fields", nullable = false, updatable = false, length = 500)
    private String changedFields;

    @Column(name = "before_value", updatable = false, length = 300)
    private String beforeValue;

    @Column(name = "after_value", updatable = false, length = 300)
    private String afterValue;

    @Column(name = "before_display_order", updatable = false)
    private Integer beforeDisplayOrder;

    @Column(name = "after_display_order", updatable = false)
    private Integer afterDisplayOrder;

    @Column(name = "before_verified", updatable = false)
    private Boolean beforeVerified;

    @Column(name = "after_verified", updatable = false)
    private Boolean afterVerified;

    @Column(
            name = "before_deleted_at",
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant beforeDeletedAt;

    @Column(
            name = "after_deleted_at",
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant afterDeletedAt;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant occurredAt;
}
