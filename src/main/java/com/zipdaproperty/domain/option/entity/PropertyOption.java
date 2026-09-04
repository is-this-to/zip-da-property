package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "property_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyOption extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_option_id")
    private Long propertyOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_code_id", nullable = false)
    private PropertyOptionCode optionCode;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "number_value", precision = 12, scale = 2)
    private BigDecimal numberValue;

    @Column(name = "text_value", length = 300)
    private String textValue;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "verified", nullable = false)
    private boolean verified;
}