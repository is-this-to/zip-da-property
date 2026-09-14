package com.zipdaproperty.domain.file.controller;

import com.zipdaproperty.domain.file.service.PropertyFileDeleteService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Property File API",
        description = "매물 파일 관리 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property-files")
public class PropertyFileDeleteController {

    private final PropertyFileDeleteService propertyFileDeleteService;

    @Operation(
            summary = "미연결 파일 삭제",
            description = "본인이 소유한 미연결 파일을 삭제합니다."
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.FILE_OWNERSHIP_REQUIRED,
            CustomResponseCode.FILE_MANAGED_ERROR,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<GlobalResponseDTO<Void>> delete(
            @PathVariable
            Long fileId,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        propertyFileDeleteService.delete(fileId, actorContext);

        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
