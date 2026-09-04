package com.simhastha.model;
public record EmergencyAlert(String id, String title, String message, Severity severity, String locationLabel, String affectedArea, double latitude, double longitude, String createdAt, String expiresAt) {
  public enum Severity { CRITICAL, HIGH, INFO }
  public boolean hasLocation() {
    return Double.isFinite(latitude) && Double.isFinite(longitude)
        && !(latitude == 0D && longitude == 0D)
        && latitude >= -90D && latitude <= 90D
        && longitude >= -180D && longitude <= 180D;
  }
}
