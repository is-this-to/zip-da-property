package com.zipdaproperty.domain.option.controller;

import com.zipdaproperty.domain.option.response.PropertyOptionCodeListResponse;
import com.zipdaproperty.domain.option.service.PropertyOptionQueryService;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PropertyOptionController {

    private final PropertyOptionQueryService propertyOptionQueryService;

    @GetMapping("/property-option-codes")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyOptionCodeListResponse>> getOptionCodes(
            @RequestParam
            PropertyType propertyType
    ) {
        PropertyOptionCodeListResponse response =
                propertyOptionQueryService.getOptionCodes(propertyType);

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }
}