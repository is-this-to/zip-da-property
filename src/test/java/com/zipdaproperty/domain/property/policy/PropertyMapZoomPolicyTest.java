package com.zipdaproperty.domain.property.policy;

import com.zipdaproperty.domain.property.constant.PropertyMapResponseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyMapZoomPolicyTest {

    private final PropertyMapZoomPolicy policy = new PropertyMapZoomPolicy();

    @Test
    @DisplayName("zoomLevel 13~14는 시도 단위 지역 집계다")
    void resolveSidoAggregate() {
        assertThat(policy.resolveResponseType(13))
                .isEqualTo(PropertyMapResponseType.REGION_AGGREGATE);
        assertThat(policy.resolveAggregationRegionLevel(13)).isEqualTo(1);
        assertThat(policy.resolveAggregationRegionLevel(14)).isEqualTo(1);
    }

    @Test
    @DisplayName("zoomLevel 11~12는 시군구 단위 지역 집계다")
    void resolveSigunguAggregate() {
        assertThat(policy.resolveAggregationRegionLevel(11)).isEqualTo(2);
        assertThat(policy.resolveAggregationRegionLevel(12)).isEqualTo(2);
    }

    @Test
    @DisplayName("zoomLevel 8~10은 읍면동 단위 지역 집계다")
    void resolveEupMyeonDongAggregate() {
        assertThat(policy.resolveAggregationRegionLevel(8)).isEqualTo(3);
        assertThat(policy.resolveAggregationRegionLevel(10)).isEqualTo(3);
    }

    @Test
    @DisplayName("zoomLevel 5~7은 프런트 클러스터링용 좌표 응답이다")
    void resolvePropertyPoints() {
        assertThat(policy.resolveResponseType(5))
                .isEqualTo(PropertyMapResponseType.PROPERTY_POINTS);
        assertThat(policy.resolveResponseType(7))
                .isEqualTo(PropertyMapResponseType.PROPERTY_POINTS);
    }

    @Test
    @DisplayName("zoomLevel 1~4는 개별 가격 마커 응답이다")
    void resolvePropertyMarker() {
        assertThat(policy.resolveResponseType(1))
                .isEqualTo(PropertyMapResponseType.PROPERTY_MARKER);
        assertThat(policy.resolveResponseType(4))
                .isEqualTo(PropertyMapResponseType.PROPERTY_MARKER);
    }

    @Test
    @DisplayName("지역 집계가 아닌 확대 수준에서는 집계 단계를 구할 수 없다")
    void rejectAggregationLevelForPropertyZoom() {
        assertThatThrownBy(() -> policy.resolveAggregationRegionLevel(7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zoomLevel 8 이상");
    }
}
