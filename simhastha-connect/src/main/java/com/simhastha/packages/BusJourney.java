package com.simhastha.packages;
import java.time.LocalDate;
/** Curated internal demo schedule. Charges remain package charges, never operator fares. */
public record BusJourney(String operator, BusType type, String fromCity, String toCity, String departureTime,
        String arrivalTime, String duration, String boardingPoint, String dropPoint, String demoSeats, LocalDate journeyDate) { }
