package com.zipdaproperty.domain.option.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptionRepositoryMethodTest {

    @Test
    void jpaRepositories_customQueries_movedToQueryDsl() {
        assertThat(PropertyOptionRepository.class.getDeclaredMethods()).isEmpty();
        assertThat(PropertyTypeOptionRepository.class.getDeclaredMethods()).isEmpty();
        assertThat(PropertyOptionCodeRepository.class.getDeclaredMethods()).isEmpty();
        assertThat(PropertyOptionHistoryRepository.class.getDeclaredMethods()).isEmpty();
    }
}
