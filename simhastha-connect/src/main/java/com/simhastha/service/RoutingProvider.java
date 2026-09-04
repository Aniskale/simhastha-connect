package com.simhastha.service;

import java.util.List;
import java.util.Optional;

public interface RoutingProvider {
    record Point(double latitude, double longitude) { }
    record Route(List<Point> geometry, double distanceMetres, double durationSeconds) { }
    Optional<Route> route(Point source, Point destination, String mode) throws Exception;
}
