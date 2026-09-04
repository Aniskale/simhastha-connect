package com.simhastha.packages;

/** Selected Nashik restaurant meal metadata retained alongside the single MEAL PackageOption. */
public record RestaurantMealSelection(String mealId, String restaurantName, String locality, String mealPlan,
        String categoryTags, int upgradeCharge) { }
