package com.etsa.aicreator;

import com.badlogic.gdx.math.MathUtils;

/** Shared conversion between continuous world positions and detailed world tiles. */
final class WorldCoordinates {
    static final float TILE_SIZE = 6_000f;
    static final float HALF_TILE_SIZE = TILE_SIZE * 0.5f;
    static final float EDGE_APPROACH_DISTANCE = 250f;

    private WorldCoordinates() {
    }

    static int tileIndex(float worldCoordinate) {
        return MathUtils.floor((worldCoordinate + HALF_TILE_SIZE) / TILE_SIZE);
    }

    static float tileCenter(int tileIndex) {
        return tileIndex * TILE_SIZE;
    }

    static float localCoordinate(float worldCoordinate, int tileIndex) {
        return worldCoordinate - tileCenter(tileIndex);
    }

    static boolean isInsideTile(float worldX, float worldZ, int tileX, int tileZ) {
        float localX = localCoordinate(worldX, tileX);
        float localZ = localCoordinate(worldZ, tileZ);
        return localX >= -HALF_TILE_SIZE && localX <= HALF_TILE_SIZE
                && localZ >= -HALF_TILE_SIZE && localZ <= HALF_TILE_SIZE;
    }
}
