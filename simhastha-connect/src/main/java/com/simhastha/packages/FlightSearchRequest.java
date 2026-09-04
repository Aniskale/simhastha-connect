package com.simhastha.packages;

import java.time.LocalDate;
public record FlightSearchRequest(String originAirport, String destinationAirport, LocalDate departureDate, int travellers, CabinClass cabinClass) { }
