package com.simhastha.packages;
import java.util.*;
import java.util.concurrent.*;
/** Async UI boundary; JavaFX never calls the network provider on its application thread. */
public final class FlightSearchService {
    private final FlightProvider provider;
    public FlightSearchService() { this(new AviationstackFlightProvider()); }
    public FlightSearchService(FlightProvider provider) { this.provider = provider; }
    public CompletableFuture<List<FlightOption>> searchAsync(FlightSearchRequest request) { return CompletableFuture.supplyAsync(() -> { try { return provider.search(request); } catch (FlightProviderException exception) { throw new CompletionException(exception); } }); }
}
