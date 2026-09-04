package com.simhastha.service;

import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Refresh abstraction: polling can be replaced by a Firestore realtime listener without changing the UI. */
public final class GhatOperationalStateService {
    private final GhatService ghatService;
    public GhatOperationalStateService(GhatService ghatService) { this.ghatService = ghatService; }
    public AutoCloseable subscribe(String token, Duration interval, Consumer<List<Ghat>> onUpdate, Consumer<Throwable> onError) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> { Thread thread = new Thread(r, "ghat-status-refresh"); thread.setDaemon(true); return thread; });
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean refreshInProgress = new AtomicBoolean();
        Runnable refresh = () -> {
            if (closed.get() || !refreshInProgress.compareAndSet(false, true)) return;
            ghatService.loadGhats(token).whenComplete((ghats, error) -> {
                refreshInProgress.set(false);
                if (closed.get()) return;
                try {
                    if (error == null) onUpdate.accept(ghats == null ? List.of() : ghats);
                    else onError.accept(error);
                } catch (Throwable callbackError) {
                    onError.accept(callbackError);
                }
            });
        };
        refresh.run(); executor.scheduleAtFixedRate(refresh, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);
        return () -> { closed.set(true); executor.shutdownNow(); };
    }
    public static GhatOperationalState.ZoneStatus effectiveAccess(Ghat ghat, LocalTime now) {
        for (GhatOperationalState.AccessWindow window : ghat.operationalState().accessWindows()) if (window.activeAt(now)) return window.accessStatus();
        return ghat.operationalStatus() == Ghat.OperationalStatus.OPEN ? GhatOperationalState.ZoneStatus.OPEN : GhatOperationalState.ZoneStatus.RESTRICTED;
    }
}
