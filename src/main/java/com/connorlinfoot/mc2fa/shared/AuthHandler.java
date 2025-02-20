package com.connorlinfoot.mc2fa.shared;

import com.connorlinfoot.mc2fa.shared.storage.StorageHandler;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.util.HashMap;
import java.util.UUID;

public abstract class AuthHandler {
    protected StorageHandler storageHandler;
    protected HashMap<UUID, AuthState> authStates = new HashMap<>();
    private final HashMap<UUID, String> pendingKeys = new HashMap<>();

    public enum AuthState {
        LOADING, DISABLED, PENDING_SETUP, PENDING_LOGIN, AUTHENTICATED
    }

    public AuthState getState(UUID uuid) {
        if (authStates.containsKey(uuid))
            return authStates.get(uuid);
        return null;
    }

    public boolean isEnabled(UUID uuid) {
        return authStates.get(uuid).equals(AuthState.PENDING_LOGIN) || authStates.get(uuid).equals(AuthState.AUTHENTICATED);
    }

    public boolean isPendingSetup(UUID uuid) {
        return authStates.get(uuid).equals(AuthState.PENDING_SETUP);
    }

    public String createKey(UUID uuid) {
        GoogleAuthenticator authenticator = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        changeState(uuid, AuthState.PENDING_SETUP);
        pendingKeys.put(uuid, key.getKey());
        return key.getKey();
    }

    public boolean validateKey(UUID uuid, Integer password) {
        try {
            String key = getKey(uuid);
            if (key != null && new GoogleAuthenticator().authorize(key, password)) {
                changeState(uuid, AuthState.AUTHENTICATED);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public boolean approveKey(UUID uuid, Integer password) {
        String key = getPendingKey(uuid);
        if (key != null && new GoogleAuthenticator().authorize(key, password)) {
            changeState(uuid, AuthState.AUTHENTICATED);
            pendingKeys.remove(uuid);
            getStorageHandler().setKey(uuid, key);
            return true;
        }
        return false;
    }

    private String getKey(UUID uuid) {
        if (!isEnabled(uuid))
            return null;
        return getStorageHandler().getKey(uuid);
    }

    private String getPendingKey(UUID uuid) {
        if (!isPendingSetup(uuid))
            return null;
        return pendingKeys.get(uuid);
    }

    public String getQRCodeURL(String urlTemplate, UUID uuid) {
        String key = getPendingKey(uuid);
        if (key == null)
            return null;
        return urlTemplate.replaceAll("%%key%%", key);
    }

    public boolean needsToAuthenticate(UUID uuid) {
        return isEnabled(uuid) && !authStates.get(uuid).equals(AuthState.AUTHENTICATED);
    }

    public void reset(UUID uuid) {
        pendingKeys.remove(uuid);
        changeState(uuid, AuthState.DISABLED);
        getStorageHandler().removeKey(uuid);
    }

    public void playerJoin(UUID uuid) {

    }

    public void playerQuit(UUID uuid) {
        pendingKeys.remove(uuid);
        authStates.remove(uuid);
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    public abstract void changeState(UUID uuid, AuthState authState);

}
