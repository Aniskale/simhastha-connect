package com.simhastha.packages;
import java.util.List;
public interface FlightProvider { List<FlightOption> search(FlightSearchRequest request) throws FlightProviderException; }
