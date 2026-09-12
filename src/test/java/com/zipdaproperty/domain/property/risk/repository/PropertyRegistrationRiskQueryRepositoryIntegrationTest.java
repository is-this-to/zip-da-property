package com.zipdaproperty.domain.property.risk.repository;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(
        named = "PROPERTY_RISK_QUERY_TEST_ENABLED",
        matches = "true"
)
@SpringBootTest
@Transactional(readOnly = true)
class PropertyRegistrationRiskQueryRepositoryIntegrationTest {

    private static final String NON_EXISTING_ADDRESS =
            "ZIPDA_RISK_QUERY_TEST_ADDRESS_DOES_NOT_EXIST";

    @Autowired
    private PropertyRegistrationRiskQueryRepository repository;

    @Test
    void riskCandidateQueries_executeAgainstMySqlSchema() {
        PropertyCreateCommand command = mock(PropertyCreateCommand.class);
        when(command.salePrice()).thenReturn(987_654_321_012L);
        when(command.deposit()).thenReturn(null);
        when(command.monthlyRent()).thenReturn(null);

        assertThat(repository.findActivePropertyIdsByAddress(
                NON_EXISTING_ADDRESS,
                null
        )).isEmpty();

        assertThat(repository
                .findActivePropertyIdsByAddressPricePublisher(
                        NON_EXISTING_ADDRESS,
                        null,
                        command,
                        Long.MAX_VALUE
                )).isEmpty();

        assertThat(repository.findActivePropertyIdsByImageChecksums(
                Set.of("f".repeat(64))
        )).isEmpty();
    }
}
