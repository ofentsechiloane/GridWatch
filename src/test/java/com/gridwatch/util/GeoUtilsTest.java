package com.gridwatch.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    private static final double JHB_LAT = -26.2041;
    private static final double JHB_LON = 28.0473;

    // ---------- haversineMeters ----------

    @Test
    void samePointIsZero() {
        assertThat(GeoUtils.haversineMeters(JHB_LAT, JHB_LON, JHB_LAT, JHB_LON)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111km() {
        assertThat(GeoUtils.haversineMeters(0, 0, 1, 0)).isCloseTo(111_195, within(10.0));
    }

    @Test
    void johannesburgToPretoria() {
        assertThat(GeoUtils.haversineMeters(JHB_LAT, JHB_LON, -25.7479, 28.2293))
                .isCloseTo(53_890, within(50.0));
    }

    @Test
    void longitudeDegreesAreShorterAwayFromTheEquator() {
        // 0.01 degrees of longitude is about 1.11 km at the equator but only about 998 m in Johannesburg
        double atEquator = GeoUtils.haversineMeters(0, 28.0, 0, 28.01);
        double atJohannesburg = GeoUtils.haversineMeters(JHB_LAT, 28.0, JHB_LAT, 28.01);

        assertThat(atJohannesburg).isCloseTo(998, within(5.0));
        assertThat(atJohannesburg).isLessThan(atEquator);
    }

    @Test
    void isSymmetric() {
        double ab = GeoUtils.haversineMeters(-26.2, 28.0, -25.7, 28.2);
        double ba = GeoUtils.haversineMeters(-25.7, 28.2, -26.2, 28.0);
        assertThat(ab).isEqualTo(ba);
    }

    @Test
    void antipodalPointsDoNotProduceNaN() {
        // Half the Earth's circumference
        assertThat(GeoUtils.haversineMeters(0, 0, 0, 180)).isCloseTo(20_015_087, within(100.0));
    }

    // ---------- boundingBox: shape ----------

    @Test
    void boundingBoxContainsTheCircle() {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(JHB_LAT, JHB_LON, 2000);

        assertThat(box.minLat()).isCloseTo(JHB_LAT - 2000 / 111_195.0, within(1e-4));
        assertThat(box.maxLat()).isCloseTo(JHB_LAT + 2000 / 111_195.0, within(1e-4));
        assertThat(box.minLon()).isLessThan(JHB_LON);
        assertThat(box.maxLon()).isGreaterThan(JHB_LON);
    }

    @Test
    void boundingBoxEdgesAreTheRequestedRadiusAway() {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(JHB_LAT, JHB_LON, 2000);

        assertThat(GeoUtils.haversineMeters(JHB_LAT, JHB_LON, box.maxLat(), JHB_LON)).isCloseTo(2000, within(0.01));
        assertThat(GeoUtils.haversineMeters(JHB_LAT, JHB_LON, JHB_LAT, box.maxLon())).isCloseTo(2000, within(0.01));
        assertThat(GeoUtils.haversineMeters(JHB_LAT, JHB_LON, JHB_LAT, box.minLon())).isCloseTo(2000, within(0.01));
    }

    @Test
    void boundingBoxIsWiderInDegreesAwayFromTheEquator() {
        GeoUtils.BoundingBox atEquator = GeoUtils.boundingBox(0, 28.0, 2000);
        GeoUtils.BoundingBox atSixtySouth = GeoUtils.boundingBox(-60, 28.0, 2000);

        double equatorWidth = atEquator.maxLon() - atEquator.minLon();
        double southWidth = atSixtySouth.maxLon() - atSixtySouth.minLon();

        assertThat(southWidth).isCloseTo(equatorWidth * 2, within(1e-6));   // cos(60 degrees) = 0.5
    }

    @Test
    void zeroRadiusGivesASinglePoint() {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(JHB_LAT, JHB_LON, 0);

        assertThat(box.minLat()).isEqualTo(box.maxLat());
        assertThat(box.minLon()).isEqualTo(box.maxLon());
    }

    @Test
    void boundingBoxIsClampedToValidCoordinates() {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(89.99, 179.99, 50_000);

        assertThat(box.maxLat()).isEqualTo(90.0);
        assertThat(box.maxLon()).isEqualTo(180.0);
    }

    // ---------- boundingBox: validation ----------

    @Test
    void boundingBoxRejectsNegativeRadius() {
        assertThatThrownBy(() -> GeoUtils.boundingBox(JHB_LAT, JHB_LON, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundingBoxRejectsInfiniteRadius() {
        assertThatThrownBy(() -> GeoUtils.boundingBox(JHB_LAT, JHB_LON, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundingBoxRejectsOutOfRangeLatitude() {
        assertThatThrownBy(() -> GeoUtils.boundingBox(91, JHB_LON, 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundingBoxRejectsOutOfRangeLongitude() {
        assertThatThrownBy(() -> GeoUtils.boundingBox(JHB_LAT, 181, 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundingBoxRejectsNaNCoordinates() {
        assertThatThrownBy(() -> GeoUtils.boundingBox(Double.NaN, JHB_LON, 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}