package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.region.constant.RegionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
                    MAX(ST_Y(aggregate_region.center_location)),
                    (
                        MAX(region_boundary.south_west_latitude)
                        + MAX(region_boundary.north_east_latitude)
                    ) / 2
                ) AS latitude,
                COALESCE(
                    MAX(ST_X(aggregate_region.center_location)),
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
              AND property.deleted_at IS NULL
              AND property_address.deleted_at IS NULL
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

        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toQueryRow)
                .toList();
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
