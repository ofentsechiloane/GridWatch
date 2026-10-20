package com.gridwatch.util;

/**
 * Geographic helpers used for grouping reports and outages by location.
 * All distances are in metres and all coordinates are in decimal degrees.
 */
public final class GeoUtils {

    /** Mean Earth radius in metres. */
    public static final double EARTH_RADIUS_M = 6_371_000;

    private static final double MAX_LATITUDE = 90.0;
    private static final double MAX_LONGITUDE = 180.0;

    /** Stops the longitude maths from blowing up near the poles, where cos(lat) approaches 0. */
    private static final double MIN_COS_LATITUDE = 0.01;

    private GeoUtils() { }

    /** A rectangle in degrees, used to pre-filter database rows before exact distance checks. */
    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) { }

    /**
     * Builds a rectangle that fully contains the circle of the given radius around a point.
     *
     * <p>The rectangle is slightly larger than the circle, so filter the rows it returns
     * with {@link #haversineMeters} to get exact distances. It does not wrap around the
     * 180th meridian and is not meant for polar regions, which is fine for South Africa.
     *
     * @param lat          centre latitude, -90 to 90
     * @param lon          centre longitude, -180 to 180
     * @param radiusMeters circle radius, zero or more and finite
     * @throws IllegalArgumentException if an argument is out of range, NaN or infinite
     */
    public static BoundingBox boundingBox(double lat, double lon, double radiusMeters) {
        requireValidCoordinates(lat, lon);
        if (!Double.isFinite(radiusMeters) || radiusMeters < 0) {
            throw new IllegalArgumentException(
                    "radiusMeters must be a finite number >= 0: " + radiusMeters);
        }

        double latDelta = Math.toDegrees(radiusMeters / EARTH_RADIUS_M);
        double cosLat = Math.max(Math.cos(Math.toRadians(lat)), MIN_COS_LATITUDE);
        double lonDelta = latDelta / cosLat;

        return new BoundingBox(
                clamp(lat - latDelta, MAX_LATITUDE),
                clamp(lat + latDelta, MAX_LATITUDE),
                clamp(lon - lonDelta, MAX_LONGITUDE),
                clamp(lon + lonDelta, MAX_LONGITUDE));
    }

    /**
     * Great-circle distance between two points, in metres.
     * Coordinates are not validated here because this runs in tight loops;
     * validate at the API boundary instead.
     */
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double sinHalfLat = Math.sin(dLat / 2);
        double sinHalfLon = Math.sin(dLon / 2);
        double a = sinHalfLat * sinHalfLat
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * sinHalfLon * sinHalfLon;

        // Rounding can push 'a' just above 1, which would make asin return NaN.
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    private static void requireValidCoordinates(double lat, double lon) {
        // Written as "in range" checks because every comparison with NaN is false,
        // so a NaN fails here instead of slipping through.
        if (!isWithin(lat, MAX_LATITUDE) || !isWithin(lon, MAX_LONGITUDE)) {
            throw new IllegalArgumentException("Invalid coordinates: lat=" + lat + ", lon=" + lon);
        }
    }

    private static boolean isWithin(double value, double limit) {
        return value >= -limit && value <= limit;
    }

    private static double clamp(double value, double limit) {
        return Math.max(-limit, Math.min(limit, value));
    }
}