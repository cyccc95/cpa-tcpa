package org.hansu;

/**
 * Distance, Azimuth, CPA/TCPA calculation class
 *
 * @author hschoi
 * @version 1.0
 */
public class Calculation {

    /**
     * Earth Radius(m unit)
     */
    private static int R = 6371000;

    /**
     * Haversine Formula
     * Calculate distance between two coordinates
     *
     * @param latitude1  Latitude in degree format for first coordinate(ex. 37.13461)
     * @param longitude1 Longitude in degree format for first coordinate(ex. 126.88848)
     * @param latitude2  Latitude in degree format for second coordinate(ex. 37.5011)
     * @param longitude2 Longitude in degree format for second coordinate(ex. 127.67278)
     * @return distance between two coordinates(m unit)
     */
    public double calculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        validateCoordinates(latitude1, longitude1, latitude2, longitude2);

        double lat1Rad = Math.toRadians(latitude1);
        double lon1Rad = Math.toRadians(longitude1);
        double lat2Rad = Math.toRadians(latitude2);
        double lon2Rad = Math.toRadians(longitude2);

        double deltaLat = (lat2Rad - lat1Rad) / 2;
        double deltaLon = (lon2Rad - lon1Rad) / 2;

        double sqrtRoot = Math.sin(deltaLat) * Math.sin(deltaLat) + Math.sin(deltaLon) * Math.sin(deltaLon)
                * Math.cos(lat1Rad) * Math.cos(lat2Rad);

        return 2 * R * Math.atan2(Math.sqrt(sqrtRoot), Math.sqrt(1 - sqrtRoot));
    }

    /**
     * Calculate azimuth between two coordinates
     *
     * @param latitude1  Latitude in degree format for first coordinate(ex. 37.13461)
     * @param longitude1 Longitude in degree format for first coordinate(ex. 126.88848)
     * @param latitude2  Latitude in degree format for second coordinate(ex. 37.5011)
     * @param longitude2 Longitude in degree format for second coordinate(ex. 127.67278)
     * @return azimuth between two coordinates(degree unit)
     */
    public double calculateAzimuth(double latitude1, double longitude1, double latitude2, double longitude2) {
        validateCoordinates(latitude1, longitude1, latitude2, longitude2);

        double lat1Rad = Math.toRadians(latitude1);
        double lon1Rad = Math.toRadians(longitude1);
        double lat2Rad = Math.toRadians(latitude2);
        double lon2Rad = Math.toRadians(longitude2);

        double deltaLon = lon2Rad - lon1Rad;

        double y = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);
        double bearingRad = Math.atan2(y, x);
        double azimuth = (Math.toDegrees(bearingRad) + 360) % 360;
        if (azimuth < 0) azimuth += 360;

        return azimuth;
    }

    /**
     * Calculate cpa/tcpa between two vessels
     *
     * @param latitude1  Latitude in degree format for first vessel(ex. 37.13461)
     * @param longitude1 Longitude in degree format for first vessel(ex. 126.88848)
     * @param sog1       Speed over ground in knots for first vessel(ex. 5.7)
     * @param cog1       Course over ground in degree format for first vessel(ex. 153.1)
     * @param latitude2  Latitude in degree format for second coordinate(ex. 37.5011)
     * @param longitude2 Longitude in degree format for second coordinate(ex. 127.67278)
     * @param sog2       Speed over ground in knots for second vessel(ex. 9.8)
     * @param cog2       Course over ground in degree format for second vessel(ex. 180.6)
     * @return CPA and TCPA between two vessels(cpa : m unit, tcpa : second unit)
     */
    public CpaAndTcpa calculateCpaAndTcpa(double latitude1, double longitude1, double sog1, double cog1,
                                double latitude2, double longitude2, double sog2, double cog2) {
        validateCoordinates(latitude1, longitude1, latitude2, longitude2);
        validateSogAndCog(sog1, cog1, sog2, cog2);

        double vesselDistance = calculateDistance(latitude1, longitude1, latitude2, longitude2);

        double[] position1 = {convertLongitudeEPSG4326toEPSG3857(longitude1), convertLatitudeEPSG4326toEPSG3857(latitude1), 0};
        double[] position2 = {convertLongitudeEPSG4326toEPSG3857(longitude2), convertLatitudeEPSG4326toEPSG3857(latitude2), 0};

        double[] velocity1 = {sog1 * Math.sin((cog1 * Math.PI) / 180), sog1 * Math.cos((cog1 * Math.PI) / 180), 0};
        double[] velocity2 = {sog2 * Math.sin((cog2 * Math.PI) / 180), sog2 * Math.cos((cog2 * Math.PI) / 180), 0};

        double tcpa = calculateCpa(position1, velocity1, position2, velocity2);
        if (tcpa < 0) tcpa = 0;

        double[] cpaPosition1 = {convertLongitudeEPSG3857toEPSG4326(position1[0] + velocity1[0] * tcpa),
                convertLatitudeEPSG3857toEPSG4326(position1[1] + velocity1[1] * tcpa)};
        double[] cpaPosition2 = {convertLongitudeEPSG3857toEPSG4326(position2[0] + velocity2[0] * tcpa),
                convertLatitudeEPSG3857toEPSG4326(position2[1] + velocity2[1] * tcpa)};

        double cpa = calculateDistance(cpaPosition1[1], cpaPosition1[0], cpaPosition2[1], cpaPosition2[0]);
        double tcpaDistance = calculateDistance(latitude1, longitude1, cpaPosition1[1], cpaPosition1[0]);
        double realTcpa = tcpaDistance / ((sog1 * 1.852) / 3600) / 1000;

        return new CpaAndTcpa(cpa, realTcpa);
    }

    private void validateCoordinates(double latitude1, double longitude1, double latitude2, double longitude2) {
        if (latitude1 > 90 || latitude1 < -90)
            throw new IllegalArgumentException("[" + latitude1 + "] Latitude must be between -90 and 90 degrees");
        if (latitude2 > 90 || latitude2 < -90)
            throw new IllegalArgumentException("[" + latitude2 + "] Latitude must be between -90 and 90 degrees");
        if (longitude1 > 180 || longitude1 < -180)
            throw new IllegalArgumentException("[" + longitude1 + "] Longitude must be between -180 and 180 degrees");
        if (longitude2 > 180 || longitude2 < -180)
            throw new IllegalArgumentException("[" + longitude2 + "] Longitude must be between -180 and 180 degrees");
    }

    private void validateSogAndCog(double sog1, double cog1, double sog2, double cog2) {
        if (sog1 < 0 || sog1 > 102)
            throw new IllegalArgumentException("[" + sog1 + "] Sog must be between 0 and 102 knots");
        if (sog2 < 0 || sog2 > 102)
            throw new IllegalArgumentException("[" + sog2 + "] Sog must be between 0 and 102 knots");
        if (cog1 < 0 || cog1 > 360)
            throw new IllegalArgumentException("[" + cog1 + "] Cog must be between 0 and 360 degrees");
        if (cog2 < 0 || cog2 > 360)
            throw new IllegalArgumentException("[" + cog2 + "] Cog must be between 0 and 360 degrees");
    }

    private double convertLatitudeEPSG4326toEPSG3857(double latitude) {
        return Math.log(Math.tan((90 + latitude) * Math.PI / 360)) / (Math.PI / 180) * (20037508.34 / 180);
    }

    private double convertLongitudeEPSG4326toEPSG3857(double longitude) {
        return longitude * (20037508.34 / 180);
    }

    private double convertLatitudeEPSG3857toEPSG4326(double latitude) {
        return 180 / Math.PI * (2 * Math.atan(Math.exp((latitude / 20037508.34) * 180 * Math.PI / 180)) - Math.PI / 2);
    }

    private double convertLongitudeEPSG3857toEPSG4326(double longitude) {
        return (longitude / 20037508.34) * 180;
    }

    private double calculateCpa(double[] position1, double[] velocity1, double[] position2, double[] velocity2) {
        double[] positionDiff = {position2[0] - position1[0], position2[1] - position1[1], position2[2] - position1[2]};
        double[] velocityDiff = {velocity2[0] - velocity1[0], velocity2[1] - velocity1[1], velocity2[2] - velocity1[2]};
        double zahler = (positionDiff[0] * velocityDiff[0]
                + positionDiff[1] * velocityDiff[1]
                + positionDiff[2] * velocityDiff[2])
                * (-1);
        double nenner = velocityDiff[0] * velocityDiff[0] + velocityDiff[1] * velocityDiff[1] + velocityDiff[2] * velocityDiff[2];

        return nenner == 0.0 ? Double.NaN : zahler / nenner;
    }
}