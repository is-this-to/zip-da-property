package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.domain.option.type.OptionChangeType;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "changed_fields_json",
            nullable = false,
            updatable = false,
            columnDefinition = "JSON"
    )
    private String changedFieldsJson;

    @Column(name = "before_boolean_value", updatable = false)
    private Boolean beforeBooleanValue;

    @Column(name = "after_boolean_value", updatable = false)
    private Boolean afterBooleanValue;

    @Column(
            name = "before_number_value",
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal beforeNumberValue;

    @Column(
            name = "after_number_value",
            precision = 12,
            scale = 2,
            updatable = false
    )
    private BigDecimal afterNumberValue;

    @Column(
            name = "before_text_value",
            length = 300,
            updatable = false
    )
    private String beforeTextValue;

    @Column(
            name = "after_text_value",
            length = 300,
            updatable = false
    )
    private String afterTextValue;

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