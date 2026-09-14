package com.zipdaproperty.domain.file.controller;

import com.zipdaproperty.domain.file.service.PropertyFileDeleteService;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PropertyFileDeleteControllerTest {

    private static final Long FILE_ID = 101L;
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            1001L,
            ActorRole.USER,
            "property-file-delete-controller-test"
    );

    private final PropertyFileDeleteService deleteService =
            mock(PropertyFileDeleteService.class);
    private final PropertyFileDeleteController controller =
            new PropertyFileDeleteController(deleteService);

    @Test
    void deleteEndpoint_matchesConfirmedApiContract() throws Exception {
        RequestMapping baseMapping = PropertyFileDeleteController.class
                .getAnnotation(RequestMapping.class);
        Method method = PropertyFileDeleteController.class.getDeclaredMethod(
                "delete",
                Long.class,
                ActorContext.class
        );
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);

        assertThat(baseMapping.value()).containsExactly("/api/property-files");
        assertThat(deleteMapping.value()).containsExactly("/{fileId}");
    }

    @Test
    void delete_delegatesToServiceAndReturnsOk() {
        var response = controller.delete(FILE_ID, ACTOR_CONTEXT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(deleteService).delete(FILE_ID, ACTOR_CONTEXT);
    }
}
