package com.zipdaproperty.domain.property.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PropertyWriteObservabilityFilterTest {

    private final PropertyWriteApiObservationRecorder recorder =
            mock(PropertyWriteApiObservationRecorder.class);

    private final PropertyWriteObservabilityFilter filter =
            new PropertyWriteObservabilityFilter(recorder);

    @Test
    void doFilter_propertyWriteRequest_recordsStatusAndDuration()
            throws ServletException, IOException {
        MockHttpServletRequest request = request(
                "PATCH",
                "/api/property/properties/1001"
        );
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        response.setStatus(409);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(recorder).record(
                eq("PATCH"),
                eq(409),
                anyLong()
        );
    }

    @Test
    void doFilter_propertyReadRequest_doesNotRecordObservation()
            throws ServletException, IOException {
        MockHttpServletRequest request = request(
                "GET",
                "/api/property/properties/1001"
        );
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(recorder, never()).record(
                anyString(),
                anyInt(),
                anyLong()
        );
    }

    @Test
    void doFilter_similarPrefixDoesNotRecordObservation()
            throws ServletException, IOException {
        MockHttpServletRequest request = request(
                "POST",
                "/api/property/properties-legacy"
        );
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(recorder, never()).record(
                anyString(),
                anyInt(),
                anyLong()
        );
    }

    @Test
    void doFilter_unhandledFailure_recordsServerErrorAndRethrows()
            throws ServletException, IOException {
        MockHttpServletRequest request = request(
                "POST",
                "/api/property/properties"
        );
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        ServletException failure = new ServletException("test failure");

        doThrow(failure)
                .when(filterChain)
                .doFilter(request, response);

        assertThatThrownBy(
                () -> filter.doFilter(
                        request,
                        response,
                        filterChain
                )
        ).isSameAs(failure);

        verify(recorder).record(
                eq("POST"),
                eq(500),
                anyLong()
        );
    }

    private MockHttpServletRequest request(
            String method,
            String requestUri
    ) {
        return new MockHttpServletRequest(method, requestUri);
    }
}
