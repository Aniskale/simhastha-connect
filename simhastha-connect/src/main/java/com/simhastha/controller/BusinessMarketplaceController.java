package com.simhastha.controller;

import com.simhastha.model.PublicBusinessListing;
import com.simhastha.service.BusinessMarketplaceService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class BusinessMarketplaceController {

    private final BusinessMarketplaceService service = new BusinessMarketplaceService();

    public CompletableFuture<List<PublicBusinessListing>> loadApprovedBusinesses(String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.loadApprovedBusinesses(idToken);
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new BusinessMarketplaceException(exception);
            }
        });
    }

    public List<PublicBusinessListing> filter(List<PublicBusinessListing> businesses, String query, String category) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "All" : category.trim();
        return businesses.stream()
                .filter(business -> "All".equalsIgnoreCase(normalizedCategory)
                        || categoryMatches(business, normalizedCategory))
                .filter(business -> normalizedQuery.isBlank() || business.searchText().contains(normalizedQuery))
                .toList();
    }

    private boolean categoryMatches(PublicBusinessListing business, String category) {
        if ("More".equalsIgnoreCase(category)) {
            return true;
        }
        String display = business.displayCategory() == null ? "" : business.displayCategory();
        String raw = business.category() == null ? "" : business.category();
        return display.equalsIgnoreCase(category) || raw.toLowerCase(Locale.ROOT).contains(category.toLowerCase(Locale.ROOT));
    }

    public static final class BusinessMarketplaceException extends RuntimeException {
        public BusinessMarketplaceException(Throwable cause) {
            super(cause);
        }
    }
}
