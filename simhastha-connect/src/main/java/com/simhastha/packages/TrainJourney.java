package com.simhastha.packages;
import java.time.LocalDate;
import java.util.List;
/** Provider-neutral structure; no local/demo journey values are supplied. */
public record TrainJourney(String trainNumber, String trainName, String fromStationCode, String fromStationName,
        String toStationCode, String toStationName, String departureTime, String arrivalTime, String duration,
        LocalDate travelDate, List<TrainTravelClass> availableClasses, String runningDays, String availabilityStatus,
        Integer fare, String providerSource) { }
