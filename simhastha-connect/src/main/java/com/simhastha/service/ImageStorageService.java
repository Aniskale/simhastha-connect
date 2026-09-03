package com.simhastha.service;

import java.io.File;
import java.util.Optional;

/** Configuration boundary for report imagery. Implementations must never manufacture public URLs. */
public interface ImageStorageService {
    boolean configured();
    Optional<String> upload(File image) throws Exception;
    static ImageStorageService unavailable() {
        return new ImageStorageService() {
            public boolean configured() { return false; }
            public Optional<String> upload(File image) { return Optional.empty(); }
        };
    }
}
