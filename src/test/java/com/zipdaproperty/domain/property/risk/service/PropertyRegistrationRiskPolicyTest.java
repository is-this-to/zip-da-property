package com.zipdaproperty.domain.property.risk.service;

import com.zipdaproperty.domain.option.command.PropertyOptionCreateCommand;
import com.zipdaproperty.domain.property.command.PropertyAddressCommand;
import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.risk.constant.PropertyRiskDecision;
import com.zipdaproperty.domain.property.risk.constant.PropertyRiskRuleCode;
import com.zipdaproperty.domain.property.risk.model.PropertyRiskAssessmentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyRegistrationRiskPolicyTest {

    private final PropertyRegistrationRiskPolicy policy =
            new PropertyRegistrationRiskPolicy();

    @Test
    void evaluate_noRiskSignal_returnsPass() {
        PropertyRiskAssessmentResult result = policy.evaluate(
                command(500_000_000L, "정상 매물", "정상적인 매물 설명입니다."),
                Set.of(),
                Set.of(),
                Set.of()
        );

        assertThat(result.score()).isEqualByComparingTo("0");
        assertThat(result.decision()).isEqualTo(PropertyRiskDecision.PASS);
        assertThat(result.matchedRuleCodes()).isEmpty();
    }

    @Test
    void evaluate_sameAddressPricePublisher_returnsInReview() {
        PropertyRiskAssessmentResult result = policy.evaluate(
                command(500_000_000L, "정상 매물", "정상적인 매물 설명입니다."),
                Set.of(100L),
                Set.of(100L),
                Set.of()
        );

        assertThat(result.score()).isEqualByComparingTo("70");
        assertThat(result.decision())
                .isEqualTo(PropertyRiskDecision.IN_REVIEW);
        assertThat(result.matchedRuleCodes()).containsExactly(
                PropertyRiskRuleCode.SAME_ADDRESS_PRICE_PUBLISHER
        );
        assertThat(result.duplicatePropertyId()).isEqualTo(100L);
    }

    @Test
    void evaluate_sameAddressAndImage_returnsBlockedExactDuplicate() {
        PropertyRiskAssessmentResult result = policy.evaluate(
                command(500_000_000L, "정상 매물", "정상적인 매물 설명입니다."),
                Set.of(100L),
                Set.of(),
                Set.of(100L)
        );

        assertThat(result.score()).isEqualByComparingTo("100");
        assertThat(result.decision())
                .isEqualTo(PropertyRiskDecision.BLOCKED);
        assertThat(result.exactDuplicateDetected()).isTrue();
        assertThat(result.matchedRuleCodes()).containsExactly(
                PropertyRiskRuleCode.DUPLICATE_IMAGE_CHECKSUM,
                PropertyRiskRuleCode.SAME_ADDRESS_IMAGE
        );
        assertThat(result.duplicatePropertyId()).isEqualTo(100L);
    }

    @Test
    void evaluate_forbiddenWordAndExternalContact_returnsInReview() {
        PropertyRiskAssessmentResult result = policy.evaluate(
                command(
                        500_000_000L,
                        "미끼매물",
                        "카카오톡으로 연락 주세요."
                ),
                Set.of(),
                Set.of(),
                Set.of()
        );

        assertThat(result.score()).isEqualByComparingTo("70");
        assertThat(result.decision())
                .isEqualTo(PropertyRiskDecision.IN_REVIEW);
        assertThat(result.matchedRuleCodes()).containsExactly(
                PropertyRiskRuleCode.FORBIDDEN_WORD,
                PropertyRiskRuleCode.EXTERNAL_CONTACT_INDUCEMENT
        );
    }

    @Test
    void evaluate_abnormallyLowSalePrice_addsRiskWithoutBlocking() {
        PropertyRiskAssessmentResult result = policy.evaluate(
                command(1L, "정상 매물", "정상적인 매물 설명입니다."),
                Set.of(),
                Set.of(),
                Set.of()
        );

        assertThat(result.score()).isEqualByComparingTo("30");
        assertThat(result.decision()).isEqualTo(PropertyRiskDecision.PASS);
        assertThat(result.matchedRuleCodes()).containsExactly(
                PropertyRiskRuleCode.ABNORMAL_PRICE
        );
    }

    private PropertyCreateCommand command(
            Long salePrice,
            String title,
            String description
    ) {
        return new PropertyCreateCommand(
                53390L,
                null,
                PublisherType.DIRECT_OWNER,
                PropertyType.APARTMENT,
                TransactionType.SALE,
                salePrice,
                null,
                null,
                150_000L,
                new BigDecimal("84.99"),
                new BigDecimal("59.99"),
                3,
                1,
                5,
                20,
                "중층",
                "남향",
                LocalDate.of(2020, 1, 1),
                "공동주택",
                true,
                true,
                false,
                title,
                description,
                List.of(1001L),
                new PropertyAddressCommand(
                        "대구 수성구 달구벌대로 2450",
                        "대구광역시 수성구 범어동 123",
                        "2726010100",
                        new BigDecimal("128.625123"),
                        new BigDecimal("35.859321")
                ),
                List.of(new PropertyOptionCreateCommand("PARKING", "true"))
        );
    }
}
