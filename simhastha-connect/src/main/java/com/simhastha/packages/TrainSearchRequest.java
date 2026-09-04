package com.simhastha.packages;
import java.time.LocalDate;
public record TrainSearchRequest(String fromStation, String toStation, LocalDate journeyDate, int travellers, TrainTravelClass travelClass) { }
