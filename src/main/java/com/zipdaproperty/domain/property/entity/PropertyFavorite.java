package com.zipdaproperty.domain.property.entity;

import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "property_favorite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyFavorite extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(
            name = "active_favorite_key",
            insertable = false,
            updatable = false
    )
    private String activeFavoriteKey;

}