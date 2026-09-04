package com.zipdaproperty.domain.option.entity;

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


@Getter
@Entity
@Table(name = "property_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyOption extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_option_id")
    private Long propertyOptionId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "option_code_id", nullable = false)
    private Long optionCodeId;

    @Column(name = "option_value", nullable = false, length = 300)
    private String optionValue;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "verified", nullable = false)
    private boolean verified;
}
