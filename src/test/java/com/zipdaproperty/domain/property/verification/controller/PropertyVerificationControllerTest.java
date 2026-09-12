package com.zipdaproperty.domain.property.verification.controller;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationEvidenceType;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationEvidenceRequest;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationSubmitRequest;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationResponse;
import com.zipdaproperty.domain.property.verification.service.PropertyVerificationService;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyVerificationControllerTest {

    private static final Long PROPERTY_ID = 884685586571263701L;
    private static final Long MEMBER_ID = 1001L;

    private final PropertyVerificationService verificationService =
            mock(PropertyVerificationService.class);
    private final PropertyVerificationController controller =
            new PropertyVerificationController(verificationService);
    private final ActorContext actorContext = ActorContext.member(
            MEMBER_ID,
            ActorRole.USER,
            "verification-controller-test"
    );

    @Test
    void verificationEndpoints_matchConfirmedApiContract() throws Exception {
        RequestMapping baseMapping = PropertyVerificationController.class
                .getAnnotation(RequestMapping.class);

        assertThat(baseMapping.value()).containsExactly(
                "/api/property/properties/{propertyId}"
        );
        assertPostMapping("submitOwner", "/verifications/owner");
        assertPostMapping("submitTenant", "/verifications/tenant");
        assertPostMapping("resubmit", "/reverification");
    }

    @Test
    void submitOwner_usesServerSelectedOwnerTypeAndReturnsAccepted() {
        PropertyVerificationSubmitRequest request = request();
        PropertyVerificationResponse response =
                mock(PropertyVerificationResponse.class);
        when(verificationService.submitForType(
                PROPERTY_ID,
                PropertyVerificationType.OWNER,
                request,
                actorContext
        )).thenReturn(response);

        var result = controller.submitOwner(
                PROPERTY_ID,
                "0",
                request,
                actorContext
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(verificationService).submitForType(
                PROPERTY_ID,
                PropertyVerificationType.OWNER,
                request,
                actorContext
        );
    }

    @Test
    void submitTenant_usesServerSelectedTenantTypeAndReturnsAccepted() {
        PropertyVerificationSubmitRequest request = request();
        PropertyVerificationResponse response =
                mock(PropertyVerificationResponse.class);
        when(verificationService.submitForType(
                PROPERTY_ID,
                PropertyVerificationType.TENANT,
                request,
                actorContext
        )).thenReturn(response);

        var result = controller.submitTenant(
                PROPERTY_ID,
                "0",
                request,
                actorContext
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(verificationService).submitForType(
                PROPERTY_ID,
                PropertyVerificationType.TENANT,
                request,
                actorContext
        );
    }

    @Test
    void resubmit_delegatesRenewalAndReturnsAccepted() {
        PropertyVerificationSubmitRequest request = request();
        PropertyVerificationResponse response =
                mock(PropertyVerificationResponse.class);
        when(verificationService.resubmit(
                PROPERTY_ID,
                request,
                actorContext
        )).thenReturn(response);

        var result = controller.resubmit(
                PROPERTY_ID,
                "0",
                request,
                actorContext
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(verificationService).resubmit(
                PROPERTY_ID,
                request,
                actorContext
        );
    }

    private void assertPostMapping(
            String methodName,
            String expectedPath
    ) throws Exception {
        Method method = PropertyVerificationController.class
                .getDeclaredMethod(
                        methodName,
                        Long.class,
                        String.class,
                        PropertyVerificationSubmitRequest.class,
                        ActorContext.class
                );
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        assertThat(mapping.value()).containsExactly(expectedPath);
    }

    private PropertyVerificationSubmitRequest request() {
        return new PropertyVerificationSubmitRequest(
                0L,
                List.of(new PropertyVerificationEvidenceRequest(
                        884700000000000003L,
                        PropertyVerificationEvidenceType.REGISTRY_DOCUMENT,
                        0
                ))
        );
    }
}
