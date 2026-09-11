package com.zipdaproperty.domain.image.entity;

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
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Getter
@Entity
@Filter(name = "softDelete")
@Table(name = "property_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyImage extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_image_id")
    private Long propertyImageId;

    @Column(
            name = "property_id",
            nullable = false,
            updatable = false
    )
    private Long propertyId;

    @Column(
            name = "property_file_id",
            nullable = false,
            updatable = false
    )
    private Long propertyFileId;

    @Column(
            name = "sort_order",
            nullable = false
    )
    private Integer sortOrder;

    @Column(
            name = "is_representative",
            nullable = false
    )
    private Boolean isRepresentative;

    @Column(
            name = "alt_text",
            length = 300
    )
    private String altText;

    @Column(
            name = "active_representative_key",
            insertable = false,
            updatable = false
    )
    private Long activeRepresentativeKey;

    @Column(
            name = "active_property_file_key",
            insertable = false,
            updatable = false
    )
    private Long activePropertyFileKey;

    private PropertyImage(
            Long propertyId,
            Long propertyFileId,
            Integer sortOrder,
            Boolean isRepresentative,
            String altText,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.propertyId = propertyId;
        this.propertyFileId = propertyFileId;
        this.sortOrder = sortOrder;
        this.isRepresentative = isRepresentative;
        this.altText = altText;
    }

    public static PropertyImage create(
            Long propertyId,
            Long propertyFileId,
            Integer sortOrder,
            Boolean isRepresentative,
            String altText,
            ActorContext actorContext
    ) {
        return new PropertyImage(
                propertyId,
                propertyFileId,
                sortOrder,
                isRepresentative,
                altText,
                actorContext
        );
    }

    public void changeSortOrder(
            Integer sortOrder,
            ActorContext actorContext
    ) {
        if (this.sortOrder.equals(sortOrder)) {
            return;
        }

        this.sortOrder = sortOrder;
        recordUpdate(actorContext);
    }

    public void changeRepresentative(
            boolean representative,
            ActorContext actorContext
    ) {
        if (this.isRepresentative == representative) {
            return;
        }

        this.isRepresentative = representative;
        recordUpdate(actorContext);
    }

    public void softDelete(
            ActorContext actorContext,
            Instant deletedAt,
            String deleteReason
    ) {
        recordDeletion(
                actorContext,
                deletedAt,
                deleteReason
        );
    }
}
