package com.simhastha.model;

import java.util.List;

/** Development-only emergency locations; never persisted or mixed with Firestore results. */
public final class EmergencyDevelopmentServices {
    private EmergencyDevelopmentServices() { }

    public static List<EmergencyService> create() {
        return List.of(
                service("demo-hospital-panchavati", "Hospital - Panchavati", EmergencyService.Category.HOSPITAL, 20.0112, 73.7931),
                service("demo-hospital-central", "Hospital - Central Nashik", EmergencyService.Category.HOSPITAL, 20.0068, 73.7879),
                service("demo-hospital-college-road", "Hospital - College Road", EmergencyService.Category.HOSPITAL, 20.0182, 73.7816),
                service("demo-hospital-gangapur-road", "Hospital - Gangapur Road", EmergencyService.Category.HOSPITAL, 20.0051, 73.7664),
                service("demo-hospital-mumbai-naka", "Hospital - Mumbai Naka", EmergencyService.Category.HOSPITAL, 19.9958, 73.7819),
                service("demo-camp-ramkund", "Medical Camp - Ramkund", EmergencyService.Category.MEDICAL_CAMP, 20.0071, 73.7922),
                service("demo-camp-panchavati", "Medical Camp - Panchavati", EmergencyService.Category.MEDICAL_CAMP, 20.0135, 73.7962),
                service("demo-camp-godavari-ghat", "Medical Camp - Godavari Ghat", EmergencyService.Category.MEDICAL_CAMP, 20.0044, 73.8001),
                service("demo-camp-tapovan", "Medical Camp - Tapovan", EmergencyService.Category.MEDICAL_CAMP, 20.0188, 73.8082),
                service("demo-camp-sector-4", "Medical Camp - Sector 4", EmergencyService.Category.MEDICAL_CAMP, 20.0028, 73.7896),
                service("demo-aid-ramkund", "First Aid Center - Ramkund", EmergencyService.Category.FIRST_AID, 20.0056, 73.7939),
                service("demo-aid-panchavati", "First Aid Center - Panchavati", EmergencyService.Category.FIRST_AID, 20.0098, 73.7984),
                service("demo-aid-godavari-ghat", "First Aid Center - Godavari Ghat", EmergencyService.Category.FIRST_AID, 20.0019, 73.7970),
                service("demo-aid-tapovan", "First Aid Center - Tapovan", EmergencyService.Category.FIRST_AID, 20.0181, 73.8060),
                service("demo-ambulance-ramkund", "Ambulance Point - Ramkund", EmergencyService.Category.AMBULANCE, 20.0063, 73.7902),
                service("demo-ambulance-panchavati", "Ambulance Point - Panchavati", EmergencyService.Category.AMBULANCE, 20.0127, 73.8014),
                service("demo-ambulance-central", "Ambulance Point - Central Nashik", EmergencyService.Category.AMBULANCE, 20.0157, 73.7898),
                service("demo-ambulance-tapovan", "Ambulance Point - Tapovan", EmergencyService.Category.AMBULANCE, 20.0197, 73.8105),
                service("demo-police-panchavati", "Police Help Center - Panchavati", EmergencyService.Category.POLICE, 20.0124, 73.7901),
                service("demo-police-ramkund", "Police Help Center - Ramkund", EmergencyService.Category.POLICE, 20.0062, 73.7967),
                service("demo-police-central", "Police Help Center - Central Nashik", EmergencyService.Category.POLICE, 20.0169, 73.7974),
                service("demo-police-tapovan", "Police Help Center - Tapovan", EmergencyService.Category.POLICE, 20.0206, 73.8059),
                service("demo-fire-panchavati", "Fire & Safety Unit - Panchavati", EmergencyService.Category.FIRE_SAFETY, 20.0149, 73.8032),
                service("demo-fire-central", "Fire & Safety Unit - Central Nashik", EmergencyService.Category.FIRE_SAFETY, 20.0191, 73.7888),
                service("demo-fire-ramkund", "Fire & Safety Unit - Ramkund", EmergencyService.Category.FIRE_SAFETY, 20.0008, 73.7911),
                service("demo-fire-tapovan", "Fire & Safety Unit - Tapovan", EmergencyService.Category.FIRE_SAFETY, 20.0221, 73.8083),
                service("demo-desk-ramkund", "Emergency Help Desk - Ramkund", EmergencyService.Category.HELP_DESK, 20.0040, 73.7907),
                service("demo-desk-panchavati", "Emergency Help Desk - Panchavati", EmergencyService.Category.HELP_DESK, 20.0108, 73.7993),
                service("demo-desk-central", "Emergency Help Desk - Central Nashik", EmergencyService.Category.HELP_DESK, 20.0177, 73.7944),
                service("demo-desk-tapovan", "Emergency Help Desk - Tapovan", EmergencyService.Category.HELP_DESK, 20.0214, 73.8120),
                service("demo-exit-ramkund-east", "Emergency Exit - Ramkund East", EmergencyService.Category.EMERGENCY_EXIT, 20.0046, 73.8010),
                service("demo-exit-ramkund-west", "Emergency Exit - Ramkund West", EmergencyService.Category.EMERGENCY_EXIT, 20.0050, 73.7862),
                service("demo-exit-panchavati", "Emergency Exit - Panchavati", EmergencyService.Category.EMERGENCY_EXIT, 20.0142, 73.8046),
                service("demo-exit-tapovan", "Emergency Exit - Tapovan", EmergencyService.Category.EMERGENCY_EXIT, 20.0233, 73.8141));
    }

    private static EmergencyService service(String id, String name, EmergencyService.Category category,
            double latitude, double longitude) {
        String location = locationFor(name);
        return new EmergencyService(id, name, category, location,
                "Service location details are subject to verification.", latitude, longitude,
                location, location, "Nashik", location, "0000000000", "",
                EmergencyService.OperationalStatus.OPEN, List.of("Emergency support"), true, "", "", "", "");
    }

    private static String locationFor(String name) {
        if (name.contains("Ramkund")) return "Ramkund, Nashik";
        if (name.contains("Panchavati")) return "Panchavati, Nashik";
        if (name.contains("Central Nashik")) return "Central Nashik";
        if (name.contains("College Road")) return "College Road, Nashik";
        if (name.contains("Sector 4")) return "Sector 4, Nashik";
        if (name.contains("Sector 5")) return "Sector 5, Nashik";
        if (name.contains("Godavari")) return "Godavari Side, Nashik";
        if (name.contains("Kalaram")) return "Kalaram, Nashik";
        return "Nashik Kumbh Zone";
    }
}
