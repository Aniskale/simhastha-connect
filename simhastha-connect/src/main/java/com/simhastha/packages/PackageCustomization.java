package com.simhastha.packages;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Per-user, in-memory choice state. The immutable package catalogue is never altered. */
public final class PackageCustomization {
    private final String packageId; private final String packageCode; private final Map<PackageOptionType, PackageOption> single = new EnumMap<>(PackageOptionType.class);
    private final Map<String, PackageOption> optional = new LinkedHashMap<>(); private final Map<String, Integer> quantities = new LinkedHashMap<>();
    private String mealPreference = "Standard Vegetarian";
    private FlightOption selectedFlight;
    private TrainSearchRequest trainSearch;
    private TrainJourney selectedTrain;
    private BusJourney selectedBus;
    private HotelStaySelection selectedHotel;
    private RestaurantMealSelection selectedRestaurantMeal;
    private LocalTransportSelection selectedLocalTransport;
    private final Map<String, KumbhExperienceSelection> selectedKumbhExperiences = new LinkedHashMap<>();
    private final Map<String, SightseeingSelection> selectedSightseeing = new LinkedHashMap<>();
    private final Map<String, PackageAddonSelection> selectedPackageAddons = new LinkedHashMap<>();
    public PackageCustomization(String packageId, String packageCode) { this.packageId = packageId; this.packageCode = packageCode == null ? packageId : packageCode; }
    public String packageId() { return packageId; } public String packageCode() { return packageCode; }
    public void select(PackageOption option) { if (option != null) single.put(option.type(), option); }
    public PackageOption selected(PackageOptionType type) { return single.get(type); }
    public void toggle(PackageOption option, boolean selected) { if (option == null || option.mandatory()) return; if (selected) { optional.put(option.id(), option); quantities.putIfAbsent(option.id(), 1); } else { optional.remove(option.id()); quantities.remove(option.id()); } }
    public boolean selected(PackageOption option) { return option != null && (option.included() || option.mandatory() || optional.containsKey(option.id()) || option.equals(single.get(option.type()))); }
    public Collection<PackageOption> optionalSelections() { return optional.values(); }
    public int quantity(PackageOption option) { return quantities.getOrDefault(option.id(), 1); }
    public void quantity(PackageOption option, int value) { if (option != null && option.quantityAllowed()) quantities.put(option.id(), Math.max(1, value)); }
    public String mealPreference() { return mealPreference; } public void mealPreference(String value) { mealPreference = value == null ? "Standard Vegetarian" : value; }
    public FlightOption selectedFlight() { return selectedFlight; }
    public void selectedFlight(FlightOption flight) { selectedFlight = flight; }
    public TrainSearchRequest trainSearch() { return trainSearch; }
    public void trainSearch(TrainSearchRequest value) { trainSearch = value; }
    public TrainJourney selectedTrain() { return selectedTrain; }
    public void selectedTrain(TrainJourney value) { selectedTrain = value; }
    public BusJourney selectedBus() { return selectedBus; }
    public void selectedBus(BusJourney value) { selectedBus = value; }
    public HotelStaySelection selectedHotel() { return selectedHotel; }
    public void selectedHotel(HotelStaySelection value) { selectedHotel = value; }
    public RestaurantMealSelection selectedRestaurantMeal() { return selectedRestaurantMeal; }
    public void selectedRestaurantMeal(RestaurantMealSelection value) { selectedRestaurantMeal = value; }
    public LocalTransportSelection selectedLocalTransport() { return selectedLocalTransport; }
    public void selectedLocalTransport(LocalTransportSelection value) { selectedLocalTransport = value; }
    public void selectKumbhExperience(KumbhExperienceSelection value) { if (value != null) selectedKumbhExperiences.put(value.id(), value); }
    public void removeKumbhExperience(String id) { selectedKumbhExperiences.remove(id); }
    public Collection<KumbhExperienceSelection> selectedKumbhExperiences() { return Collections.unmodifiableCollection(selectedKumbhExperiences.values()); }
    public void selectSightseeing(SightseeingSelection value) { if (value != null) selectedSightseeing.put(value.id(), value); }
    public void removeSightseeing(String id) { selectedSightseeing.remove(id); }
    public Collection<SightseeingSelection> selectedSightseeing() { return Collections.unmodifiableCollection(selectedSightseeing.values()); }
    public void selectPackageAddon(PackageAddonSelection value) { if (value != null) selectedPackageAddons.put(value.id(), value); }
    public void removePackageAddon(String id) { selectedPackageAddons.remove(id); }
    public Collection<PackageAddonSelection> selectedPackageAddons() { return Collections.unmodifiableCollection(selectedPackageAddons.values()); }
    /** A real selection replaces the previous package transport, while merely opening another panel does not. */
    public void activateTravel(PackageOption option) { single.remove(PackageOptionType.TRAVEL_CLASS); selectedFlight=null; selectedTrain=null; selectedBus=null; select(option); }
    public void clearActiveTravel() { single.remove(PackageOptionType.TRAVEL_CLASS); selectedFlight=null; selectedTrain=null; selectedBus=null; }
    public PackageCustomizationSnapshot snapshot(PackagePricingService.Pricing pricing) { return new PackageCustomizationSnapshot(packageId, packageCode, selectedLabels(), pricing.components(), pricing.total(), "INR", Instant.now().toString()); }
    private Map<String, String> selectedLabels() { Map<String,String> result = new LinkedHashMap<>(); single.forEach((type, option) -> result.put(type.name(), option.id() + "|" + option.label())); optional.values().forEach(option -> result.put(option.id(), option.label() + " x" + quantity(option))); result.put("MEAL_PREFERENCE", mealPreference); if(selectedFlight!=null) result.put("FLIGHT", selectedFlight.airline()+"|"+selectedFlight.flightNumber()+"|"+selectedFlight.departure().airportCode()+"|"+selectedFlight.departure().scheduledTime()+"|"+selectedFlight.arrival().airportCode()+"|"+selectedFlight.arrival().scheduledTime()); if(selectedRestaurantMeal!=null) result.put("RESTAURANT_MEAL", selectedRestaurantMeal.mealId()+"|"+selectedRestaurantMeal.restaurantName()+"|"+selectedRestaurantMeal.locality()+"|"+selectedRestaurantMeal.mealPlan()); if(selectedLocalTransport!=null) result.put("LOCAL_TRANSPORT", selectedLocalTransport.id()+"|"+selectedLocalTransport.name()+"|"+selectedLocalTransport.description()); selectedKumbhExperiences.values().forEach(value -> result.put("KUMBH_"+value.id(), value.name())); selectedSightseeing.values().forEach(value -> result.put("SIGHTSEEING_"+value.id(), value.name()+"|"+value.location())); selectedPackageAddons.values().forEach(value -> result.put("PUJA_ADDON_"+value.id(), value.name()+"|"+value.category())); return result; }
}
