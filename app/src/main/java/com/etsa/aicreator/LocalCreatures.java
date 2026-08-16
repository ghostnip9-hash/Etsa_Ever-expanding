package com.etsa.aicreator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/** Streams a tiny deterministic test population around the active local view. */
final class LocalCreatures {
    private static final int CREATURES_PER_KIND = 2;
    private static final int SEARCH_ATTEMPTS = 96;
    private static final float STREAM_RADIUS = 900f;
    private static final float REBUILD_DISTANCE = 520f;
    private static final float ANCHOR_SIZE = 520f;
    private static final float TILE_MARGIN = 55f;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position
            | VertexAttributes.Usage.Normal;

    private final ModelCache cache = new ModelCache();
    private final Vector3 lastCenter = new Vector3(Float.MAX_VALUE, 0f, Float.MAX_VALUE);

    private final Model landBody;
    private final Model landHead;
    private final Model landHorn;
    private final Model landLeg;
    private final Model airBody;
    private final Model airWing;
    private final Model airEye;
    private final Model waterBody;
    private final Model waterFin;
    private final Model waterEye;
    private int tileX = Integer.MIN_VALUE;
    private int tileZ = Integer.MIN_VALUE;

    LocalCreatures() {
        ModelBuilder builder = new ModelBuilder();
        landBody = builder.createSphere(15f, 7f, 9f, 8, 6,
                material(0.28f, 0.42f, 0.18f), ATTRIBUTES);
        landHead = builder.createSphere(7f, 6f, 7f, 7, 5,
                material(0.38f, 0.52f, 0.20f), ATTRIBUTES);
        landHorn = builder.createCone(2.2f, 7f, 2.2f, 6,
                material(0.68f, 0.63f, 0.42f), ATTRIBUTES);
        landLeg = builder.createCylinder(2.2f, 5f, 2.2f, 6,
                material(0.20f, 0.30f, 0.13f), ATTRIBUTES);

        airBody = builder.createSphere(12f, 4f, 15f, 8, 6,
                material(0.30f, 0.22f, 0.55f), ATTRIBUTES);
        airWing = builder.createSphere(15f, 2.4f, 8f, 7, 5,
                material(0.47f, 0.34f, 0.72f), ATTRIBUTES);
        airEye = builder.createSphere(1.8f, 1.8f, 1.8f, 6, 4,
                material(0.78f, 0.92f, 0.94f), ATTRIBUTES);

        waterBody = builder.createSphere(16f, 6f, 9f, 8, 6,
                material(0.10f, 0.48f, 0.48f), ATTRIBUTES);
        waterFin = builder.createCone(5f, 9f, 5f, 6,
                material(0.12f, 0.66f, 0.58f), ATTRIBUTES);
        waterEye = builder.createSphere(2f, 2f, 2f, 6, 4,
                material(0.88f, 0.78f, 0.24f), ATTRIBUTES);
    }

    void update(PerspectiveCamera camera, Vector3 focus, int activeTileX, int activeTileZ) {
        float dx = focus.x - lastCenter.x;
        float dz = focus.z - lastCenter.z;
        boolean sameTile = tileX == activeTileX && tileZ == activeTileZ;
        if (sameTile && dx * dx + dz * dz < REBUILD_DISTANCE * REBUILD_DISTANCE) {
            return;
        }

        tileX = activeTileX;
        tileZ = activeTileZ;
        lastCenter.set(focus);
        int anchorX = MathUtils.floor(focus.x / ANCHOR_SIZE);
        int anchorZ = MathUtils.floor(focus.z / ANCHOR_SIZE);
        cache.begin(camera);

        addAirCreatures(focus, anchorX, anchorZ);
        int landCount = 0;
        int waterCount = 0;
        for (int attempt = 0; attempt < SEARCH_ATTEMPTS
                && (landCount < CREATURES_PER_KIND || waterCount < CREATURES_PER_KIND); attempt++) {
            float angle = WorldGenerator.random01(anchorX, anchorZ, 311 + attempt * 17)
                    * MathUtils.PI2;
            float distance = 150f + WorldGenerator.random01(anchorX, anchorZ,
                    401 + attempt * 23) * (STREAM_RADIUS - 150f);
            float x = focus.x + MathUtils.cos(angle) * distance;
            float z = focus.z + MathUtils.sin(angle) * distance;
            if (!insideActiveTile(x, z)) {
                continue;
            }

            float generatedHeight = WorldGenerator.height(x, z);
            if (landCount < CREATURES_PER_KIND
                    && generatedHeight > WorldGenerator.WATER_LEVEL + 3f
                    && WorldGenerator.slope(x, z) < 0.72f) {
                addLandProwler(x, EtsaWorld.terrainSurfaceHeight(x, z), z,
                        WorldGenerator.random01(anchorX, anchorZ, 503 + attempt) * 360f);
                landCount++;
            } else if (waterCount < CREATURES_PER_KIND
                    && generatedHeight < WorldGenerator.WATER_LEVEL - 1.5f) {
                addReefBeast(x, WorldGenerator.WATER_LEVEL + 0.9f, z,
                        WorldGenerator.random01(anchorX, anchorZ, 607 + attempt) * 360f);
                waterCount++;
            }
        }
        cache.end();
    }

    ModelCache cache() {
        return cache;
    }

    void dispose() {
        cache.dispose();
        landBody.dispose();
        landHead.dispose();
        landHorn.dispose();
        landLeg.dispose();
        airBody.dispose();
        airWing.dispose();
        airEye.dispose();
        waterBody.dispose();
        waterFin.dispose();
        waterEye.dispose();
    }

    private void addAirCreatures(Vector3 focus, int anchorX, int anchorZ) {
        float tileCenterX = WorldCoordinates.tileCenter(tileX);
        float tileCenterZ = WorldCoordinates.tileCenter(tileZ);
        float limit = WorldCoordinates.HALF_TILE_SIZE - TILE_MARGIN;
        for (int index = 0; index < CREATURES_PER_KIND; index++) {
            float angle = WorldGenerator.random01(anchorX, anchorZ, 701 + index * 31)
                    * MathUtils.PI2;
            float distance = 230f + WorldGenerator.random01(anchorX, anchorZ,
                    733 + index * 37) * 430f;
            float x = MathUtils.clamp(focus.x + MathUtils.cos(angle) * distance,
                    tileCenterX - limit, tileCenterX + limit);
            float z = MathUtils.clamp(focus.z + MathUtils.sin(angle) * distance,
                    tileCenterZ - limit, tileCenterZ + limit);
            float surface = Math.max(WorldGenerator.WATER_LEVEL,
                    EtsaWorld.terrainSurfaceHeight(x, z));
            addSkyRay(x, surface + 48f + index * 13f, z,
                    WorldGenerator.random01(anchorX, anchorZ, 809 + index) * 360f);
        }
    }

    private boolean insideActiveTile(float x, float z) {
        float localX = WorldCoordinates.localCoordinate(x, tileX);
        float localZ = WorldCoordinates.localCoordinate(z, tileZ);
        float limit = WorldCoordinates.HALF_TILE_SIZE - TILE_MARGIN;
        return Math.abs(localX) <= limit && Math.abs(localZ) <= limit;
    }

    private void addLandProwler(float x, float y, float z, float angle) {
        add(landBody, x, y + 5.4f, z, angle, 1f, 1f, 1f);
        add(landHead, x + 7f * MathUtils.sinDeg(angle), y + 7.2f,
                z + 7f * MathUtils.cosDeg(angle), angle, 1f, 1f, 1f);
        add(landHorn, x + 7f * MathUtils.sinDeg(angle), y + 12f,
                z + 7f * MathUtils.cosDeg(angle), angle, 1f, 1f, 1f);
        addLandLeg(x, y, z, angle, -4.5f, -2.7f);
        addLandLeg(x, y, z, angle, 4.5f, -2.7f);
        addLandLeg(x, y, z, angle, -4.5f, 2.7f);
        addLandLeg(x, y, z, angle, 4.5f, 2.7f);
    }

    private void addLandLeg(float x, float y, float z, float angle,
                            float forward, float side) {
        float offsetX = MathUtils.sinDeg(angle) * forward + MathUtils.cosDeg(angle) * side;
        float offsetZ = MathUtils.cosDeg(angle) * forward - MathUtils.sinDeg(angle) * side;
        float legX = x + offsetX;
        float legZ = z + offsetZ;
        float legGround = EtsaWorld.terrainSurfaceHeight(legX, legZ);
        add(landLeg, legX, legGround + 2.5f, legZ, angle, 1f, 1f, 1f);
    }

    private void addSkyRay(float x, float y, float z, float angle) {
        add(airBody, x, y, z, angle, 1f, 1f, 1f);
        float sideX = MathUtils.cosDeg(angle) * 10f;
        float sideZ = -MathUtils.sinDeg(angle) * 10f;
        add(airWing, x + sideX, y, z + sideZ, angle + 12f, 1f, 1f, 1f);
        add(airWing, x - sideX, y, z - sideZ, angle - 12f, 1f, 1f, 1f);
        float frontX = MathUtils.sinDeg(angle) * 6f;
        float frontZ = MathUtils.cosDeg(angle) * 6f;
        add(airEye, x + frontX + sideX * 0.18f, y + 0.5f,
                z + frontZ + sideZ * 0.18f, angle, 1f, 1f, 1f);
        add(airEye, x + frontX - sideX * 0.18f, y + 0.5f,
                z + frontZ - sideZ * 0.18f, angle, 1f, 1f, 1f);
    }

    private void addReefBeast(float x, float y, float z, float angle) {
        add(waterBody, x, y, z, angle, 1f, 1f, 1f);
        add(waterFin, x, y + 5.2f, z, angle, 1f, 1f, 1f);
        float frontX = MathUtils.sinDeg(angle) * 7f;
        float frontZ = MathUtils.cosDeg(angle) * 7f;
        float sideX = MathUtils.cosDeg(angle) * 2.5f;
        float sideZ = -MathUtils.sinDeg(angle) * 2.5f;
        add(waterEye, x + frontX + sideX, y + 2.4f, z + frontZ + sideZ,
                angle, 1f, 1f, 1f);
        add(waterEye, x + frontX - sideX, y + 2.4f, z + frontZ - sideZ,
                angle, 1f, 1f, 1f);
    }

    private void add(Model model, float x, float y, float z, float angle,
                     float scaleX, float scaleY, float scaleZ) {
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x, y, z)
                .rotate(Vector3.Y, angle)
                .scale(scaleX, scaleY, scaleZ);
        cache.add(instance);
    }

    private static Material material(float red, float green, float blue) {
        return new Material(ColorAttribute.createDiffuse(new Color(red, green, blue, 1f)));
    }
}
