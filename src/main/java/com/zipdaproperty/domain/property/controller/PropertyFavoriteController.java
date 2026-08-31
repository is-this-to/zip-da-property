package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.request.PropertyFavoriteUpdateRequest;
import com.zipdaproperty.domain.property.response.PropertyFavoriteUpdateResponse;
import com.zipdaproperty.domain.property.service.PropertyFavoriteService;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
public class PropertyFavoriteController {

    private final PropertyFavoriteService propertyFavoriteService;

    @PutMapping("/{propertyId}/favorite")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    public ResponseEntity<
            GlobalResponseDTO<PropertyFavoriteUpdateResponse>
            > updateFavorite(
            @PathVariable
            @Positive(message = "매물 ID는 0보다 커야 합니다.")
            Long propertyId,
            @Valid
            @RequestBody
            PropertyFavoriteUpdateRequest request,
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
}
