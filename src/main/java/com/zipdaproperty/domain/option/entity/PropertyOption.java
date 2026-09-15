package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.global.entity.BaseAuditEntity;
import com.zipdaproperty.global.context.ActorContext;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "property_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyOption extends BaseAuditEntity {

  private static final String TRUE_VALUE = "true";
  private static final String FALSE_VALUE = "false";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "property_option_id")
  private Long propertyOptionId;

  @Column(name = "property_id", nullable = false)
  private Long propertyId;

  @Column(name = "option_code_id", nullable = false)
  private Long optionCodeId;

  @Column(name = "option_value", nullable = false, length = 5)
  private String optionValue;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "verified", nullable = false)
  private boolean verified;

  public PropertyOption(
          Long propertyId,
          Long optionCodeId,
          String optionValue,
          int displayOrder,
          ActorContext actorContext
  ) {
    super(actorContext);
    this.propertyId = propertyId;
    this.optionCodeId = optionCodeId;
    this.optionValue = requireBooleanValue(optionValue);
    this.displayOrder = displayOrder;
    this.verified = false;
  }

  public void changeValue(
          String optionValue,
          ActorContext actorContext
  ) {
    ensureActive();
    this.optionValue = requireBooleanValue(optionValue);
    recordUpdate(actorContext);
  }

  public void changeValueAndDisplayOrder(
          String optionValue,
          int displayOrder,
          ActorContext actorContext
  ) {
    ensureActive();
    this.optionValue = requireBooleanValue(optionValue);
    this.displayOrder = displayOrder;
    recordUpdate(actorContext);
  }

  public void softDelete(
          ActorContext actorContext,
          Instant deletedAt,
          String deleteReason
  ) {
    ensureActive();
    recordDeletion(actorContext, deletedAt, deleteReason);
  }

  private void ensureActive() {
    if (isDeleted()) {
      throw new IllegalStateException("삭제된 옵션은 변경할 수 없습니다.");
    }
  }

  private static String requireBooleanValue(String optionValue) {
    String requiredValue = Objects.requireNonNull(
            optionValue,
            "옵션 값은 필수입니다."
    );

    if (!TRUE_VALUE.equals(requiredValue) && !FALSE_VALUE.equals(requiredValue)) {
      throw new IllegalArgumentException(
              "옵션 값은 true 또는 false만 사용할 수 있습니다."
      );
    }

    return requiredValue;
  }
}
