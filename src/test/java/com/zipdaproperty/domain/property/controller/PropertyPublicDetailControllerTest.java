package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.response.PropertyPublicDetailResponse;
import com.zipdaproperty.domain.property.service.PropertyPublicDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyPublicDetailControllerTest {

    private static final Long PROPERTY_ID = 884685586571263701L;
    private final PropertyPublicDetailService service = mock(PropertyPublicDetailService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PropertyPublicDetailController(service))
                .build();
    }

    @Test
    void getPublicDetail_anonymous_returnsPublicFieldsWithoutPrivateLocation() throws Exception {
        when(service.findDetail(PROPERTY_ID, null)).thenReturn(response(false));

        mockMvc.perform(get("/api/properties/{propertyId}", PROPERTY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.propertyId").value(PROPERTY_ID.toString()))
                .andExpect(jsonPath("$.data.publicAddress")
                        .value("서울특별시 강남구 역삼동"))
                .andExpect(jsonPath("$.data.latitude").value(37.51))
                .andExpect(jsonPath("$.data.longitude").value(127.01))
                .andExpect(jsonPath("$.data.isFavorite").value(false))
                .andExpect(jsonPath("$.data.exactRoadAddress").doesNotExist())
                .andExpect(jsonPath("$.data.exactJibunAddress").doesNotExist())
                .andExpect(jsonPath("$.data.detailAddressEncrypted").doesNotExist())
                .andExpect(jsonPath("$.data.exactLocation").doesNotExist())
                .andExpect(jsonPath("$.data.authorMemberId").doesNotExist())
                .andExpect(jsonPath("$.data.riskScore").doesNotExist());

        verify(service).findDetail(PROPERTY_ID, null);
    }

    @Test
    void getPublicDetail_loggedIn_passesMemberId() throws Exception {
        when(service.findDetail(PROPERTY_ID, 1001L)).thenReturn(response(true));

        mockMvc.perform(get("/api/properties/{propertyId}", PROPERTY_ID)
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite").value(true));

        verify(service).findDetail(PROPERTY_ID, 1001L);
    }

    private PropertyPublicDetailResponse response(boolean favorite) {
        return new PropertyPublicDetailResponse(
                PROPERTY_ID,
                null,
                null,
                null,
                PropertyType.APARTMENT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "공개 매물",
                "설명",
                "서울특별시 강남구 역삼동",
                37.51,
                127.01,
                List.of(),
                List.of(),
                3L,
                favorite
        );
    }
}
