package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.constant.PropertyMapResponseType;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.request.PropertyMapBoundsRequest;
import com.zipdaproperty.domain.property.response.PropertyMapBoundsResponse;
import com.zipdaproperty.domain.property.service.PropertyMapBoundsService;
import com.zipdaproperty.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PropertyMapControllerTest {

    @Mock
    private PropertyMapBoundsService propertyMapBoundsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PropertyMapController controller =
                new PropertyMapController(
                        propertyMapBoundsService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    @DisplayName("반복 Query Parameter로 복수 필터를 바인딩한다")
    void bindRepeatedFilterQueryParameters() throws Exception {
        when(propertyMapBoundsService.findProperties(any()))
                .thenReturn(
                        PropertyMapBoundsResponse.of(
                                PropertyMapResponseType.PROPERTY_POINTS,
                                List.of(),
                                0L,
                                false
                        )
                );

        mockMvc.perform(
                        get("/api/property/properties/map")
                                .param("minLat", "37.45")
                                .param("maxLat", "37.55")
                                .param("minLng", "126.95")
                                .param("maxLng", "127.10")
                                .param("zoomLevel", "6")
                                .param(
                                        "propertyTypes",
                                        "APARTMENT",
                                        "ROOM"
                                )
                                .param(
                                        "transactionTypes",
                                        "JEONSE",
                                        "MONTHLY_RENT"
                                )
                                .param("sort", "PRICE_ASC")
                )
                .andExpect(status().isOk());

        ArgumentCaptor<PropertyMapBoundsRequest> captor =
                ArgumentCaptor.forClass(
                        PropertyMapBoundsRequest.class
                );

        verify(propertyMapBoundsService)
                .findProperties(captor.capture());

        assertThat(captor.getValue().propertyTypes())
                .containsExactlyInAnyOrder(
                        PropertyType.APARTMENT,
                        PropertyType.ROOM
                );
        assertThat(captor.getValue().transactionTypes())
                .containsExactlyInAnyOrder(
                        TransactionType.JEONSE,
                        TransactionType.MONTHLY_RENT
                );
    }

    @Test
    @DisplayName("지원하지 않는 필터 Enum은 잘못된 요청으로 처리한다")
    void rejectUnsupportedFilterEnum() throws Exception {
        mockMvc.perform(
                        get("/api/property/properties/map")
                                .param("minLat", "37.45")
                                .param("maxLat", "37.55")
                                .param("minLng", "126.95")
                                .param("maxLng", "127.10")
                                .param("zoomLevel", "6")
                                .param(
                                        "propertyTypes",
                                        "UNSUPPORTED"
                                )
                )
                .andExpect(status().isBadRequest());
    }
}
