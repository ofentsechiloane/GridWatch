package com.gridwatch.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test
    void samePointIsZero() {
        assertThat(GeoUtils.haversineMeters(-26.2041, 28.0473, -26.2041, 28.0473)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111km() {
        assertThat(GeoUtils.haversineMeters(0, 0, 1, 0)).isCloseTo(111_195, within(10.0));
    }

    @Test
    void johannesburgToPretoria() {
        assertThat(GeoUtils.haversineMeters(-26.2041, 28.0473, -25.7479, 28.2293))
                .isCloseTo(53_890, within(50.0));
    }

    @Test
    void isSymmetric() {
        double ab = GeoUtils.haversineMeters(-26.2, 28.0, -25.7, 28.2);
        double ba = GeoUtils.haversineMeters(-25.7, 28.2, -26.2, 28.0);
        assertThat(ab).isEqualTo(ba);
    }
}
