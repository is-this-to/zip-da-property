package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.domain.option.type.OptionCategory;
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

@Getter
@Entity
@Table(name = "property_option_code")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyOptionCode extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_code_id")
    private Long optionCodeId;

    @Column(name = "option_code", nullable = false, length = 50)
    private String optionCode;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_category", nullable = false, length = 30)
    private OptionCategory optionCategory;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_filterable", nullable = false)
    private boolean filterable;

    @Column(name = "is_detail_visible", nullable = false)
    private boolean detailVisible;

    @Column(name = "is_registration_enabled", nullable = false)
    private boolean registrationEnabled;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
