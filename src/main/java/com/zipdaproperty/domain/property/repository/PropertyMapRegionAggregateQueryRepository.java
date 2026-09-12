package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import com.zipdaproperty.domain.region.constant.RegionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PropertyMapRegionAggregateQueryRepository {

    private static final String REGION_AGGREGATE_SQL = """
            SELECT
                aggregate_region.region_id,
                aggregate_region.region_name,
                aggregate_region.region_level,
                aggregate_region.region_type,
                COALESCE(
                    MAX(ST_X(aggregate_region.center_location)),
                    (
                        MAX(region_boundary.south_west_latitude)
                        + MAX(region_boundary.north_east_latitude)
                    ) / 2
                ) AS latitude,
                COALESCE(
                    MAX(ST_Y(aggregate_region.center_location)),
                    (
                        MAX(region_boundary.south_west_longitude)
                        + MAX(region_boundary.north_east_longitude)
                    ) / 2
                ) AS longitude,
                COUNT(DISTINCT property.property_id) AS property_count
            FROM property_address property_address
            INNER JOIN property property
                ON property.property_id = property_address.property_id
            INNER JOIN region source_region
                ON source_region.region_id = property.region_id
            LEFT JOIN region first_parent
                ON first_parent.region_id = source_region.parent_region_id
            LEFT JOIN region second_parent
                ON second_parent.region_id = first_parent.parent_region_id
            LEFT JOIN region third_parent
                ON third_parent.region_id = second_parent.parent_region_id
            INNER JOIN region aggregate_region
                ON aggregate_region.region_id =
                    CASE
                        WHEN source_region.region_level = :targetRegionLevel
                            THEN source_region.region_id
                        WHEN first_parent.region_level = :targetRegionLevel
                            THEN first_parent.region_id
                        WHEN second_parent.region_level = :targetRegionLevel
                            THEN second_parent.region_id
                        WHEN third_parent.region_level = :targetRegionLevel
                            THEN third_parent.region_id
                        ELSE NULL
                    END
            LEFT JOIN region_boundary region_boundary
                ON region_boundary.region_id = aggregate_region.region_id
                AND region_boundary.simplification_level = 0
                AND region_boundary.deleted_at IS NULL
            WHERE property.publication_status = 'PUBLISHED'
              AND property.transaction_status IN ('AVAILABLE', 'RESERVED')
              AND property.verification_status IN (
                  'OWNER_VERIFIED',
                  'TENANT_VERIFIED',
                  'AGENT_VERIFIED'
              )
              AND property.deleted_at IS NULL
              AND property_address.deleted_at IS NULL
              AND property.property_type IN (:propertyTypes)
              AND property.transaction_type IN (:transactionTypes)
              AND property.publisher_type IN (:publisherTypes)
              AND (
                  (
                      property.transaction_type = 'SALE'
                      AND (
                          :minSalePriceEnabled = FALSE
                          OR property.sale_price >= :minSalePrice
                      )
                      AND (
                          :maxSalePriceEnabled = FALSE
                          OR property.sale_price <= :maxSalePrice
                      )
                  )
                  OR (
                      property.transaction_type = 'JEONSE'
                      AND (
                          :minDepositEnabled = FALSE
                          OR property.deposit >= :minDeposit
                      )
                      AND (
                          :maxDepositEnabled = FALSE
                          OR property.deposit <= :maxDeposit
                      )
                  )
                  OR (
                      property.transaction_type = 'MONTHLY_RENT'
                      AND (
                          :minDepositEnabled = FALSE
                          OR property.deposit >= :minDeposit
                      )
                      AND (
                          :maxDepositEnabled = FALSE
                          OR property.deposit <= :maxDeposit
                      )
                      AND (
                          :minMonthlyRentEnabled = FALSE
                          OR property.monthly_rent >= :minMonthlyRent
                      )
                      AND (
                          :maxMonthlyRentEnabled = FALSE
                          OR property.monthly_rent <= :maxMonthlyRent
                      )
                  )
              )
              AND (
                  :minMaintenanceFeeEnabled = FALSE
                  OR property.maintenance_fee >= :minMaintenanceFee
              )
              AND (
                  :maxMaintenanceFeeEnabled = FALSE
                  OR property.maintenance_fee <= :maxMaintenanceFee
              )
              AND (
                  :minExclusiveAreaEnabled = FALSE
                  OR property.exclusive_area >= :minExclusiveArea
              )
              AND (
                  :maxExclusiveAreaEnabled = FALSE
                  OR property.exclusive_area <= :maxExclusiveArea
              )
              AND (
                  :roomCountMinEnabled = FALSE
                  OR property.room_count >= :roomCountMin
              )
              AND (
                  :roomCountMaxEnabled = FALSE
                  OR property.room_count <= :roomCountMax
              )
              AND (
                  :approvalDateFromEnabled = FALSE
                  OR property.approval_date >= :approvalDateFrom
              )
              AND (
                  :approvalDateToEnabled = FALSE
                  OR property.approval_date <= :approvalDateTo
              )
              AND (
                  :parkingFilterEnabled = FALSE
                  OR property.is_parking_available = :parkingValue
              )
              AND (
                  :elevatorFilterEnabled = FALSE
                  OR property.has_elevator = :elevatorValue
              )
              AND (
                  :petFilterEnabled = FALSE
                  OR property.is_pet_allowed = :petValue
              )
              AND source_region.is_active = TRUE
              AND source_region.deleted_at IS NULL
              AND aggregate_region.is_active = TRUE
              AND aggregate_region.deleted_at IS NULL
              AND MBRIntersects(
                  property_address.public_location,
                  ST_GeomFromText(
                      :boundsWkt,
                      4326,
                      'axis-order=long-lat'
                  )
              )
              AND ST_Intersects(
                  property_address.public_location,
                  ST_GeomFromText(
                      :boundsWkt,
                      4326,
                      'axis-order=long-lat'
                  )
              )
            GROUP BY
                aggregate_region.region_id,
                aggregate_region.region_name,
                aggregate_region.region_level,
                aggregate_region.region_type
            ORDER BY
                property_count DESC,
                aggregate_region.region_id ASC
            """;

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<PropertyMapRegionAggregateQueryRow> findRegionAggregates(
            PropertyMapBounds bounds,
            PropertyMapSearchCondition condition,
            int targetRegionLevel
    ) {
        Query query = entityManager.createNativeQuery(REGION_AGGREGATE_SQL);

        query.setParameter(
                "boundsWkt",
                bounds.toPolygon().toText()
        );
        query.setParameter(
                "targetRegionLevel",
                targetRegionLevel
        );

        bindSearchCondition(
                query,
                condition
        );

        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toQueryRow)
                .toList();
    }

    private void bindSearchCondition(
            Query query,
            PropertyMapSearchCondition condition
    ) {
        NativeQuery<?> nativeQuery =
                query.unwrap(NativeQuery.class);

        nativeQuery.setParameterList(
                "propertyTypes",
                effectiveNames(
                        condition.propertyTypes(),
                        PropertyType.class
                )
        );
        nativeQuery.setParameterList(
                "transactionTypes",
                effectiveNames(
                        condition.transactionTypes(),
                        TransactionType.class
                )
        );
        nativeQuery.setParameterList(
                "publisherTypes",
                effectiveNames(
                        condition.publisherTypes(),
                        PublisherType.class
                )
        );

        bindLongFilter(
                query,
                "minSalePrice",
                condition.minSalePrice()
        );
        bindLongFilter(
                query,
                "maxSalePrice",
                condition.maxSalePrice()
        );
        bindLongFilter(
                query,
                "minDeposit",
                condition.minDeposit()
        );
        bindLongFilter(
                query,
                "maxDeposit",
                condition.maxDeposit()
        );
        bindLongFilter(
                query,
                "minMonthlyRent",
                condition.minMonthlyRent()
        );
        bindLongFilter(
                query,
                "maxMonthlyRent",
                condition.maxMonthlyRent()
        );
        bindLongFilter(
                query,
                "minMaintenanceFee",
                condition.minMaintenanceFee()
        );
        bindLongFilter(
                query,
                "maxMaintenanceFee",
                condition.maxMaintenanceFee()
        );
        bindDecimalFilter(
                query,
                "minExclusiveArea",
                condition.minExclusiveArea()
        );
        bindDecimalFilter(
                query,
                "maxExclusiveArea",
                condition.maxExclusiveArea()
        );
        bindIntegerFilter(
                query,
                "roomCountMin",
                condition.roomCountMin()
        );
        bindIntegerFilter(
                query,
                "roomCountMax",
                condition.roomCountMax()
        );
        bindDateFilter(
                query,
                "approvalDateFrom",
                condition.approvalDateFrom()
        );
        bindDateFilter(
                query,
                "approvalDateTo",
                condition.approvalDateTo()
        );
        bindBooleanFilter(
                query,
                "parking",
                condition.isParkingAvailable()
        );
        bindBooleanFilter(
                query,
                "elevator",
                condition.hasElevator()
        );
        bindBooleanFilter(
                query,
                "pet",
                condition.isPetAllowed()
        );
    }

    private <E extends Enum<E>> List<String> effectiveNames(
            Set<E> selectedValues,
            Class<E> enumType
    ) {
        Set<E> effectiveValues = selectedValues.isEmpty()
                ? EnumSet.allOf(enumType)
                : selectedValues;

        return effectiveValues.stream()
                .map(Enum::name)
                .toList();
    }

    private void bindLongFilter(
            Query query,
            String parameterName,
            Long value
    ) {
        query.setParameter(
                parameterName + "Enabled",
                value != null
        );
        query.setParameter(
                parameterName,
                value == null ? 0L : value
        );
    }

    private void bindDecimalFilter(
            Query query,
            String parameterName,
            BigDecimal value
    ) {
        query.setParameter(
                parameterName + "Enabled",
                value != null
        );
        query.setParameter(
                parameterName,
                value == null ? BigDecimal.ZERO : value
        );
    }

    private void bindIntegerFilter(
            Query query,
            String parameterName,
            Integer value
    ) {
        query.setParameter(
                parameterName + "Enabled",
                value != null
        );
        query.setParameter(
                parameterName,
                value == null ? 0 : value
        );
    }

    private void bindDateFilter(
            Query query,
            String parameterName,
            LocalDate value
    ) {
        query.setParameter(
                parameterName + "Enabled",
                value != null
        );
        query.setParameter(
                parameterName,
                value == null ? LocalDate.of(1970, 1, 1) : value
        );
    }

    private void bindBooleanFilter(
            Query query,
            String parameterPrefix,
            Boolean value
    ) {
        query.setParameter(
                parameterPrefix + "FilterEnabled",
                value != null
        );
        query.setParameter(
                parameterPrefix + "Value",
                Boolean.TRUE.equals(value)
        );
    }

    private PropertyMapRegionAggregateQueryRow toQueryRow(Object[] row) {
        if (row[4] == null || row[5] == null) {
            throw new IllegalStateException(
                    "지역 집계 표시 좌표가 존재하지 않습니다. regionId=" + row[0]
            );
        }

        return new PropertyMapRegionAggregateQueryRow(
                number(row[0]).longValue(),
                String.valueOf(row[1]),
                number(row[2]).intValue(),
                RegionType.valueOf(String.valueOf(row[3])),
                number(row[4]).doubleValue(),
                number(row[5]).doubleValue(),
                number(row[6]).longValue()
        );
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }

        throw new IllegalStateException(
                "숫자 형식의 지도 집계 결과가 필요합니다."
        );
    }
}
