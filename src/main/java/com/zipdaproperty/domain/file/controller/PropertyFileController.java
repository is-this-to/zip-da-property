package com.zipdaproperty.domain.file.controller;

import com.zipdaproperty.domain.file.request.PropertyFileCompleteRequest;
import com.zipdaproperty.domain.file.request.UploadSessionCreateRequest;
import com.zipdaproperty.domain.file.response.PropertyFileCompleteResponse;
import com.zipdaproperty.domain.file.response.UploadSessionCreateResponse;
import com.zipdaproperty.domain.file.service.PropertyFileCompleteService;
import com.zipdaproperty.domain.file.service.UploadSessionService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Property File API",
        description = "매물 파일 업로드 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property-files")
public class PropertyFileController {

    private final UploadSessionService uploadSessionService;
    private final PropertyFileCompleteService propertyFileCompleteService;

    @Operation(
            summary = "파일 업로드 세션 생성",
            description = "파일 메타데이터를 등록하고 MinIO presigned PUT URL을 발급합니다."
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.INVALID_FILE_TYPE,
            CustomResponseCode.FILE_TOO_LARGE,
            CustomResponseCode.FILE_MANAGED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.DB_DUPLICATED_KEY_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping("/upload-sessions")
    public ResponseEntity<GlobalResponseDTO<UploadSessionCreateResponse>> createUploadSession(
            @Valid
            @RequestBody
            UploadSessionCreateRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        UploadSessionCreateResponse response = uploadSessionService.create(
                request,
                actorContext
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(response));
    }

    @Operation(
            summary = "파일 업로드 완료",
            description = "MinIO 객체의 크기, 이미지 형식과 체크섬을 검증하고 업로드를 완료합니다."
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.INVALID_FILE_TYPE,
            CustomResponseCode.FILE_TOO_LARGE,
            CustomResponseCode.UPLOAD_SESSION_EXPIRED,
            CustomResponseCode.FILE_OWNERSHIP_REQUIRED,
            CustomResponseCode.FILE_MANAGED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping("/{fileId}/complete")
    public ResponseEntity<GlobalResponseDTO<PropertyFileCompleteResponse>> complete(
            @PathVariable
            Long fileId,

            @Valid
            @RequestBody
            PropertyFileCompleteRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyFileCompleteResponse response = propertyFileCompleteService.complete(
                fileId,
                request,
                actorContext
        );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }
}
