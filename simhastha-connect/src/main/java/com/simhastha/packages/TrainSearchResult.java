package com.simhastha.packages;
import java.util.List;
public record TrainSearchResult(List<TrainJourney> journeys, String providerSource) { }
