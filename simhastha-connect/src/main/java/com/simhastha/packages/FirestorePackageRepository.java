package com.simhastha.packages;

import com.simhastha.view.FirebaseConfig;
import com.simhastha.view.FirestoreGateway;
import java.io.IOException;
import java.util.*;

/** Firestore-backed source of truth with an explicit in-process fallback only when Firebase is disabled. */
public final class FirestorePackageRepository {
    private static final Map<String, ManagedKumbhPackage> local = new LinkedHashMap<>();
    private final FirebaseConfig config = FirebaseConfig.load();
    private final FirestoreGateway gateway = new FirestoreGateway(config);

    public boolean hasProductionSource() { return config.isEnabled() || !local.isEmpty(); }

    public List<ManagedKumbhPackage> getAllPackagesForAdmin(String token) throws IOException, InterruptedException {
        if (!config.isEnabled()) return List.copyOf(local.values());
        if (token == null || token.isBlank()) throw new IOException("An authenticated admin token is required to load Kumbh Packages.");
        return gateway.loadKumbhPackages(token);
    }
    public Optional<ManagedKumbhPackage> getPackageById(String packageId, String token) throws IOException, InterruptedException {
        return getAllPackagesForAdmin(token).stream().filter(p -> p.packageId().equals(packageId)).findFirst();
    }
    /** Production published catalogue. Failures intentionally propagate to the explicit development fallback boundary. */
    public List<KumbhPackage> getPublishedPackagesForUsers() throws IOException, InterruptedException {
        List<ManagedKumbhPackage> records;
        if (!config.isEnabled()) records = List.copyOf(local.values());
        else records = gateway.loadKumbhPackages("");
        return records.stream().filter(p -> p.status() == PackageStatus.PUBLISHED).map(ManagedKumbhPackage::cataloguePackage).toList();
    }
    public void createPackage(ManagedKumbhPackage item, String token) throws IOException, InterruptedException { save(item, token); }
    public void updatePackage(ManagedKumbhPackage item, String token) throws IOException, InterruptedException { save(item, token); }
    public void publishPackage(ManagedKumbhPackage item, String token) throws IOException, InterruptedException { save(withStatus(item, PackageStatus.PUBLISHED), token); }
    public void pausePackage(ManagedKumbhPackage item, String token) throws IOException, InterruptedException { save(withStatus(item, PackageStatus.PAUSED), token); }
    public void archivePackage(ManagedKumbhPackage item, String token) throws IOException, InterruptedException { save(withStatus(item, PackageStatus.ARCHIVED), token); }
    /** Completes only after Firestore accepts the write and an admin reload can see the immutable id. */
    public List<ManagedKumbhPackage> saveAndReload(ManagedKumbhPackage item, String token) throws IOException, InterruptedException {
        if (item.packageId() == null || item.packageId().isBlank()) throw new IOException("Package ID was not generated.");
        if (!config.isEnabled()) { local.put(item.packageId(), item); return List.copyOf(local.values()); }
        if (token == null || token.isBlank()) throw new IOException("An authenticated admin token is required to save Kumbh Packages.");
        gateway.saveKumbhPackage(item, token);
        List<ManagedKumbhPackage> reloaded = gateway.loadKumbhPackages(token);
        if (reloaded.stream().noneMatch(p -> p.packageId().equals(item.packageId()))) throw new IOException("Firestore accepted the request but the package could not be reloaded. Check the kumbh_packages rules and admin token.");
        return reloaded;
    }
    public void save(ManagedKumbhPackage item, String token) throws IOException, InterruptedException { saveAndReload(item, token); }
    private ManagedKumbhPackage withStatus(ManagedKumbhPackage p, PackageStatus status) { String now = String.valueOf(System.currentTimeMillis()); return new ManagedKumbhPackage(p.packageId(),p.packageCode(),p.name(),p.category(),p.theme(),p.badge(),p.origin(),p.destination(),p.days(),p.nights(),p.shortDescription(),p.description(),p.travelOptions(),p.stayOptions(),p.mealOptions(),p.facilities(),p.touristPlaces(),p.itinerary(),p.basePrice(),p.startingPrice(),p.originalPrice(),p.discount(),p.inclusions(),p.exclusions(),p.policies(),p.availableFrom(),p.availableUntil(),p.departureDates(),p.maximumCapacity(),p.minimumTravellers(),status,p.createdBy(),p.createdAt(),now,status==PackageStatus.PUBLISHED?now:p.publishedAt(),p.coverImage(),p.heroImage(),p.gallery()); }
}
