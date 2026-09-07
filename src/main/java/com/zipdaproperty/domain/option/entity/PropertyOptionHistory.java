package com.zipdaproperty.domain.option.entity;

import com.zipdaproperty.domain.option.type.OptionChangeType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

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

  @Column(
      name = "changed_fields",
      nullable = false,
      updatable = false,
      length = 500
  )
  private String changedFields;

  @Column(
      name = "before_value",
      updatable = false,
      length = 5
  )
  private String beforeValue;

  @Column(
      name = "after_value",
      updatable = false,
      length = 5
  )
  private String afterValue;

  @Column(
      name = "before_display_order",
      updatable = false
  )
  private Integer beforeDisplayOrder;

  @Column(
      name = "after_display_order",
      updatable = false
  )
  private Integer afterDisplayOrder;

  @Column(
      name = "before_verified",
      updatable = false
  )
  private Boolean beforeVerified;

  @Column(
      name = "after_verified",
      updatable = false
  )
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

  private PropertyOptionHistory(
      Long propertyRevisionId,
      Long propertyOptionId,
      Long optionCodeId,
      OptionChangeType changeType,
      String changedFields,
      Snapshot before,
      Snapshot after,
      Instant occurredAt,
      ActorContext actorContext
  ) {
    super(actorContext);
    this.propertyRevisionId = Objects.requireNonNull(
        propertyRevisionId,
        "매물 리비전 ID는 필수입니다."
    );
    this.propertyOptionId = Objects.requireNonNull(
        propertyOptionId,
        "매물 옵션 ID는 필수입니다."
    );
    this.optionCodeId = Objects.requireNonNull(
        optionCodeId,
        "옵션 코드 ID는 필수입니다."
    );
    this.changeType = Objects.requireNonNull(changeType, "변경 유형은 필수입니다.");
    this.changedFields = requireChangedFields(changedFields);
    this.beforeValue = before == null ? null : before.optionValue();
    this.afterValue = after == null ? null : after.optionValue();
    this.beforeDisplayOrder = before == null ? null : before.displayOrder();
    this.afterDisplayOrder = after == null ? null : after.displayOrder();
    this.beforeVerified = before == null ? null : before.verified();
    this.afterVerified = after == null ? null : after.verified();
    this.beforeDeletedAt = before == null ? null : before.deletedAt();
    this.afterDeletedAt = after == null ? null : after.deletedAt();
    this.occurredAt = Objects.requireNonNull(occurredAt, "변경 발생 시각은 필수입니다.");
  }

  public static PropertyOptionHistory create(
      Long propertyRevisionId,
      PropertyOption propertyOption,
      String changedFields,
      Instant occurredAt,
      ActorContext actorContext
  ) {
    Objects.requireNonNull(propertyOption, "매물 옵션은 필수입니다.");
    return new PropertyOptionHistory(
        propertyRevisionId,
        propertyOption.getPropertyOptionId(),
        propertyOption.getOptionCodeId(),
        OptionChangeType.CREATE,
        changedFields,
        null,
        Snapshot.from(propertyOption),
        occurredAt,
        actorContext
    );
  }

  public static PropertyOptionHistory update(
      Long propertyRevisionId,
      PropertyOption propertyOption,
      String changedFields,
      Snapshot before,
      Instant occurredAt,
      ActorContext actorContext
  ) {
    return changed(
        propertyRevisionId,
        propertyOption,
        OptionChangeType.UPDATE,
        changedFields,
        before,
        occurredAt,
        actorContext
    );
  }

  public static PropertyOptionHistory softDelete(
      Long propertyRevisionId,
      PropertyOption propertyOption,
      String changedFields,
      Snapshot before,
      Instant occurredAt,
      ActorContext actorContext
  ) {
    return changed(
        propertyRevisionId,
        propertyOption,
        OptionChangeType.SOFT_DELETE,
        changedFields,
        before,
        occurredAt,
        actorContext
    );
  }

  private static PropertyOptionHistory changed(
      Long propertyRevisionId,
      PropertyOption propertyOption,
      OptionChangeType changeType,
      String changedFields,
      Snapshot before,
      Instant occurredAt,
      ActorContext actorContext
  ) {
    Objects.requireNonNull(propertyOption, "매물 옵션은 필수입니다.");
    Objects.requireNonNull(before, "변경 전 상태는 필수입니다.");
    return new PropertyOptionHistory(
        propertyRevisionId,
        propertyOption.getPropertyOptionId(),
        propertyOption.getOptionCodeId(),
        changeType,
        changedFields,
        before,
        Snapshot.from(propertyOption),
        occurredAt,
        actorContext
    );
  }

  private static String requireChangedFields(String changedFields) {
    if (changedFields == null || changedFields.isBlank()) {
      throw new IllegalArgumentException("변경 필드는 필수입니다.");
    }
    if (changedFields.length() > 500) {
      throw new IllegalArgumentException("변경 필드는 500자를 초과할 수 없습니다.");
    }
    return changedFields;
  }

  public record Snapshot(
      String optionValue,
      Integer displayOrder,
      Boolean verified,
      Instant deletedAt
  ) {

    public static Snapshot from(PropertyOption propertyOption) {
      Objects.requireNonNull(propertyOption, "매물 옵션은 필수입니다.");
      return new Snapshot(
          propertyOption.getOptionValue(),
          propertyOption.getDisplayOrder(),
          propertyOption.isVerified(),
          propertyOption.getDeletedAt()
      );
    }
  }
}
