package com.zipdaproperty.domain.property.idempotency.service;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.idempotency.entity.PropertyIdempotency;
import com.zipdaproperty.domain.property.idempotency.repository.PropertyIdempotencyRepository;
import com.zipdaproperty.domain.property.response.PropertyCreateResponse;
import com.zipdaproperty.domain.property.service.PropertyCreateService;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
public class PropertyIdempotencyService {

    private static final String CREATE_PROPERTY_ENDPOINT =
            "POST:/api/properties";

    private static final String PROPERTY_RESOURCE_TYPE =
            "PROPERTY";

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private static final Duration IDEMPOTENCY_RETENTION_PERIOD =
            Duration.ofHours(24);

    private final PropertyIdempotencyRepository
            propertyIdempotencyRepository;

    private final PropertyCreateService propertyCreateService;

    private final ObjectMapper objectMapper;

    private final TransactionTemplate transactionTemplate;

    public PropertyIdempotencyService(
            PropertyIdempotencyRepository
                    propertyIdempotencyRepository,
            PropertyCreateService propertyCreateService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.propertyIdempotencyRepository =
                propertyIdempotencyRepository;
        this.propertyCreateService = propertyCreateService;
        this.objectMapper = objectMapper;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    public PropertyCreateResponse create(
            String idempotencyKey,
            PropertyCreateCommand command,
            ActorContext actorContext
    ) {
        PropertyCreateCommand requiredCommand =
                requireCommand(command);

        ActorContext requiredActorContext =
                requireMemberActor(actorContext);

        String normalizedIdempotencyKey =
                normalizeIdempotencyKey(idempotencyKey);

        String requestHash =
                createRequestHash(requiredCommand);

        PreparationResult preparationResult = prepare(
                normalizedIdempotencyKey,
                requestHash,
                requiredActorContext
        );

        if (!preparationResult.shouldExecute()) {
            return preparationResult.cachedResponse();
        }

        try {
            return executeInTransaction(
                    () -> executeCreationInTransaction(
                            normalizedIdempotencyKey,
                            requestHash,
                            requiredCommand,
                            requiredActorContext
                    )
            );
        } catch (RuntimeException exception) {
            recordFailure(
                    normalizedIdempotencyKey,
                    requestHash,
                    requiredActorContext,
                    exception
            );

            throw exception;
        }
    }

    private PreparationResult prepare(
            String idempotencyKey,
            String requestHash,
            ActorContext actorContext
    ) {
        try {
            return executeInTransaction(
                    () -> prepareInTransaction(
                            idempotencyKey,
                            requestHash,
                            actorContext
                    )
            );
        } catch (DataIntegrityViolationException exception) {
            return executeInTransaction(
                    () -> resolveAfterConcurrentInsert(
                            idempotencyKey,
                            requestHash,
                            actorContext
                    )
            );
        }
    }

    private PreparationResult prepareInTransaction(
            String idempotencyKey,
            String requestHash,
            ActorContext actorContext
    ) {
        Instant currentTime = Instant.now();

        Optional<PropertyIdempotency> existing =
                propertyIdempotencyRepository
                        .findByMemberIdAndEndpointKeyAndIdempotencyKey(
                                actorContext.memberId(),
                                CREATE_PROPERTY_ENDPOINT,
                                idempotencyKey
                        );

        if (existing.isPresent()) {
            return handleExisting(
                    existing.get(),
                    idempotencyKey,
                    requestHash,
                    currentTime,
                    actorContext
            );
        }

        return registerProcessing(
                idempotencyKey,
                requestHash,
                currentTime,
                actorContext
        );
    }

    private PreparationResult resolveAfterConcurrentInsert(
            String idempotencyKey,
            String requestHash,
            ActorContext actorContext
    ) {
        PropertyIdempotency existing =
                propertyIdempotencyRepository
                        .findByMemberIdAndEndpointKeyAndIdempotencyKey(
                                actorContext.memberId(),
                                CREATE_PROPERTY_ENDPOINT,
                                idempotencyKey
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        CustomResponseCode.DB_ERROR,
                                        "동시 요청 이후 멱등 기록을 찾을 수 없습니다."
                                )
                        );

        return handleExisting(
                existing,
                idempotencyKey,
                requestHash,
                Instant.now(),
                actorContext
        );
    }

    private PreparationResult handleExisting(
            PropertyIdempotency existing,
            String idempotencyKey,
            String requestHash,
            Instant currentTime,
            ActorContext actorContext
    ) {
        if (existing.isExpired(currentTime)) {
            propertyIdempotencyRepository.delete(existing);
            propertyIdempotencyRepository.flush();

            return registerProcessing(
                    idempotencyKey,
                    requestHash,
                    currentTime,
                    actorContext
            );
        }

        if (!existing.hasSameRequestHash(requestHash)) {
            throw new BusinessException(
                    CustomResponseCode.IDEMPOTENCY_CONFLICT,
                    "동일한 Idempotency-Key가 다른 요청에 사용되었습니다."
            );
        }

        if (existing.isCompleted()) {
            return PreparationResult.replay(
                    readCompletedResponse(existing)
            );
        }

        if (existing.isProcessing()) {
            throw new BusinessException(
                    CustomResponseCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                    "동일한 Idempotency-Key 요청을 현재 처리하고 있습니다."
            );
        }

        if (existing.isFailed()) {
            propertyIdempotencyRepository.delete(existing);
            propertyIdempotencyRepository.flush();

            return registerProcessing(
                    idempotencyKey,
                    requestHash,
                    currentTime,
                    actorContext
            );
        }

        throw new BusinessException(
                CustomResponseCode.SYSTEM_ERROR,
                "처리할 수 없는 멱등 요청 상태입니다."
        );
    }

    private PreparationResult registerProcessing(
            String idempotencyKey,
            String requestHash,
            Instant currentTime,
            ActorContext actorContext
    ) {
        PropertyIdempotency idempotency =
                PropertyIdempotency.start(
                        actorContext.memberId(),
                        CREATE_PROPERTY_ENDPOINT,
                        idempotencyKey,
                        requestHash,
                        currentTime.plus(
                                IDEMPOTENCY_RETENTION_PERIOD
                        ),
                        actorContext
                );

        propertyIdempotencyRepository.saveAndFlush(idempotency);

        return PreparationResult.executeRequired();
    }

    private PropertyCreateResponse executeCreationInTransaction(
            String idempotencyKey,
            String requestHash,
            PropertyCreateCommand command,
            ActorContext actorContext
    ) {
        PropertyIdempotency idempotency =
                propertyIdempotencyRepository
                        .findByMemberIdAndEndpointKeyAndIdempotencyKey(
                                actorContext.memberId(),
                                CREATE_PROPERTY_ENDPOINT,
                                idempotencyKey
                        )
                        .orElseThrow(
                                () -> new BusinessException(
                                        CustomResponseCode.DB_ERROR,
                                        "처리를 시작한 멱등 요청 기록이 없습니다."
                                )
                        );

        if (!idempotency.hasSameRequestHash(requestHash)) {
            throw new BusinessException(
                    CustomResponseCode.IDEMPOTENCY_CONFLICT,
                    "멱등 요청 본문이 최초 요청과 일치하지 않습니다."
            );
        }

        if (idempotency.isCompleted()) {
            return readCompletedResponse(idempotency);
        }

        if (!idempotency.isProcessing()) {
            throw new BusinessException(
                    CustomResponseCode.SYSTEM_ERROR,
                    "매물 등록을 실행할 수 없는 멱등 요청 상태입니다."
            );
        }

        PropertyCreateResponse response =
                propertyCreateService.create(
                        command,
                        actorContext
                );

        String responseBodyJson =
                writeResponseBody(response);

        idempotency.complete(
                PROPERTY_RESOURCE_TYPE,
                response.propertyId(),
                HttpStatus.CREATED.value(),
                responseBodyJson,
                actorContext
        );

        return response;
    }

    private void recordFailure(
            String idempotencyKey,
            String requestHash,
            ActorContext actorContext,
            RuntimeException originalException
    ) {
        try {
            executeInTransaction(
                    () -> {
                        Optional<PropertyIdempotency> existing =
                                propertyIdempotencyRepository
                                        .findByMemberIdAndEndpointKeyAndIdempotencyKey(
                                                actorContext.memberId(),
                                                CREATE_PROPERTY_ENDPOINT,
                                                idempotencyKey
                                        );

                        if (
                                existing.isPresent()
                                        && existing
                                        .get()
                                        .hasSameRequestHash(requestHash)
                                        && existing
                                        .get()
                                        .isProcessing()
                        ) {
                            CustomResponseCode responseCode =
                                    resolveResponseCode(
                                            originalException
                                    );

                            GlobalResponseDTO<Void> errorResponse =
                                    GlobalResponseDTO.from(responseCode);

                            String responseBodyJson =
                                    objectMapper.writeValueAsString(
                                            errorResponse
                                    );

                            existing.get().fail(
                                    responseCode
                                            .getHttpStatus()
                                            .value(),
                                    responseBodyJson,
                                    actorContext
                            );
                        }

                        return Boolean.TRUE;
                    }
            );
        } catch (RuntimeException failureRecordingException) {
            log.error(
                    "Failed to record idempotency failure. key={}",
                    idempotencyKey,
                    failureRecordingException
            );
        }
    }

    private PropertyCreateResponse readCompletedResponse(
            PropertyIdempotency idempotency
    ) {
        try {
            return objectMapper.readValue(
                    idempotency.getResponseBodyJson(),
                    PropertyCreateResponse.class
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to read cached idempotency response. id={}",
                    idempotency.getIdempotencyId(),
                    exception
            );

            throw new BusinessException(
                    CustomResponseCode.SYSTEM_ERROR,
                    "저장된 멱등 응답을 복원할 수 없습니다."
            );
        }
    }

    private String writeResponseBody(
            PropertyCreateResponse response
    ) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to serialize property creation response.",
                    exception
            );

            throw new BusinessException(
                    CustomResponseCode.SYSTEM_ERROR,
                    "매물 등록 응답을 저장할 수 없습니다."
            );
        }
    }

    private String createRequestHash(
            PropertyCreateCommand command
    ) {
        try {
            byte[] requestBytes =
                    objectMapper.writeValueAsBytes(command);

            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes =
                    messageDigest.digest(requestBytes);

            return HexFormat
                    .of()
                    .formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }

    private PropertyCreateCommand requireCommand(
            PropertyCreateCommand command
    ) {
        if (command == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "매물 등록 요청 본문은 필수입니다."
            );
        }

        return command;
    }

    private ActorContext requireMemberActor(
            ActorContext actorContext
    ) {
        if (
                actorContext == null
                        || !actorContext.isMemberRequest()
        ) {
            throw new BusinessException(
                    CustomResponseCode.UNAUTHENTICATED,
                    "회원 요청 정보가 필요합니다."
            );
        }

        return actorContext;
    }

    private String normalizeIdempotencyKey(
            String idempotencyKey
    ) {
        if (
                idempotencyKey == null
                        || idempotencyKey.isBlank()
        ) {
            throw new BusinessException(
                    CustomResponseCode.IDEMPOTENCY_KEY_REQUIRED,
                    "Idempotency-Key 헤더는 필수입니다."
            );
        }

        String normalizedKey = idempotencyKey.trim();

        if (
                normalizedKey.length()
                        > MAX_IDEMPOTENCY_KEY_LENGTH
        ) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "Idempotency-Key는 100자를 초과할 수 없습니다."
            );
        }

        return normalizedKey;
    }

    private CustomResponseCode resolveResponseCode(
            RuntimeException exception
    ) {
        if (exception instanceof BusinessException businessException) {
            return businessException.getCustomResponseCode();
        }

        return CustomResponseCode.SYSTEM_ERROR;
    }

    private <T> T executeInTransaction(
            Supplier<T> action
    ) {
        T result = transactionTemplate.execute(
                transactionStatus -> action.get()
        );

        return Objects.requireNonNull(
                result,
                "트랜잭션 처리 결과는 null일 수 없습니다."
        );
    }

    private record PreparationResult(
            boolean shouldExecute,
            PropertyCreateResponse cachedResponse
    ) {

        private static PreparationResult executeRequired() {
            return new PreparationResult(
                    true,
                    null
            );
        }

        private static PreparationResult replay(
                PropertyCreateResponse cachedResponse
        ) {
            return new PreparationResult(
                    false,
                    Objects.requireNonNull(
                            cachedResponse,
                            "재사용할 멱등 응답은 필수입니다."
                    )
            );
        }
    }
}