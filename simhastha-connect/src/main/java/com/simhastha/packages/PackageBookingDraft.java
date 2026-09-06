package com.simhastha.packages;

import java.time.Instant;
import java.util.List;

/** Local-only Part 1 booking draft. It is never written to Firestore or payment systems. */
public record PackageBookingDraft(String packageId, PackageCustomizationSnapshot customizationSnapshot,
        PrimaryContact primaryContact, List<PackageTraveller> travellers, String emergencyContact,
        String specialAssistanceNotes, int totalAmount, Instant createdAt) { }
