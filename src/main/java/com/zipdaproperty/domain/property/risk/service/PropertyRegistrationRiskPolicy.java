package com.zipdaproperty.domain.property.risk.service;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.risk.constant.PropertyRiskDecision;
import com.zipdaproperty.domain.property.risk.constant.PropertyRiskRuleCode;
import com.zipdaproperty.domain.property.risk.model.PropertyRiskAssessmentResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PropertyRegistrationRiskPolicy {

    static final int REVIEW_THRESHOLD = 70;
    static final int BLOCK_THRESHOLD = 90;

    private static final int SAME_ADDRESS_PRICE_PUBLISHER_SCORE = 70;
    private static final int DUPLICATE_IMAGE_SCORE = 70;
    private static final int SAME_ADDRESS_IMAGE_SCORE = 90;
    private static final int ABNORMAL_PRICE_SCORE = 30;
    private static final int FORBIDDEN_WORD_SCORE = 30;
    private static final int EXTERNAL_CONTACT_SCORE = 40;

    private static final long MIN_SALE_PRICE = 5_000_000L;
    private static final long MAX_SALE_PRICE = 100_000_000_000L;
    private static final long MIN_JEONSE_DEPOSIT = 1_000_000L;
    private static final long MAX_JEONSE_DEPOSIT = 50_000_000_000L;
    private static final long MAX_MONTHLY_DEPOSIT = 10_000_000_000L;
    private static final long MAX_MONTHLY_RENT = 100_000_000L;

    private static final List<String> FORBIDDEN_WORDS = List.of(
            "허위매물",
            "미끼매물",
            "실거래가 조작",
            "다운계약",
            "업계약"
    );

    private static final Pattern EXTERNAL_CONTACT_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.|카카오톡|카톡|오픈채팅|텔레그램|telegram|"
                    + "라인\\s*(아이디|id)|line\\s*id|위챗|wechat|"
                    + "01[016789][- .]?\\d{3,4}[- .]?\\d{4})"
    );

    public PropertyRiskAssessmentResult evaluate(
            PropertyCreateCommand command,
            Set<Long> sameAddressPropertyIds,
            Set<Long> sameAddressPricePublisherPropertyIds,
            Set<Long> duplicateImagePropertyIds
    ) {
        List<PropertyRiskRuleCode> matchedRules = new ArrayList<>();
        int score = 0;

        Set<Long> sameAddressImagePropertyIds =
                intersection(
                        sameAddressPropertyIds,
                        duplicateImagePropertyIds
                );

        if (!sameAddressPricePublisherPropertyIds.isEmpty()) {
            matchedRules.add(
                    PropertyRiskRuleCode.SAME_ADDRESS_PRICE_PUBLISHER
            );
            score += SAME_ADDRESS_PRICE_PUBLISHER_SCORE;
        }

        if (!duplicateImagePropertyIds.isEmpty()) {
            matchedRules.add(
                    PropertyRiskRuleCode.DUPLICATE_IMAGE_CHECKSUM
            );
            score += DUPLICATE_IMAGE_SCORE;
        }

        if (!sameAddressImagePropertyIds.isEmpty()) {
            matchedRules.add(PropertyRiskRuleCode.SAME_ADDRESS_IMAGE);
            score += SAME_ADDRESS_IMAGE_SCORE;
        }

        if (isAbnormalPrice(command)) {
            matchedRules.add(PropertyRiskRuleCode.ABNORMAL_PRICE);
            score += ABNORMAL_PRICE_SCORE;
        }

        String searchableText = normalizeSearchableText(
                command.title(),
                command.description()
        );

        if (containsForbiddenWord(searchableText)) {
            matchedRules.add(PropertyRiskRuleCode.FORBIDDEN_WORD);
            score += FORBIDDEN_WORD_SCORE;
        }

        if (EXTERNAL_CONTACT_PATTERN.matcher(searchableText).find()) {
            matchedRules.add(
                    PropertyRiskRuleCode.EXTERNAL_CONTACT_INDUCEMENT
            );
            score += EXTERNAL_CONTACT_SCORE;
        }

        int boundedScore = Math.min(score, 100);
        PropertyRiskDecision decision = decide(boundedScore);

        return new PropertyRiskAssessmentResult(
                BigDecimal.valueOf(boundedScore),
                decision,
                matchedRules,
                firstDuplicatePropertyId(
                        sameAddressImagePropertyIds,
                        sameAddressPricePublisherPropertyIds,
                        duplicateImagePropertyIds
                )
        );
    }

    private boolean isAbnormalPrice(PropertyCreateCommand command) {
        TransactionType transactionType = command.transactionType();
        if (transactionType == TransactionType.SALE) {
            return outside(
                    command.salePrice(),
                    MIN_SALE_PRICE,
                    MAX_SALE_PRICE
            );
        }
        if (transactionType == TransactionType.JEONSE) {
            return outside(
                    command.deposit(),
                    MIN_JEONSE_DEPOSIT,
                    MAX_JEONSE_DEPOSIT
            );
        }
        return (command.deposit() != null
                && command.deposit() > MAX_MONTHLY_DEPOSIT)
                || (command.monthlyRent() != null
                && command.monthlyRent() > MAX_MONTHLY_RENT);
    }

    private boolean outside(Long value, long minimum, long maximum) {
        return value != null && (value < minimum || value > maximum);
    }

    private String normalizeSearchableText(String title, String description) {
        return ((title == null ? "" : title)
                + " "
                + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsForbiddenWord(String text) {
        return FORBIDDEN_WORDS.stream().anyMatch(text::contains);
    }

    private Set<Long> intersection(Set<Long> left, Set<Long> right) {
        java.util.LinkedHashSet<Long> result =
                new java.util.LinkedHashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private PropertyRiskDecision decide(int score) {
        if (score >= BLOCK_THRESHOLD) {
            return PropertyRiskDecision.BLOCKED;
        }
        if (score >= REVIEW_THRESHOLD) {
            return PropertyRiskDecision.IN_REVIEW;
        }
        return PropertyRiskDecision.PASS;
    }

    @SafeVarargs
    private Long firstDuplicatePropertyId(Set<Long>... candidates) {
        for (Set<Long> candidate : candidates) {
            if (!candidate.isEmpty()) {
                return candidate.iterator().next();
            }
        }
        return null;
    }
}
