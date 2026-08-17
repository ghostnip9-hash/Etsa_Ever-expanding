package com.etsa.aicreator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Small durable world record; detailed terrain remains generated locally from the seed. */
final class PersistentWorldState {
    private static final String PREFERENCES_NAME = "etsa-ever-expanding-world";
    private static final String KEY_SCHEMA = "schema";
    private static final String KEY_WORLD_SEED = "worldSeed";
    private static final String KEY_PLAYER_X = "playerX";
    private static final String KEY_PLAYER_Z = "playerZ";
    private static final int SCHEMA_VERSION = 1;
    private static final float SAVE_INTERVAL_SECONDS = 2f;
    private static final float POSITION_EPSILON_SQUARED = 0.01f;

    private final Preferences preferences;
    private float playerX;
    private float playerZ;
    private float secondsSinceSave;
    private boolean dirty;

    PersistentWorldState() {
        preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        playerX = finiteOrZero(preferences.getFloat(KEY_PLAYER_X, 0f));
        playerZ = finiteOrZero(preferences.getFloat(KEY_PLAYER_Z, 0f));
        if (preferences.getInteger(KEY_SCHEMA, 0) != SCHEMA_VERSION
                || preferences.getLong(KEY_WORLD_SEED, Long.MIN_VALUE) != WorldGenerator.WORLD_SEED) {
            dirty = true;
            flush();
        }
    }

    float playerX() {
        return playerX;
    }

    float playerZ() {
        return playerZ;
    }

    void updatePlayerPosition(float worldX, float worldZ, float deltaSeconds) {
        float dx = worldX - playerX;
        float dz = worldZ - playerZ;
        if (dx * dx + dz * dz > POSITION_EPSILON_SQUARED) {
            playerX = worldX;
            playerZ = worldZ;
            dirty = true;
        }
        if (dirty) {
            secondsSinceSave += deltaSeconds;
            if (secondsSinceSave >= SAVE_INTERVAL_SECONDS) {
                flush();
            }
        }
    }

    void flush() {
        if (!dirty) {
            return;
        }
        preferences.putInteger(KEY_SCHEMA, SCHEMA_VERSION);
        preferences.putLong(KEY_WORLD_SEED, WorldGenerator.WORLD_SEED);
        preferences.putFloat(KEY_PLAYER_X, playerX);
        preferences.putFloat(KEY_PLAYER_Z, playerZ);
        preferences.flush();
        secondsSinceSave = 0f;
        dirty = false;
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0f;
    }
}
