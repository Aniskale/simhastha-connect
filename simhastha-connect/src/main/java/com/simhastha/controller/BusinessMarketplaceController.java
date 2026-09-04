package com.simhastha.controller;

import com.simhastha.model.PublicBusinessListing;
import com.simhastha.service.BusinessMarketplaceService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BusinessMarketplaceController {

    private static final Logger LOGGER = Logger.getLogger(BusinessMarketplaceController.class.getName());
    private final BusinessMarketplaceService service = new BusinessMarketplaceService();

    public CompletableFuture<List<PublicBusinessListing>> loadApprovedBusinesses(String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.loadApprovedBusinesses(idToken);
            } catch (IOException | InterruptedException exception) {
                LOGGER.log(Level.WARNING, "Business marketplace load failed.", exception);
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new BusinessMarketplaceException(exception);
            }
        });
    }

    public CompletableFuture<Optional<PublicBusinessListing>> loadApprovedBusiness(String businessId, String idToken) {
        return loadApprovedBusinesses(idToken).thenApply(businesses -> businesses.stream()
                .filter(business -> business.businessId().equals(businessId))
                .findFirst());
    }

    public List<PublicBusinessListing> filter(List<PublicBusinessListing> businesses, String query, String category) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "All" : category.trim();
        return businesses.stream()
                .filter(business -> "All".equalsIgnoreCase(normalizedCategory)
                        || categoryMatches(business, normalizedCategory))
                .filter(business -> normalizedQuery.isBlank() || queryMatchesBusiness(business, normalizedQuery))
                .toList();
    }

    private boolean queryMatchesBusiness(PublicBusinessListing business, String query) {
        if (business.searchText().contains(query)) {
            return true;
        }
        String category = (business.displayCategory() + " " + business.category()).toLowerCase(Locale.ROOT);
        return switch (query) {
            case "food", "foods", "eat", "eating", "restaurant", "restaurants", "snacks", "prasadam" ->
                    category.contains("food") || category.contains("restaurant")
                            || category.contains("snack") || category.contains("prasadam")
                            || category.contains("tea");
            case "stay", "stays", "room", "rooms", "lodging", "accommodation" ->
                    category.contains("stay") || category.contains("hotel")
                            || category.contains("lodge") || category.contains("accommodation");
            case "tent", "tents", "camp", "camps" -> category.contains("tent") || category.contains("camp");
            case "shop", "shops", "store", "stores" -> category.contains("shop") || category.contains("store");
            case "puja", "pooja", "prayer" -> category.contains("puja") || category.contains("pooja");
            default -> false;
        };
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
