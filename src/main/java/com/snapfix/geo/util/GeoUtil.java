package com.snapfix.geo.util;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;

public class GeoUtil {

    private static final GeometryFactory factory = new GeometryFactory();

    public static Point createPoint(double lat, double lng) {
        return factory.createPoint(new Coordinate(lng, lat)); // lng first!
    }
}