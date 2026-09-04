package com.simhastha.service;

import com.simhastha.model.Ghat;
import java.io.IOException;
import java.util.List;

/** Shared durable store for the Admin and pilgrim Ghat flows. */
public interface GhatRepository {
    List<Ghat> loadGhats(String idToken) throws IOException, InterruptedException;
    List<Ghat> loadAdminGhats(String idToken) throws IOException, InterruptedException;
    Ghat loadGhat(String ghatId, String idToken) throws IOException, InterruptedException;
    void saveGhat(Ghat ghat, String idToken) throws IOException, InterruptedException;
    default void updateGhatImage(String ghatId, String imageUrl, String idToken) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Ghat image updates are not implemented by this repository.");
    }
    default void updateGhatImage(String ghatId, String imageUrl, String imagePublicId, String idToken) throws IOException, InterruptedException {
        updateGhatImage(ghatId, imageUrl, idToken);
    }
}
