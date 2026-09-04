package com.simhastha.dao;

import com.simhastha.model.UserProfile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.simhastha.view.AppDataStore;

public interface UserDao {
    Optional<UserProfile> findProfile(String uid, String idToken) throws IOException, InterruptedException;

    List<AppDataStore.UserRecord> findAll(String idToken) throws IOException, InterruptedException;

    void saveProfile(UserProfile profile, String idToken) throws IOException, InterruptedException;

    void updateStatus(String uid, String status, String idToken) throws IOException, InterruptedException;
}
