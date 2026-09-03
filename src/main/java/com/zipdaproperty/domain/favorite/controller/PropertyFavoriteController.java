package com.zipdaproperty.domain.favorite.controller;

import com.zipdaproperty.domain.favorite.request.PropertyFavoriteUpdateRequest;
import com.zipdaproperty.domain.favorite.response.PropertyFavoriteListResponse;
import com.zipdaproperty.domain.favorite.response.PropertyFavoriteUpdateResponse;
import com.zipdaproperty.domain.favorite.service.PropertyFavoriteService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PropertyFavoriteController {

    private final PropertyFavoriteService propertyFavoriteService;

    @PutMapping("/properties/{propertyId}/favorite")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.FAVORITE_TARGET_UNAVAILABLE,
            CustomResponseCode.METHOD_NOT_ALLOWED
    })
    public ResponseEntity<GlobalResponseDTO<PropertyFavoriteUpdateResponse>> updateFavorite(
            @PathVariable
            @Positive(message = "매물 ID는 0보다 커야 합니다.")
            Long propertyId,
            @Valid
            @RequestBody
            PropertyFavoriteUpdateRequest request,
            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyFavoriteUpdateResponse response =
                propertyFavoriteService.updateFavorite(
                        propertyId,
                        request.favorite(),
                        actorContext
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    @GetMapping("/me/property-favorites")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.METHOD_NOT_ALLOWED
    })
    public ResponseEntity<GlobalResponseDTO<PropertyFavoriteListResponse>> getMyFavorites(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "cursor는 0 이상이어야 합니다.")
            int cursor,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size,
            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyFavoriteListResponse response =
                propertyFavoriteService.getMyFavorites(
                        actorContext,
                        cursor,
                        size
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }
}
