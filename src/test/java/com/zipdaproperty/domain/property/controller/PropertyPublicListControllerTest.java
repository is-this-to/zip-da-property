package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.request.PropertyPublicListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicListResponse;
import com.zipdaproperty.domain.property.service.PropertyPublicListService;
import com.zipdaproperty.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
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
class PropertyPublicListControllerTest {

    @Mock
    private PropertyPublicListService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PropertyPublicListController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void bindBoundsFiltersSortCursorAndSize() throws Exception {
        when(service.findProperties(any(), org.mockito.ArgumentMatchers.eq(1001L))).thenReturn(
                new PropertyPublicListResponse(List.of(), null, false)
        );

        mockMvc.perform(get("/api/property/properties")
                        .param("minLat", "37.4")
                        .param("maxLat", "37.6")
                        .param("minLng", "126.8")
                        .param("maxLng", "127.2")
                        .param("propertyTypes", "APARTMENT", "ROOM")
                        .param("sort", "PRICE_ASC")
                        .param("cursor", "opaque-cursor")
                        .param("size", "30")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk());

        ArgumentCaptor<PropertyPublicListRequest> captor =
                ArgumentCaptor.forClass(PropertyPublicListRequest.class);
        verify(service).findProperties(captor.capture(), org.mockito.ArgumentMatchers.eq(1001L));

        PropertyPublicListRequest request = captor.getValue();
        assertThat(request.propertyTypes()).containsExactlyInAnyOrder(
                PropertyType.APARTMENT,
                PropertyType.ROOM
        );
        assertThat(request.sort()).isEqualTo(PropertyMapSort.PRICE_ASC);
        assertThat(request.cursor()).isEqualTo("opaque-cursor");
        assertThat(request.size()).isEqualTo(30);
    }

    @Test
    void rejectSizeGreaterThanFifty() throws Exception {
        mockMvc.perform(get("/api/property/properties")
                        .param("minLat", "37.4")
                        .param("maxLat", "37.6")
                        .param("minLng", "126.8")
                        .param("maxLng", "127.2")
                        .param("size", "51"))
                .andExpect(status().isBadRequest());
    }
}
