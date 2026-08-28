package com.zipdaproperty.domain.property.entity;

import com.zipdaproperty.global.context.ActorContext;
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

    public PropertyFavorite(
            Long memberId,
            Long propertyId,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.memberId = memberId;
        this.propertyId = propertyId;
    }
}