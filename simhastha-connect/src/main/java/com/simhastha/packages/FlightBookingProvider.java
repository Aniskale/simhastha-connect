package com.simhastha.packages;

/** Future certified-ticketing boundary. No implementation is configured, so the UI never claims ticket issuance. */
public interface FlightBookingProvider {
    FlightReservationRequest requestReservation(FlightOption flight, int travellers, CabinClass cabinClass);
    record FlightReservationRequest(FlightOption flight, int travellers, CabinClass cabinClass, String status) { }
}
