package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "property_type_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyTypeOption extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_type_option_id")
    private Long propertyTypeOptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_code_id", nullable = false)
    private PropertyOptionCode optionCode;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "default_boolean_value")
    private Boolean defaultBooleanValue;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}