package com.zipdaproperty.domain.favorite.entity;

import com.zipdaproperty.global.context.ActorContext;
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

import java.time.Instant;

@Getter
@Entity
@Table(name = "property_favorite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyFavorite extends BaseAuditEntity {

    private static final String FAVORITE_REMOVED_REASON =
            "FAVORITE_REMOVED";

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
            length = 150,
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

    public void remove(
            ActorContext actorContext,
            Instant deletedAt
    ) {
        if (isDeleted()) {
            return;
        }

        recordDeletion(
                actorContext,
                deletedAt,
                FAVORITE_REMOVED_REASON
        );
    }
}
