package com.simhastha.service;

import com.simhastha.model.BusinessMedia;
import com.simhastha.model.PublicBusinessItem;
import com.simhastha.model.PublicBusinessListing;

import java.util.List;

/** Isolated, local-only presentation fixtures. They never overwrite Firestore production records. */
public final class PresentationBusinessCatalog {
    private PresentationBusinessCatalog() { }

    public static List<PublicBusinessListing> listings() {
        return List.of(
                business("presentation-locker", "presentation-owner-locker", "Simhastha Secure Locker Centre",
                        "Local Service", "Locker Service", "Secure short-duration lockers for pilgrims near Ramkund.",
                        "Ramkund, Panchavati, Nashik", "Ramkund Road", "Panchavati", "Nashik", "20.0064", "73.7904",
                        "10:00 AM – 10:00 PM", "Starts at ₹80", "0253-240-1101", "locker@simhastha.demo",
                        "CCTV monitored, photo ID required, valuables declaration, collection before closing.",
                        "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=1200&q=80",
                        List.of(item("locker-small", "presentation-locker", "Small Locker", "Locker", "Handbag and essentials locker.", "80", "50", "32", "CCTV, digital lock, attendant", "Available"),
                                item("locker-medium", "presentation-locker", "Medium Locker", "Locker", "Cabin-bag sized secure locker.", "140", "30", "18", "CCTV, digital lock, charging point", "Available"),
                                item("locker-large", "presentation-locker", "Large Locker", "Locker", "Family luggage locker.", "220", "20", "11", "CCTV, digital lock, attendant", "Available"))),
                business("presentation-tent", "presentation-owner-tent", "Simhastha Riverside Tent Camp",
                        "Tent / Camp Stay", "Tent Camp", "Riverside tent stay with guided access and pilgrim support.",
                        "Tapovan Riverside, Nashik", "Tapovan Road", "Panchavati", "Nashik", "20.0204", "73.7788",
                        "24 hours | Check-in 2 PM", "Starts at ₹1,200/night", "0253-240-1102", "camp@simhastha.demo",
                        "Government ID at check-in, quiet hours after 10 PM, no outside cooking.",
                        "https://images.unsplash.com/photo-1504851149312-7a075b496cc7?auto=format&fit=crop&w=1200&q=80",
                        List.of(item("tent-standard", "presentation-tent", "Standard Tent", "Tent", "Comfortable twin-bed tent.", "1200", "25", "16", "Bedding, charging, shared washroom", "Available"),
                                item("tent-family", "presentation-tent", "Family Tent", "Tent", "Spacious tent for four guests.", "2200", "14", "8", "Four beds, power, washroom access", "Available"),
                                item("tent-premium", "presentation-tent", "Premium Tent", "Tent", "Premium riverside tent stay.", "3200", "8", "5", "Attached washroom, breakfast, parking", "Available"))),
                business("presentation-dharamshala", "presentation-owner-dharamshala", "Panchavati Yatri Dharamshala",
                        "Dharamshala", "Pilgrim Stay", "Clean, value-focused rooms and beds for yatris.",
                        "Panchavati, Nashik", "Sita Gufa Road", "Panchavati", "Nashik", "20.0103", "73.7931",
                        "24 hours | Check-in noon", "Starts at ₹450/night", "0253-240-1103", "yatri@simhastha.demo",
                        "ID required, family rooms subject to availability, vegetarian premises.",
                        "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80",
                        List.of(item("dharamshala-shared", "presentation-dharamshala", "Shared Bed", "Dormitory Bed", "Clean shared pilgrim dormitory bed.", "450", "40", "24", "Locker, shared bathroom, drinking water", "Available"),
                                item("dharamshala-standard", "presentation-dharamshala", "Standard Room", "Room", "Two guest standard room.", "1100", "18", "10", "Attached bathroom, hot water, Wi-Fi", "Available"),
                                item("dharamshala-family", "presentation-dharamshala", "Family Room", "Room", "Four guest family room.", "1800", "10", "6", "Attached bathroom, Wi-Fi, parking", "Available"))),
                business("presentation-hotel", "presentation-owner-hotel", "Godavari Residency Nashik",
                        "Accommodation / Hotel", "Hotel", "Professional riverside hotel stay for families and pilgrims.",
                        "Godavari Ghat, Nashik", "Godavari Ghat Road", "Panchavati", "Nashik", "20.0089", "73.7916",
                        "24 hours | Check-in 2 PM", "Starts at ₹2,500/night", "0253-240-1104", "stay@godavariresidency.demo",
                        "Valid government ID required, check-out 11 AM, cancellation subject to rate plan.",
                        "https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1200&q=80",
                        List.of(item("hotel-standard", "presentation-hotel", "Standard Room", "Room", "Queen bed room with city view.", "2500", "20", "12", "AC, Wi-Fi, attached bathroom, parking", "Available"),
                                item("hotel-deluxe", "presentation-hotel", "Deluxe Room", "Room", "King bed room with premium amenities.", "3600", "16", "9", "AC, Wi-Fi, hot water, breakfast, parking", "Available"),
                                item("hotel-family", "presentation-hotel", "Family Room", "Room", "Large two-bed family room.", "4800", "12", "7", "AC, Wi-Fi, two beds, bathroom, parking", "Available")))
        );
    }

    private static PublicBusinessListing business(String id, String owner, String name, String category, String display,
            String description, String location, String address, String area, String city, String lat, String lon,
            String hours, String price, String phone, String email, String policy, String photo, List<PublicBusinessItem> items) {
        return new PublicBusinessListing(id, owner, name, category, display, description + " Policies: " + policy,
                location, address, area, city, lat, lon, "presentation", phone, email, hours, price,
                List.of(new BusinessMedia(id + "-cover", id, owner, photo, "", "cover", true, "presentation", true)), items);
    }

    private static PublicBusinessItem item(String id, String businessId, String name, String type, String description,
            String price, String total, String available, String facilities, String availability) {
        String photo = type.equals("Locker")
                ? "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=640&q=80"
                : type.equals("Tent")
                ? "https://images.unsplash.com/photo-1504851149312-7a075b496cc7?auto=format&fit=crop&w=640&q=80"
                : "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=640&q=80";
        return new PublicBusinessItem(id, businessId, name, type, description, price, total, available, "0", facilities, availability, photo);
    }
}
