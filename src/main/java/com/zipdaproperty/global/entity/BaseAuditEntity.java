package com.zipdaproperty.global.entity;

import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActionSource;
import com.zipdaproperty.global.context.constant.ActorRole;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
@MappedSuperclass
public abstract class BaseAuditEntity extends BaseTimeEntity {

    @Column(name = "created_by_member_id")
    private Long createdByMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_role", length = 30)
    private ActorRole createdByRole;

    @Column(name = "updated_by_member_id")
    private Long updatedByMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "updated_by_role", length = 30)
    private ActorRole updatedByRole;

    @Column(
            name = "deleted_at",
            columnDefinition = "DATETIME(6)"
    )
    private Instant deletedAt;

    @Column(name = "deleted_by_member_id")
    private Long deletedByMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "deleted_by_role", length = 30)
    private ActorRole deletedByRole;

    @Column(name = "delete_reason", length = 500)
    private String deleteReason;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action_source",
            nullable = false,
            length = 20
    )
    private ActionSource actionSource;

    protected BaseAuditEntity() {
    }

    protected BaseAuditEntity(ActorContext actorContext) {
        recordCreation(actorContext);
    }

    private void recordCreation(ActorContext actorContext) {
        ActorContext requiredActorContext = Objects.requireNonNull(
                actorContext,
                "생성 작업에는 ActorContext가 필요합니다."
        );

        this.createdByMemberId = requiredActorContext.memberId();
        this.createdByRole = requiredActorContext.role();
        this.updatedByMemberId = requiredActorContext.memberId();
        this.updatedByRole = requiredActorContext.role();
        this.actionSource = requiredActorContext.actionSource();
    }

    protected final void recordUpdate(ActorContext actorContext) {
        ActorContext requiredActorContext = Objects.requireNonNull(
                actorContext,
                "수정 작업에는 ActorContext가 필요합니다."
        );

        this.updatedByMemberId = requiredActorContext.memberId();
        this.updatedByRole = requiredActorContext.role();
        this.actionSource = requiredActorContext.actionSource();
    }

    protected final void recordDeletion(
            ActorContext actorContext,
            Instant deletedAt,
            String deleteReason
    ) {
        ActorContext requiredActorContext = Objects.requireNonNull(
                actorContext,
                "삭제 작업에는 ActorContext가 필요합니다."
        );

        this.deletedAt = Objects.requireNonNull(
                deletedAt,
                "삭제 시간은 필수입니다."
        );
        this.deletedByMemberId = requiredActorContext.memberId();
        this.deletedByRole = requiredActorContext.role();
        this.deleteReason = deleteReason;

        recordUpdate(requiredActorContext);
    }

    protected final void recordRestoration(ActorContext actorContext) {
        ActorContext requiredActorContext = Objects.requireNonNull(
                actorContext,
                "복구 작업에는 ActorContext가 필요합니다."
        );

        this.deletedAt = null;
        this.deletedByMemberId = null;
        this.deletedByRole = null;
        this.deleteReason = null;

        recordUpdate(requiredActorContext);
    }

    public final boolean isDeleted() {
        return deletedAt != null;
    }
}