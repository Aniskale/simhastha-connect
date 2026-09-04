package com.simhastha.packages;
public record FlightOption(String airline, String flightNumber, FlightSegment departure, FlightSegment arrival, String duration, FlightStatus status, String aircraft) { }
