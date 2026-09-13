package com.zipdaproperty.domain.report.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportActionRepositoryTest {

    @Test
    void repository_exposesAppendWithoutUpdateOrDeleteApi() {
        assertThat(PropertyReportActionRepository.class.getInterfaces())
                .containsExactly(Repository.class);

        assertThat(Arrays.stream(PropertyReportActionRepository.class.getMethods())
                .map(Method::getName))
                .containsExactly("save");
    }
}
