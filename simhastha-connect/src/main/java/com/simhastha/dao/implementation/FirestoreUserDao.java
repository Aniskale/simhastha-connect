package com.simhastha.dao.implementation;

import com.simhastha.dao.UserDao;
import com.simhastha.gateway.firebase.FirestoreGateway;
import com.simhastha.model.UserProfile;
import com.simhastha.view.AppDataStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class FirestoreUserDao implements UserDao {

    private final FirestoreGateway gateway;

    public FirestoreUserDao(FirestoreGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public Optional<UserProfile> findProfile(String uid, String idToken) throws IOException, InterruptedException {
        FirestoreGateway.UserProfile profile = gateway.loadUserProfile(uid, idToken);
        if (profile == null) {
            return Optional.empty();
        }
        return Optional.of(new UserProfile(profile.uid(), profile.name(), profile.email(), profile.mobile(),
                profile.role(), profile.status()));
    }

    @Override
    public List<AppDataStore.UserRecord> findAll(String idToken) throws IOException, InterruptedException {
        return gateway.loadUsers(idToken);
    }

    @Override
    public void saveProfile(UserProfile profile, String idToken) throws IOException, InterruptedException {
        gateway.saveUserProfile(new FirestoreGateway.UserProfile(profile.uid(), profile.name(), profile.email(),
                profile.mobile(), profile.role(), profile.status()), idToken);
    }

    @Override
    public void updateStatus(String uid, String status, String idToken) throws IOException, InterruptedException {
        gateway.updateUserStatus(uid, status, idToken);
    }
}
