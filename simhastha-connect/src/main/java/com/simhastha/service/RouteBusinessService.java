package com.simhastha.service;

import java.util.List;

/** Geometry-aware corridor filter; only approved, active businesses with coordinates are eligible. */
public final class RouteBusinessService {
    public record BusinessPoint(String id, String name, String category, double latitude, double longitude, boolean approved, boolean active) { }
    public List<BusinessPoint> nearRoute(List<GhatNavigationService.Point> geometry, List<BusinessPoint> businesses, double corridorMeters) {
        if (geometry.size() < 2) return List.of();
        return businesses.stream().filter(b -> b.approved() && b.active()).filter(b -> distanceToRoute(b, geometry) <= corridorMeters).toList();
    }
    private double distanceToRoute(BusinessPoint b, List<GhatNavigationService.Point> line) { double best = Double.MAX_VALUE; for (int i = 1; i < line.size(); i++) best = Math.min(best, distanceToSegment(b.latitude(), b.longitude(), line.get(i - 1), line.get(i))); return best; }
    private double distanceToSegment(double lat, double lon, GhatNavigationService.Point a, GhatNavigationService.Point b) { double scale = 111_320d; double x = lon * scale * Math.cos(Math.toRadians(lat)), y = lat * scale, ax = a.longitude() * scale * Math.cos(Math.toRadians(lat)), ay = a.latitude() * scale, bx = b.longitude() * scale * Math.cos(Math.toRadians(lat)), by = b.latitude() * scale; double dx = bx - ax, dy = by - ay, t = dx == 0 && dy == 0 ? 0 : Math.max(0, Math.min(1, ((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy))); return Math.hypot(x-(ax+t*dx), y-(ay+t*dy)); }
}
