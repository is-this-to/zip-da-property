package com.zipdaproperty.domain.property.idempotency.entity;

import com.zipdaproperty.domain.property.idempotency.constant.IdempotencyProcessingStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "property_idempotency",
        indexes = {
                @Index(
                        name = "idx_idempotency_expires",
                        columnList = "expires_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyIdempotency extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "idempotency_id",
            nullable = false,
            updatable = false
    )
    private Long idempotencyId;

    @Column(
            name = "member_id",
            nullable = false,
            updatable = false
    )
    private Long memberId;

    @Column(
            name = "endpoint_key",
            nullable = false,
            updatable = false,
            length = 200
    )
    private String endpointKey;

    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String idempotencyKey;

    @Column(
            name = "request_hash",
            nullable = false,
            updatable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "processing_status",
            nullable = false,
            length = 20
    )
    private IdempotencyProcessingStatus processingStatus;

    @Column(
            name = "resource_type",
            length = 50
    )
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(
            name = "response_status",
            columnDefinition = "SMALLINT"
    )
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "response_body_json",
            columnDefinition = "JSON"
    )
    private String responseBodyJson;

    @Column(
            name = "expires_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant expiresAt;

    private PropertyIdempotency(
            Long memberId,
            String endpointKey,
            String idempotencyKey,
            String requestHash,
            Instant expiresAt,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.memberId = Objects.requireNonNull(
                memberId,
                "멱등 요청의 memberId는 필수입니다."
        );
        this.endpointKey = requireText(
                endpointKey,
                "멱등 요청의 endpointKey는 필수입니다."
        );
        this.idempotencyKey = requireText(
                idempotencyKey,
                "Idempotency-Key는 필수입니다."
        );
        this.requestHash = requireText(
                requestHash,
                "멱등 요청의 requestHash는 필수입니다."
        );
        this.processingStatus =
                IdempotencyProcessingStatus.PROCESSING;
        this.expiresAt = Objects.requireNonNull(
                expiresAt,
                "멱등 키의 만료 시각은 필수입니다."
        );
    }

    public static PropertyIdempotency start(
            Long memberId,
            String endpointKey,
            String idempotencyKey,
            String requestHash,
            Instant expiresAt,
            ActorContext actorContext
    ) {
        return new PropertyIdempotency(
                memberId,
                endpointKey,
                idempotencyKey,
                requestHash,
                expiresAt,
                actorContext
        );
    }

    public void complete(
            String resourceType,
            Long resourceId,
            Integer responseStatus,
            String responseBodyJson,
            ActorContext actorContext
    ) {
        validateProcessingStatus();

        this.processingStatus =
                IdempotencyProcessingStatus.COMPLETED;
        this.resourceType = requireText(
                resourceType,
                "완료된 멱등 요청의 resourceType은 필수입니다."
        );
        this.resourceId = Objects.requireNonNull(
                resourceId,
                "완료된 멱등 요청의 resourceId는 필수입니다."
        );
        this.responseStatus = validateResponseStatus(responseStatus);
        this.responseBodyJson = requireText(
                responseBodyJson,
                "완료된 멱등 요청의 응답 본문은 필수입니다."
        );

        recordUpdate(actorContext);
    }

    public void fail(
            Integer responseStatus,
            String responseBodyJson,
            ActorContext actorContext
    ) {
        validateProcessingStatus();

        this.processingStatus =
                IdempotencyProcessingStatus.FAILED;
        this.responseStatus = validateResponseStatus(responseStatus);
        this.responseBodyJson = requireText(
                responseBodyJson,
                "실패한 멱등 요청의 응답 본문은 필수입니다."
        );

        recordUpdate(actorContext);
    }

    public boolean hasSameRequestHash(String requestHash) {
        return Objects.equals(
                this.requestHash,
                requestHash
        );
    }

    public boolean isProcessing() {
        return processingStatus
                == IdempotencyProcessingStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return processingStatus
                == IdempotencyProcessingStatus.COMPLETED;
    }

    public boolean isFailed() {
        return processingStatus
                == IdempotencyProcessingStatus.FAILED;
    }

    public boolean isExpired(Instant currentTime) {
        Objects.requireNonNull(
                currentTime,
                "현재 시각은 필수입니다."
        );

        return !expiresAt.isAfter(currentTime);
    }

    public void softDelete(
            ActorContext actorContext,
            Instant deletedAt,
            String deleteReason
    ) {
        if (isDeleted()) {
            return;
        }

        recordDeletion(
                actorContext,
                deletedAt,
                deleteReason
        );
    }

    private void validateProcessingStatus() {
        if (!isProcessing()) {
            throw new IllegalStateException(
                    "PROCESSING 상태인 멱등 요청만 완료 또는 실패로 변경할 수 있습니다."
            );
        }
    }

    private static Integer validateResponseStatus(
            Integer responseStatus
    ) {
        if (
                responseStatus == null
                        || responseStatus < 100
                        || responseStatus > 599
        ) {
            throw new IllegalArgumentException(
                    "HTTP 응답 상태 코드는 100 이상 599 이하여야 합니다."
            );
        }

        return responseStatus;
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}
