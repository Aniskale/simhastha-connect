package com.simhastha.packages;
import java.util.concurrent.*;
/** Async seam for a future railway provider. TRAIN_API_KEY / train.api.key are intentionally not consumed until one is configured. */
public final class TrainSearchService {
    public CompletableFuture<TrainSearchResult> searchAsync(TrainSearchRequest request) {
        return CompletableFuture.supplyAsync(() -> { throw new CompletionException(new TrainProviderException("Live train search provider is not configured yet.")); });
    }
}
